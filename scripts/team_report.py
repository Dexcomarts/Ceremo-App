"""Aggregate current checks without executing code from the pull request."""

import html
import re
import urllib.error
import zipfile

import ci_report as ci
from dependency_snapshot import publication_receipt


def latest_runs(values, sha):
    result = {}
    for run in values:
        name = run.get('name')
        if (name not in ci.WORKFLOWS or run.get('path') != ci.WORKFLOWS[name] or
                run.get('head_sha') != sha):
            continue
        previous = result.get(name)
        if previous is None or (run['id'], run['run_attempt']) > (previous['id'], previous['run_attempt']):
            result[name] = run
    return result


def label(value):
    # Names come from PR-editable workflow files. Do not render their Markdown/HTML.
    value = html.escape(str(value)[:160], quote=True)
    return re.sub(r'[|\[\]()`*\\\r\n]', ' ', value)


def state(item):
    if item.get('status') in ('queued', 'in_progress', 'waiting', 'requested', 'pending'):
        return ci.STATES['pending']
    return ci.STATES.get(item.get('conclusion'), ci.STATES['unknown'])


def render_team_report(repo, sha, runs, jobs, results):
    lines = [ci.MARKER, '## CEREMO pull request checks', f'PR head `{sha}`', '',
             '| Check | Result | Purpose |', '| --- | --- | --- |']
    for name, description in ci.WORKFLOW_DESCRIPTIONS.items():
        run = runs.get(name)
        title = name
        if run:
            title = f'[{name}](https://github.com/{repo}/actions/runs/{int(run["id"])})'
        lines.append(f'| {title} | {state(run or {})} | {description} |')
    lines += ['', 'Critical/high security findings block the relevant check. Medium/low findings remain available for review.',
              'A successful scan is not independent code review. Skipped, pending and unavailable checks do not establish a pass.']
    for name, run in runs.items():
        lines += ['', f'<details><summary>{label(name)} — recorded jobs and steps</summary>', '',
                  '| Task | Result | Explanation |', '| --- | --- | --- |']
        for job in jobs.get(run['id'], [])[:15]:
            url = f'https://github.com/{repo}/actions/runs/{int(run["id"])}/job/{int(job["id"])}'
            lines.append(f'| [{label(job["name"])}]({url}) | {state(job)} | Open the job for diagnostics and reports. |')
            for step in job.get('steps', [])[:40]:
                outcome = step.get('conclusion')
                explanation = ('Failed; open the step log for the exact error.' if outcome == 'failure' else
                               'Not run; check prerequisites and conditions.' if outcome == 'skipped' else
                               'Execution interrupted; no pass claimed.' if outcome in ('cancelled', 'timed_out') else
                               'Completed successfully.' if outcome == 'success' else 'Result unavailable or still running.')
                link = f'{url}#step:{int(step["number"])}:1'
                lines.append(f'| [{label(step["name"])}]({link}) | {state(step)} | {explanation} |')
        lines += ['', '</details>']
    backend = runs.get('Backend checks')
    if backend and results:
        backend_jobs = jobs.get(backend['id'], [])
        verify = next((job for job in backend_jobs if job['name'] == 'Backend verification'), None)
        all_steps = [{**step, 'jobId': job['id']} for job in backend_jobs for step in job.get('steps', [])]
        steps = {key: next((step for step in all_steps if step['name'] == title), {})
                 for key, title, _ in ci.STAGES}
        detail = ci.render_comment(backend, steps, results, verify['id'] if verify else None)
        lines += ['', '<details><summary>Tests, per-class coverage and Sonar findings</summary>', '',
                  detail.replace(ci.MARKER, ''), '</details>']
    else:
        lines += ['', '**Measurements: ⚠️ Unavailable.** Detailed measurements are unavailable. Do not infer coverage or a Sonar gate result from job status.']
    body = '\n'.join(lines) + '\n'
    if len(body.encode('utf-8')) > 60_000:
        # Keep the complete check overview rather than producing an invalid API request.
        body = '\n'.join(lines[:12]) + '\n\nStep details exceed the comment limit; open the linked workflow reports.\n'
    return body


def eligible_pr(pr, repo, sha):
    return (pr['state'] == 'open' and pr['head']['sha'] == sha and
            pr['base']['ref'] == 'main' and pr['base']['repo']['full_name'] == repo)


def measurements(repo, backend, pr, sha):
    if not backend or backend.get('status') != 'completed':
        return None
    try:
        results = ci.download_results(repo, backend)
        if (results.get('schemaVersion') != 2 or results['prHeadSha'] != sha or
                results['baseSha'] != pr['base']['sha']):
            return None
        commit = ci.github_request(f'/repos/{repo}/commits/{results["testedSha"]}')
        if [parent['sha'] for parent in commit.get('parents', [])] == [pr['base']['sha'], sha]:
            return results
    except (ValueError, KeyError, TypeError, zipfile.BadZipFile, urllib.error.URLError):
        print('Detailed quality artifact unavailable or invalid.')
    return None


def publish_team_comment(repo, run_id):
    if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repo):
        raise ValueError('Invalid repository')
    trigger = ci.github_request(f'/repos/{repo}/actions/runs/{int(run_id)}')
    name = trigger.get('name')
    if (name not in ci.WORKFLOWS or trigger.get('path') != ci.WORKFLOWS[name] or
            trigger.get('status') != 'completed' or trigger.get('repository', {}).get('full_name') != repo):
        raise ValueError('Expected a completed allowlisted workflow in this repository')
    sha = trigger['head_sha']
    if name == 'Publish dependency graph':
        # Bind even a failed publisher to its producer, without trusting PR artifacts.
        match = re.fullmatch(r'Dependency publication (\d+) attempt (\d+)', trigger.get('display_title', ''))
        if not match:
            raise ValueError('Publisher title does not bind its producer')
        producer = ci.github_request(f'/repos/{repo}/actions/runs/{int(match[1])}')
        if (producer.get('name') != 'Dependency graph' or
                producer.get('path') != ci.WORKFLOWS['Dependency graph'] or
                producer.get('repository', {}).get('full_name') != repo or
                producer.get('run_attempt') != int(match[2])):
            raise ValueError('Publisher references another workflow or attempt')
        sha = producer['head_sha']
    if not re.fullmatch(r'[a-f0-9]{40}', sha):
        raise ValueError('Invalid source revision')
    for candidate in ci.pages(f'/repos/{repo}/commits/{sha}/pulls'):
        number = int(candidate['number'])
        pr = ci.github_request(f'/repos/{repo}/pulls/{number}')
        if not eligible_pr(pr, repo, sha):
            continue
        runs = latest_runs(ci.pages(f'/repos/{repo}/actions/runs?head_sha={sha}', 'workflow_runs'), sha)
        receipt = publication_receipt(repo, sha)
        producer = runs.get('Dependency graph')
        if producer:
            title = f'Dependency publication {producer["id"]} attempt {producer["run_attempt"]}'
            publishers = ci.pages(f'/repos/{repo}/actions/workflows/dependency-publish.yml/runs', 'workflow_runs')
            candidates = [r for r in publishers if r.get('display_title') == title and
                          r.get('name') == 'Publish dependency graph' and r.get('event') == 'workflow_run' and
                          r.get('path') == ci.WORKFLOWS['Publish dependency graph'] and
                          r.get('repository', {}).get('full_name') == repo]
            if candidates:
                runs['Publish dependency graph'] = max(candidates, key=lambda r: (r['id'], r['run_attempt']))
        if receipt and 'Publish dependency graph' not in runs:
            publisher = ci.github_request(f'/repos/{repo}/actions/runs/{receipt["publisherRunId"]}')
            runs['Publish dependency graph'] = publisher
        jobs = {run['id']: ci.pages(f'/repos/{repo}/actions/runs/{run["id"]}/attempts/{run["run_attempt"]}/jobs', 'jobs')
                for run in runs.values()}
        backend = runs.get('Backend checks')
        results = measurements(repo, backend, pr, sha)
        body = render_team_report(repo, sha, runs, jobs, results)
        comments = ci.pages(f'/repos/{repo}/issues/{number}/comments')
        existing = next((c for c in comments if c['user']['login'] == 'github-actions[bot]' and
                         c['user']['type'] == 'Bot' and c['body'].startswith(ci.MARKER)), None)
        current = ci.github_request(f'/repos/{repo}/pulls/{number}')
        if not eligible_pr(current, repo, sha) or current['base']['sha'] != pr['base']['sha']:
            continue
        if existing:
            ci.github_request(f'/repos/{repo}/issues/comments/{int(existing["id"])}', 'PATCH', {'body': body})
        else:
            ci.github_request(f'/repos/{repo}/issues/{number}/comments', 'POST', {'body': body})
        print(f'Updated the current check report for PR #{number}.')
