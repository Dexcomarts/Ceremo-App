"""Enforce the approved severity policy on actual scanner results."""

import argparse
from decimal import Decimal, InvalidOperation
import json
import os
from pathlib import Path
import re

from ci_report import read_json


def evaluate_sarif(data, expected_tool):
    runs = data.get('runs')
    if not isinstance(runs, list) or not 1 <= len(runs) <= 20:
        raise ValueError('No completed scanner result')
    counts = dict.fromkeys(('critical', 'high', 'medium', 'low', 'informational', 'error'), 0)
    findings = []
    for run in runs:
        driver = run['tool']['driver']
        if driver['name'] != expected_tool or not isinstance(run.get('results'), list):
            raise ValueError('Scanner identity or findings are missing')
        if any(invocation.get('executionSuccessful') is False for invocation in run.get('invocations', [])):
            raise ValueError('Scanner execution did not complete successfully')
        rules = {rule['id']: rule for rule in driver.get('rules', [])}
        for result in run['results']:
            rule_id = result.get('ruleId')
            if not isinstance(rule_id, str) or not re.fullmatch(r'[A-Za-z0-9_/.-]{1,250}', rule_id):
                raise ValueError('Invalid scanner rule')
            if result.get('kind', 'fail') in ('pass', 'notApplicable'):
                continue
            if expected_tool == 'zizmor':
                severity = result.get('properties', {}).get('zizmor/severity', '').lower()
                if severity not in counts:
                    raise ValueError('Missing workflow-security severity')
            else:
                if rule_id not in rules:
                    raise ValueError('Finding has no rule metadata')
                score = rules[rule_id].get('properties', {}).get('security-severity')
                if score is None:
                    severity = 'error' if result.get('level') == 'error' else 'informational'
                else:
                    try:
                        score = Decimal(str(score))
                        if not score.is_finite() or not 0 <= score <= 10:
                            raise ValueError('Invalid security severity')
                    except InvalidOperation as error:
                        raise ValueError('Invalid security severity') from error
                    severity = 'critical' if score >= 9 else 'high' if score >= 7 else 'medium' if score >= 4 else 'low'
            counts[severity] += 1
            finding = {'rule': rule_id, 'severity': severity}
            locations = result.get('locations', [])
            if locations:
                physical = locations[0].get('physicalLocation', {})
                file = physical.get('artifactLocation', {}).get('uri', '')
                if re.fullmatch(r'[A-Za-z0-9_./-]{1,500}', file) and '..' not in file.split('/'):
                    finding['file'] = file
                    line = physical.get('region', {}).get('startLine')
                    if type(line) is int and 1 <= line <= 1_000_000:
                        finding['line'] = line
            findings.append(finding)
            if len(findings) > 5000:
                raise ValueError('Scanner results exceed the complete-report limit')
    return {'tool': expected_tool, 'counts': counts, 'findings': findings,
            'blocked': any(counts[name] for name in ('critical', 'high', 'error'))}


def require_jobs(needs):
    expected = {'validation', 'secrets', 'verify'}
    if set(needs) != expected or any(needs[name].get('result') != 'success' for name in expected):
        raise ValueError('Every required backend job must complete successfully')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=('sarif', 'jobs'))
    parser.add_argument('--input', type=Path)
    parser.add_argument('--tool', choices=('CodeQL', 'zizmor'))
    args = parser.parse_args()
    if args.command == 'jobs':
        require_jobs(json.loads(os.environ['JOB_RESULTS']))
        print('All required backend jobs passed.')
    else:
        if args.input.is_dir():
            files = sorted(args.input.glob('*.sarif'))
            if not files:
                raise ValueError('Scanner produced no SARIF files')
            data = {'runs': [run for file in files for run in read_json(file, 50_000_000)['runs']]}
            destination = args.input / 'findings.summary.json'
        else:
            data = read_json(args.input, 50_000_000)
            destination = args.input.with_suffix('.summary.json')
        result = evaluate_sarif(data, args.tool)
        destination.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
        summary = f"{args.tool}: " + ', '.join(f'{count} {name}' for name, count in result['counts'].items())
        print(summary)
        if os.environ.get('GITHUB_STEP_SUMMARY'):
            with open(os.environ['GITHUB_STEP_SUMMARY'], 'a', encoding='utf-8') as report:
                report.write(summary + '\n\nCritical/high security findings and correctness errors block this check.\n')
        if result['blocked']:
            raise SystemExit('Required scanner policy failed; see the retained findings and annotations.')
