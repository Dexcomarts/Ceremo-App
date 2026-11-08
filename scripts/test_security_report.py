import unittest

from security_report import evaluate_sarif, require_jobs


def sarif(score):
    return {'version': '2.1.0', 'runs': [{'tool': {'driver': {'name': 'CodeQL', 'rules': [
        {'id': 'java/example', 'properties': {'security-severity': score}}]}},
        'invocations': [{'executionSuccessful': True}],
        'results': [{'ruleId': 'java/example', 'level': 'warning'}]}]}


class SecurityReportTest(unittest.TestCase):
    def test_reports_all_severities_and_blocks_only_high_and_critical(self):
        for score, severity, blocked in [('3.0', 'low', False), ('6.0', 'medium', False),
                                          ('8.0', 'high', True), ('9.8', 'critical', True)]:
            with self.subTest(score=score):
                result = evaluate_sarif(sarif(score), 'CodeQL')
                self.assertEqual(result['counts'][severity], 1)
                self.assertEqual(result['blocked'], blocked)

    def test_rejects_missing_results_failed_analysis_and_malformed_severity(self):
        for data in ({'runs': []}, sarif('NaN'), sarif('11')):
            with self.subTest(data=data), self.assertRaises(ValueError):
                evaluate_sarif(data, 'CodeQL')
        data = sarif('8'); data['runs'][0]['invocations'][0]['executionSuccessful'] = False
        with self.assertRaises(ValueError):
            evaluate_sarif(data, 'CodeQL')

    def test_recognises_zizmor_severity_without_forwarding_raw_messages(self):
        data = sarif('1')
        data['runs'][0]['tool']['driver']['name'] = 'zizmor'
        data['runs'][0]['results'][0].update({'message': {'text': 'private context'},
            'properties': {'zizmor/severity': 'High'}})
        result = evaluate_sarif(data, 'zizmor')
        self.assertTrue(result['blocked'])
        self.assertNotIn('private context', str(result))

    def test_final_job_requires_every_expected_job_to_succeed(self):
        data = {name: {'result': 'success'} for name in ('validation', 'secrets', 'verify')}
        require_jobs(data)
        for state in ('failure', 'skipped', 'cancelled', 'pending'):
            data['verify']['result'] = state
            with self.subTest(state=state), self.assertRaises(ValueError):
                require_jobs(data)
        with self.assertRaises(ValueError):
            require_jobs({})


if __name__ == '__main__':
    unittest.main()
