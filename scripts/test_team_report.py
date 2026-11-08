import unittest
from unittest.mock import patch

import ci_report
import team_report

SHA = 'a' * 40
REPO = 'owner/project'


def workflow(name, identifier=1, state='success'):
    return {'id': identifier, 'run_attempt': 1, 'name': name, 'path': ci_report.WORKFLOWS[name],
            'head_sha': SHA, 'status': 'completed', 'conclusion': state,
            'repository': {'full_name': REPO}}


class TeamReportTest(unittest.TestCase):
    def test_latest_attempt_is_used_even_when_an_older_workflow_finished_last(self):
        values = [workflow('CodeQL', 1, 'success'), workflow('CodeQL', 2, 'failure')]
        self.assertEqual(team_report.latest_runs(values, SHA)['CodeQL']['id'], 2)
        values[1]['path'] = '.github/workflows/forged.yml'
        self.assertEqual(team_report.latest_runs(values, SHA)['CodeQL']['id'], 1)

    def test_missing_results_are_not_presented_as_a_complete_pass(self):
        runs = {name: workflow(name, i) for i, name in enumerate(ci_report.WORKFLOWS, 1)}
        body = team_report.render_team_report(REPO, SHA, runs, {}, None)
        self.assertIn('Unavailable', body)
        self.assertIn('measurements are unavailable', body)
        runs['CodeQL']['conclusion'] = 'failure'
        self.assertIn('❌ Failed', team_report.render_team_report(REPO, SHA, runs, {}, None))

    def test_measurements_require_current_base_head_and_actual_merge_parents(self):
        pr = {'base': {'sha': 'b' * 40}}
        data = {'schemaVersion': 2, 'prHeadSha': SHA, 'baseSha': 'b' * 40, 'testedSha': 'c' * 40}
        for parents in (['b' * 40, SHA], ['d' * 40, SHA], []):
            with patch.object(ci_report, 'download_results', return_value=data), \
                    patch.object(ci_report, 'github_request', return_value={'parents': [{'sha': p} for p in parents]}):
                result = team_report.measurements(REPO, workflow('Backend checks'), pr, SHA)
                self.assertEqual(result, data if parents == ['b' * 40, SHA] else None)
        data['baseSha'] = 'e' * 40
        with patch.object(ci_report, 'download_results', return_value=data), patch.object(ci_report, 'github_request') as api:
            self.assertIsNone(team_report.measurements(REPO, workflow('Backend checks'), pr, SHA))
            api.assert_not_called()

    def test_untrusted_job_names_are_escaped_and_unknown_steps_are_not_green(self):
        run = workflow('CodeQL')
        jobs = {1: [{'id': 55, 'name': 'bad | [link](https://example.invalid)', 'conclusion': 'failure',
                     'steps': [{'name': 'test | injection', 'number': 2, 'conclusion': 'skipped'}]}]}
        body = team_report.render_team_report(REPO, SHA, {'CodeQL': run}, jobs, None)
        self.assertNotIn('[link](https://example.invalid)', body)
        self.assertIn('⏭️ Skipped', body)

    def test_stale_and_wrong_base_prs_cannot_receive_comments(self):
        run = workflow('CodeQL', 123)
        pr = {'number': 7, 'state': 'open', 'head': {'sha': 'b' * 40},
              'base': {'ref': 'main', 'repo': {'full_name': REPO}}}
        with patch.object(ci_report, 'github_request', side_effect=[run, pr]) as api, \
                patch.object(ci_report, 'pages', return_value=[pr]):
            team_report.publish_team_comment(REPO, 123)
        self.assertEqual(api.call_count, 2)

    def test_report_updates_only_the_bot_comment_after_rechecking_the_pr(self):
        for moved in (False, True):
            run = workflow('CodeQL', 123)
            pr = {'number': 7, 'state': 'open', 'head': {'sha': SHA},
                  'base': {'ref': 'main', 'sha': 'b' * 40, 'repo': {'full_name': REPO}}}
            current = {**pr, 'head': {'sha': 'c' * 40}} if moved else pr
            comment = {'id': 8, 'user': {'login': 'github-actions[bot]', 'type': 'Bot'}, 'body': ci_report.MARKER}
            def pages(path, key=None):
                if path.endswith('/pulls'):
                    return [pr]
                if key == 'workflow_runs':
                    return [run] if 'head_sha=' in path else []
                if key == 'jobs':
                    return [{'id': 1, 'name': 'CodeQL', 'conclusion': 'success', 'steps': []}]
                return [comment]
            with self.subTest(moved=moved), patch.object(ci_report, 'pages', side_effect=pages), \
                    patch.object(team_report, 'publication_receipt', return_value=None), \
                    patch.object(ci_report, 'github_request', side_effect=[run, pr, current, {}]) as api:
                team_report.publish_team_comment(REPO, 123)
                writes = [call for call in api.call_args_list if len(call.args) > 1]
                self.assertEqual(len(writes), 0 if moved else 1)
                if writes:
                    self.assertEqual(writes[0].args[:2], ('/repos/owner/project/issues/comments/8', 'PATCH'))
                    self.assertIn('CodeQL', writes[0].args[2]['body'])

    def test_wrong_base_and_unrelated_workflows_never_write(self):
        run = workflow('CodeQL', 123)
        pr = {'number': 7, 'state': 'open', 'head': {'sha': SHA},
              'base': {'ref': 'release', 'repo': {'full_name': REPO}}}
        with patch.object(ci_report, 'pages', return_value=[pr]), \
                patch.object(ci_report, 'github_request', side_effect=[run, pr]) as api:
            team_report.publish_team_comment(REPO, 123)
            self.assertEqual(api.call_count, 2)
        run['path'] = '.github/workflows/forged.yml'
        with patch.object(ci_report, 'github_request', return_value=run) as api, self.assertRaises(ValueError):
            team_report.publish_team_comment(REPO, 123)
        self.assertEqual(api.call_count, 1)


if __name__ == '__main__':
    unittest.main()
