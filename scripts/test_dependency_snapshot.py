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
        for accepted in ('SUCCESS', 'FAILURE'):
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
                        if accepted == 'SUCCESS':
                            subject.publish(123, 2)
                        else:
                            with self.assertRaisesRegex(ValueError, 'confirm'):
                                subject.publish(123, 2)
                    writes = [call for call in calls if call[1] == 'POST']
                    self.assertEqual(len(writes), 1)
                    self.assertEqual(writes[0][2]['sha'], SHA)
                    self.assertEqual(writes[0][2]['job']['id'], '123-2')
                    self.assertEqual(Path('build/reports/dependencies/publication.json').exists(), accepted == 'SUCCESS')
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
        with patch('dependency_snapshot.publication_receipt', return_value={'accepted': True}):
            require_publications(REPO, SHA, 'b' * 40, timeout=0)
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


if __name__ == '__main__':
    unittest.main()
