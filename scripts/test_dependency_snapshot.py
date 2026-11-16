import copy
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import dependency_snapshot as subject
from dependency_snapshot import clean_snapshot, require_publications, source_context

SHA = 'a' * 40
REPO = 'owner/project'


def snapshot():
    return {'version': 0, 'sha': SHA, 'ref': 'refs/heads/main',
            'scanned': '2026-11-08T00:00:00Z',
            'detector': {'name': 'Gradle', 'url': 'https://github.com/gradle/github-dependency-graph-gradle-plugin', 'version': '1.5.0'},
            'job': {'id': '123', 'correlator': 'fixture'},
            'manifests': {'build.gradle.kts': {'name': 'build.gradle.kts', 'resolved': {
                'one': {'package_url': 'pkg:maven/example/one@1.0', 'relationship': 'direct',
                        'scope': 'runtime', 'dependencies': ['two']},
                'two': {'package_url': 'pkg:maven/example/two@1.0', 'relationship': 'indirect',
                        'scope': 'development', 'dependencies': []}}}}}


class DependencySnapshotTest(unittest.TestCase):
    def run_data(self):
        return {'id': 123, 'name': 'Dependency graph', 'path': '.github/workflows/dependency-submission.yml',
                'repository': {'full_name': REPO}, 'status': 'completed', 'conclusion': 'success',
                'head_sha': SHA, 'head_branch': 'main', 'event': 'push', 'run_attempt': 2}

    def archive(self, entries):
        raw = io.BytesIO()
        with zipfile.ZipFile(raw, 'w') as archive:
            for name, value in entries.items():
                archive.writestr(name, json.dumps(value))
        return raw.getvalue()

    def test_publisher_writes_only_a_validated_snapshot_and_confirmed_receipt(self):
        for accepted in ('SUCCESS', 'ACCEPTED', 'FAILURE'):
            with self.subTest(accepted=accepted), tempfile.TemporaryDirectory() as directory:
                previous = Path.cwd()
                try:
                    os.chdir(directory)
                    calls = []
                    def api(path, method='GET', body=None):
                        calls.append((path, method, body))
                        if method == 'POST':
                            return {'result': accepted}
                        return {'commit': {'sha': SHA}} if '/branches/' in path else self.run_data()
                    def pages(path, key=None):
                        return ([{'name': 'Resolve Gradle dependencies', 'conclusion': 'success'}]
                                if key == 'jobs' else [{'id': 9, 'name': 'ceremo-dependencies-123-2', 'expired': False}])
                    with patch.dict(os.environ, {'GITHUB_REPOSITORY': REPO, 'GITHUB_OUTPUT': ''}), \
                            patch.object(subject, 'github_request', side_effect=api), \
                            patch.object(subject, 'pages', side_effect=pages), \
                            patch.object(subject, 'download_archive', return_value=self.archive({'snapshot.json': snapshot()})):
                        if accepted in ('SUCCESS', 'ACCEPTED'):
                            subject.publish(123, 2)
                        else:
                            with self.assertRaisesRegex(ValueError, 'confirm'):
                                subject.publish(123, 2)
                    writes = [call for call in calls if call[1] == 'POST']
                    self.assertEqual(len(writes), 1)
                    self.assertEqual(writes[0][2]['sha'], SHA)
                    self.assertEqual(writes[0][2]['job']['id'], '123-2')
                    self.assertEqual(Path('build/reports/dependencies/publication.json').exists(), accepted in ('SUCCESS', 'ACCEPTED'))
                finally:
                    os.chdir(previous)

    def test_stale_main_wrong_revision_and_extra_archive_files_never_submit(self):
        wrong = snapshot(); wrong['sha'] = 'b' * 40
        for entries, branch in [({'snapshot.json': snapshot()}, 'b' * 40),
                                ({'snapshot.json': wrong}, SHA),
                                ({'snapshot.json': snapshot(), 'script.py': 'ignored'}, SHA)]:
            def api(path, method='GET', body=None):
                return {'commit': {'sha': branch}} if '/branches/' in path else self.run_data()
            with patch.dict(os.environ, {'GITHUB_REPOSITORY': REPO}), \
                    patch.object(subject, 'github_request', side_effect=api) as requests, \
                    patch.object(subject, 'pages', side_effect=[
                        [{'name': 'Resolve Gradle dependencies', 'conclusion': 'success'}],
                        [{'id': 9, 'name': 'ceremo-dependencies-123-2', 'expired': False}]]), \
                    patch.object(subject, 'download_archive', return_value=self.archive(entries)), \
                    self.assertRaises(ValueError):
                subject.publish(123, 2)
            self.assertFalse(any(len(call.args) > 1 and call.args[1] == 'POST' for call in requests.call_args_list))

    def test_a_forged_receipt_from_a_pr_job_is_not_accepted(self):
        artifact = {'id': 1, 'name': 'dependency-publication-' + SHA, 'expired': False, 'workflow_run': {'id': 6}}
        with patch.object(subject, 'pages', return_value=[artifact]), \
                patch.object(subject, 'github_request', return_value=self.run_data()), \
                patch.object(subject, 'download_archive') as download:
            self.assertIsNone(subject.publication_receipt(REPO, SHA))
            download.assert_not_called()

    def test_accepted_receipt_is_bound_to_a_trusted_publisher_and_source_commit(self):
        artifact = {'id': 1, 'name': 'dependency-publication-' + SHA, 'expired': False, 'workflow_run': {'id': 6}}
        publisher = {'id': 6, 'name': 'Dependency publication 123 attempt 2',
                     'display_title': 'Dependency publication 123 attempt 2', 'event': 'workflow_run',
                     'path': '.github/workflows/dependency-publish.yml',
                     'repository': {'full_name': REPO}, 'conclusion': 'success'}
        receipt = {'sha': SHA, 'accepted': True, 'sourceRunId': 123, 'sourceAttempt': 2}
        for source_sha in (SHA, 'b' * 40):
            source = self.run_data(); source['head_sha'] = source_sha
            with patch.object(subject, 'pages', return_value=[artifact]), \
                    patch.object(subject, 'github_request', side_effect=[publisher, source]), \
                    patch.object(subject, 'download_archive', return_value=self.archive({'publication.json': receipt})):
                if source_sha == SHA:
                    self.assertEqual(subject.publication_receipt(REPO, SHA)['publisherRunId'], 6)
                else:
                    with self.assertRaises(ValueError):
                        subject.publication_receipt(REPO, SHA)

    def test_missing_snapshot_receipts_fail_instead_of_becoming_warnings(self):
        with patch('dependency_snapshot.publication_receipt', return_value=None), self.assertRaises(ValueError):
            require_publications(REPO, SHA, 'b' * 40, timeout=0)
        with patch('dependency_snapshot.publication_receipt', return_value={'accepted': True}), \
                patch('dependency_snapshot.snapshots_indexed', return_value=True):
            require_publications(REPO, SHA, 'b' * 40, timeout=0)

    def test_accepted_receipts_cannot_hide_missing_comparison_snapshots(self):
        with patch.object(subject, 'publication_receipt', return_value={'accepted': True}), \
                patch.object(subject, 'snapshots_indexed', return_value=False), self.assertRaises(ValueError):
            require_publications(REPO, SHA, 'b' * 40, timeout=0)

    def test_comparison_requires_a_valid_response_without_snapshot_warnings(self):
        for warning, expected in [('', True), ('eyJtaXNzaW5nIjp0cnVlfQ==', False)]:
            with patch.object(subject, 'github_request', return_value=([], {
                    'x-github-dependency-graph-snapshot-warnings': warning})):
                self.assertEqual(subject.snapshots_indexed(REPO, SHA, 'b' * 40), expected)
        with patch.object(subject, 'github_request', return_value=({}, {})), self.assertRaises(ValueError):
            subject.snapshots_indexed(REPO, SHA, 'b' * 40)
    def test_keeps_the_full_graph_and_removes_unnecessary_metadata(self):
        data = snapshot()
        data['metadata'] = {'untrusted': 'not forwarded'}
        clean = clean_snapshot(data, SHA, 'refs/heads/main', REPO, 123, 2)
        self.assertEqual(len(clean['manifests']['build.gradle.kts']['resolved']), 2)
        self.assertNotIn('metadata', clean)
        self.assertEqual(clean['job']['id'], '123-2')
        self.assertEqual(clean['job']['correlator'], 'ceremo-gradle')

    def test_rejects_cross_commit_ref_empty_graph_and_broken_edges(self):
        variations = []
        for key, value in [('sha', 'b' * 40), ('ref', 'refs/heads/other'), ('manifests', {})]:
            data = snapshot(); data[key] = value; variations.append(data)
        data = snapshot(); data['manifests']['build.gradle.kts']['resolved']['one']['dependencies'] = ['missing']
        variations.append(data)
        data = snapshot(); data['manifests']['build.gradle.kts']['resolved']['one']['package_url'] = 'https://attacker.invalid'
        variations.append(data)
        for data in variations:
            with self.subTest(data=data), self.assertRaises(ValueError):
                clean_snapshot(data, SHA, 'refs/heads/main', REPO, 123, 1)

    def test_only_accepts_the_expected_completed_attempt_and_source(self):
        run = {'name': 'Dependency graph', 'path': '.github/workflows/dependency-submission.yml',
               'repository': {'full_name': REPO}, 'status': 'completed', 'conclusion': 'success',
               'head_sha': SHA, 'head_branch': 'main', 'event': 'push', 'run_attempt': 2}
        self.assertEqual(source_context(run, REPO, 2), (SHA, 'refs/heads/main'))
        for key, value in [('event', 'pull_request_target'), ('conclusion', 'failure'),
                           ('run_attempt', 1), ('path', '.github/workflows/other.yml'),
                           ('head_branch', 'other')]:
            candidate = copy.deepcopy(run); candidate[key] = value
            with self.subTest(key=key), self.assertRaises(ValueError):
                source_context(candidate, REPO, 2)


    def test_accepts_only_protected_branch_pushes_and_pull_requests(self):
        run = self.run_data()
        for event in ('push', 'schedule', 'workflow_dispatch'):
            for branch in ('main', 'dev'):
                candidate = {**run, 'event': event, 'head_branch': branch}
                with self.subTest(event=event, branch=branch):
                    self.assertEqual(source_context(candidate, REPO, 2), (SHA, 'refs/heads/' + branch))
            for branch in ('feature', 'dev2', 'main ', ' dev', 'Dev', 'refs/heads/dev', '', None):
                candidate = {**run, 'event': event, 'head_branch': branch}
                with self.subTest(event=event, branch=branch), self.assertRaises(ValueError):
                    source_context(candidate, REPO, 2)

    def test_pull_requests_must_target_a_protected_branch_in_this_repository(self):
        run = {**self.run_data(), 'event': 'pull_request', 'head_branch': 'feature'}
        def pr(base, repo=REPO):
            return {'number': 7, 'state': 'open', 'head': {'sha': SHA},
                    'base': {'ref': base, 'repo': {'full_name': repo}}}
        for base in ('main', 'dev'):
            with self.subTest(base=base), patch.object(subject, 'pages', return_value=[pr(base)]), \
                    patch.object(subject, 'github_request', return_value=pr(base)):
                self.assertEqual(source_context(run, REPO, 2), (SHA, 'refs/pull/7/merge'))
        for listed, current in [(pr('feature'), pr('feature')), (pr('dev2'), pr('dev2')),
                                (pr('main '), pr('main ')), (pr('dev', 'fork/project'), pr('dev', 'fork/project')),
                                (pr('dev'), pr('feature'))]:
            with self.subTest(listed=listed['base'], current=current['base']), \
                    patch.object(subject, 'pages', return_value=[listed]), \
                    patch.object(subject, 'github_request', return_value=current), self.assertRaises(ValueError):
                source_context(run, REPO, 2)
        with patch.object(subject, 'pages', return_value=[pr('main'), {**pr('dev'), 'number': 8}]), \
                patch.object(subject, 'github_request') as api, self.assertRaises(ValueError):
            source_context(run, REPO, 2)
        api.assert_not_called()

    def test_dev_snapshot_checks_the_dev_head_and_never_submits_when_dev_moved(self):
        dev = snapshot(); dev['ref'] = 'refs/heads/dev'
        for heads in ([SHA, SHA], ['b' * 40, SHA], [SHA, 'b' * 40]):
            calls = []
            pending = list(heads)
            def api(path, method='GET', body=None):
                calls.append((path, method))
                if method == 'POST':
                    return {'result': 'SUCCESS'}
                if '/branches/' in path:
                    return {'commit': {'sha': pending.pop(0)}}
                return {**self.run_data(), 'head_branch': 'dev'}
            with self.subTest(heads=heads), tempfile.TemporaryDirectory() as directory:
                previous = Path.cwd()
                try:
                    os.chdir(directory)
                    with patch.dict(os.environ, {'GITHUB_REPOSITORY': REPO, 'GITHUB_OUTPUT': ''}), \
                            patch.object(subject, 'github_request', side_effect=api), \
                            patch.object(subject, 'pages', side_effect=[
                                [{'name': 'Resolve Gradle dependencies', 'conclusion': 'success'}],
                                [{'id': 9, 'name': 'ceremo-dependencies-123-2', 'expired': False}]]), \
                            patch.object(subject, 'download_archive', return_value=self.archive({'snapshot.json': dev})):
                        if heads == [SHA, SHA]:
                            subject.publish(123, 2)
                        else:
                            with self.assertRaisesRegex(ValueError, 'Branch'):
                                subject.publish(123, 2)
                finally:
                    os.chdir(previous)
                branch_reads = [path for path, _ in calls if '/branches/' in path]
                self.assertTrue(branch_reads)
                self.assertTrue(all(path == f'/repos/{REPO}/branches/dev' for path in branch_reads))
                self.assertEqual(len([c for c in calls if c[1] == 'POST']), 1 if heads == [SHA, SHA] else 0)

    def test_a_main_artifact_cannot_be_published_for_a_dev_run(self):
        def api(path, method='GET', body=None):
            return {'commit': {'sha': SHA}} if '/branches/' in path else {**self.run_data(), 'head_branch': 'dev'}
        with patch.dict(os.environ, {'GITHUB_REPOSITORY': REPO}), \
                patch.object(subject, 'github_request', side_effect=api) as requests, \
                patch.object(subject, 'pages', side_effect=[
                    [{'name': 'Resolve Gradle dependencies', 'conclusion': 'success'}],
                    [{'id': 9, 'name': 'ceremo-dependencies-123-2', 'expired': False}]]), \
                patch.object(subject, 'download_archive', return_value=self.archive({'snapshot.json': snapshot()})), \
                self.assertRaisesRegex(ValueError, 'source revision'):
            subject.publish(123, 2)
        self.assertFalse(any(len(call.args) > 1 and call.args[1] == 'POST' for call in requests.call_args_list))


if __name__ == '__main__':
    unittest.main()
