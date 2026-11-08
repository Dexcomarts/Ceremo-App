"""Validate dependency data before a trusted workflow submits it to GitHub."""

import argparse
import io
import json
import os
from pathlib import Path
import re
import time
import zipfile

from ci_report import download_archive, github_request, pages, read_json

LIMIT = 5_000_000


def text(value, pattern, label):
    if not isinstance(value, str) or not re.fullmatch(pattern, value):
        raise ValueError('Invalid ' + label)
    return value


def clean_snapshot(data, sha, ref, repo, run_id, attempt):
    text(sha, r'[a-f0-9]{40}', 'commit')
    text(repo, r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', 'repository')
    if not isinstance(data, dict) or data.get('version') != 0 or data.get('sha') != sha or data.get('ref') != ref:
        raise ValueError('Dependency snapshot does not match its source revision')
    scanned = text(data.get('scanned'), r'\d{4}-\d\d-\d\dT[0-9:.]+(?:Z|[+-]\d\d:\d\d)', 'scan timestamp')
    manifests = data.get('manifests')
    if not isinstance(manifests, dict) or not 1 <= len(manifests) <= 100:
        raise ValueError('Expected a bounded nonempty dependency graph')
    clean = {}
    total = 0
    for name, manifest in manifests.items():
        text(name, r'[A-Za-z0-9_ .:/@+()=-]{1,500}', 'manifest name')
        if not isinstance(manifest, dict):
            raise ValueError('Invalid dependency manifest')
        resolved = manifest.get('resolved')
        if not isinstance(resolved, dict) or len(resolved) > 10_000:
            raise ValueError('Invalid resolved dependency inventory')
        packages = {}
        for key, package in resolved.items():
            if not isinstance(package, dict):
                raise ValueError('Invalid dependency package')
            text(key, r'[^\x00-\x20\x7f]{1,1000}', 'dependency key')
            item = {'package_url': text(package.get('package_url'),
                    r'pkg:[A-Za-z0-9/_.@%+:?=&~!$*(),;-]{1,1500}', 'package URL')}
            for field, choices in (('relationship', ('direct', 'indirect')),
                                   ('scope', ('runtime', 'development'))):
                if field in package:
                    if package[field] not in choices:
                        raise ValueError('Invalid dependency ' + field)
                    item[field] = package[field]
            edges = package.get('dependencies', [])
            if not isinstance(edges, list) or len(edges) > 10_000 or any(
                    not isinstance(edge, str) or edge not in resolved for edge in edges):
                raise ValueError('Dependency edges do not match the resolved inventory')
            item['dependencies'] = edges
            packages[key] = item
        total += len(packages)
        clean[name] = {'name': name, 'resolved': packages}
    if not 1 <= total <= 20_000:
        raise ValueError('Dependency graph is empty or excessive')
    result = {'version': 0, 'sha': sha, 'ref': ref, 'scanned': scanned,
              'job': {'id': f'{int(run_id)}-{int(attempt)}', 'correlator': 'ceremo-gradle',
                      'html_url': f'https://github.com/{repo}/actions/runs/{int(run_id)}'},
              'detector': {'name': 'ceremo-gradle', 'version': '1.0',
                           'url': 'https://github.com/gradle/github-dependency-graph-gradle-plugin'},
              'manifests': clean}
    if len(json.dumps(result).encode()) > LIMIT:
        raise ValueError('Dependency graph exceeds its size limit')
    return result


def source_context(run, repo, attempt):
    if (run.get('name') != 'Dependency graph' or
            run.get('path') != '.github/workflows/dependency-submission.yml' or
            run.get('repository', {}).get('full_name') != repo or
            run.get('status') != 'completed' or run.get('conclusion') != 'success' or
            run.get('run_attempt') != attempt):
        raise ValueError('Expected the successful dependency workflow and exact attempt')
    sha = text(run.get('head_sha'), r'[a-f0-9]{40}', 'source commit')
    if run.get('event') in ('push', 'schedule', 'workflow_dispatch') and run.get('head_branch') == 'main':
        return sha, 'refs/heads/main'
    if run.get('event') == 'pull_request':
        matches = [pr for pr in pages(f'/repos/{repo}/commits/{sha}/pulls')
                   if pr['state'] == 'open' and pr['head']['sha'] == sha and
                   pr['base']['ref'] == 'main' and pr['base']['repo']['full_name'] == repo]
        if len(matches) == 1:
            number = int(matches[0]['number'])
            current = github_request(f'/repos/{repo}/pulls/{number}')
            if (current['state'] == 'open' and current['head']['sha'] == sha and
                    current['base']['ref'] == 'main' and current['base']['repo']['full_name'] == repo):
                return sha, f'refs/pull/{number}/merge'
    raise ValueError('Unsupported or obsolete dependency source')


def prepare():
    files = list(Path('build/reports/dependencies/raw').glob('*.json'))
    if len(files) != 1:
        raise ValueError('Expected exactly one Gradle snapshot')
    data = clean_snapshot(read_json(files[0], LIMIT), os.environ['SNAPSHOT_SHA'],
                          os.environ['SNAPSHOT_REF'], os.environ['GITHUB_REPOSITORY'],
                          os.environ['GITHUB_RUN_ID'], os.environ['GITHUB_RUN_ATTEMPT'])
    target = Path('build/reports/dependencies/snapshot.json')
    target.write_text(json.dumps(data) + '\n', encoding='utf-8')
    print(f'Validated {sum(len(m["resolved"]) for m in data["manifests"].values())} dependency entries.')


def publish(run_id, attempt):
    repo = text(os.environ['GITHUB_REPOSITORY'], r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', 'repository')
    run = github_request(f'/repos/{repo}/actions/runs/{run_id}')
    sha, ref = source_context(run, repo, attempt)
    if ref == 'refs/heads/main':
        if github_request(f'/repos/{repo}/branches/main')['commit']['sha'] != sha:
            raise ValueError('Main has moved; do not publish an obsolete snapshot')
    jobs = pages(f'/repos/{repo}/actions/runs/{run_id}/attempts/{attempt}/jobs', 'jobs')
    if len(jobs) != 1 or jobs[0]['name'] != 'Resolve Gradle dependencies' or jobs[0]['conclusion'] != 'success':
        raise ValueError('Dependency generation job was not successful')
    name = f'ceremo-dependencies-{run_id}-{attempt}'
    artifacts = pages(f'/repos/{repo}/actions/runs/{run_id}/artifacts', 'artifacts')
    matches = [a for a in artifacts if a['name'] == name and not a['expired']]
    if len(matches) != 1:
        raise ValueError('Missing or ambiguous dependency artifact')
    raw = download_archive(repo, matches[0]['id'])
    with zipfile.ZipFile(io.BytesIO(raw)) as archive:
        if archive.namelist() != ['snapshot.json'] or archive.getinfo('snapshot.json').file_size > LIMIT:
            raise ValueError('Expected one bounded dependency data file')
        snapshot = clean_snapshot(json.loads(archive.read('snapshot.json')), sha, ref, repo, run_id, attempt)
    # Revalidate the attempt and PR/ref after all reads, before the only write.
    current = github_request(f'/repos/{repo}/actions/runs/{run_id}')
    if source_context(current, repo, attempt) != (sha, ref):
        raise ValueError('Dependency source changed during validation')
    if ref == 'refs/heads/main' and github_request(f'/repos/{repo}/branches/main')['commit']['sha'] != sha:
        raise ValueError('Main changed during validation')
    accepted = github_request(f'/repos/{repo}/dependency-graph/snapshots', 'POST', snapshot)
    if accepted.get('result') != 'SUCCESS':
        raise ValueError('GitHub did not confirm dependency snapshot acceptance')
    receipt = {'sha': sha, 'sourceRunId': run_id, 'sourceAttempt': attempt, 'accepted': True}
    target = Path('build/reports/dependencies/publication.json')
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(receipt) + '\n', encoding='utf-8')
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a', encoding='utf-8') as output:
            output.write(f'sourceSha={sha}\n')
    print('GitHub accepted the verified dependency snapshot.')


def publication_receipt(repo, sha):
    artifacts = pages(f'/repos/{repo}/actions/artifacts?name=dependency-publication-{sha}', 'artifacts')
    for artifact in artifacts[:20]:
        if artifact['expired'] or artifact['name'] != f'dependency-publication-{sha}':
            continue
        run = github_request(f'/repos/{repo}/actions/runs/{int(artifact["workflow_run"]["id"])}')
        if (run.get('name') != 'Publish dependency graph' or run.get('event') != 'workflow_run' or
                run.get('path') != '.github/workflows/dependency-publish.yml' or
                run.get('repository', {}).get('full_name') != repo or run.get('conclusion') != 'success'):
            continue
        raw = download_archive(repo, artifact['id'])
        with zipfile.ZipFile(io.BytesIO(raw)) as archive:
            if archive.namelist() != ['publication.json'] or archive.getinfo('publication.json').file_size > 4096:
                raise ValueError('Invalid publication receipt archive')
            receipt = json.loads(archive.read('publication.json'))
        if (receipt.get('sha') != sha or receipt.get('accepted') is not True or
                type(receipt.get('sourceRunId')) is not int or type(receipt.get('sourceAttempt')) is not int):
            raise ValueError('Publication receipt does not bind its source')
        source = github_request(f'/repos/{repo}/actions/runs/{receipt["sourceRunId"]}')
        if (source.get('head_sha') != sha or source.get('name') != 'Dependency graph' or
                source.get('path') != '.github/workflows/dependency-submission.yml' or
                source.get('repository', {}).get('full_name') != repo):
            raise ValueError('Publication receipt references another workflow')
        receipt['publisherRunId'] = int(run['id'])
        return receipt
    return None


def require_publications(repo, base, head, timeout=600):
    text(repo, r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', 'repository')
    for sha in (base, head):
        text(sha, r'[a-f0-9]{40}', 'snapshot commit')
    deadline = time.monotonic() + timeout
    pending = {base, head}
    while pending:
        pending = {sha for sha in pending if publication_receipt(repo, sha) is None}
        if not pending:
            print('Accepted base and candidate snapshots verified.')
            return
        if time.monotonic() >= deadline:
            raise ValueError('Required accepted dependency snapshot is unavailable; review cannot pass')
        print(f'Waiting for {len(pending)} accepted dependency snapshot(s).')
        time.sleep(min(10, max(0, deadline - time.monotonic())))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=('prepare', 'publish', 'ready'))
    parser.add_argument('--run-id', type=int)
    parser.add_argument('--attempt', type=int)
    parser.add_argument('--base')
    parser.add_argument('--head')
    args = parser.parse_args()
    if args.command == 'prepare':
        prepare()
    elif args.command == 'ready':
        require_publications(os.environ['GITHUB_REPOSITORY'], args.base, args.head)
    else:
        if not args.run_id or not args.attempt:
            parser.error('Publication requires a positive source run and attempt')
        publish(args.run_id, args.attempt)
