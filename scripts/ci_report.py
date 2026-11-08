"""Build CI summaries and publish PR comments using only the Python standard library."""

import argparse
import io
import json
import os
from pathlib import Path
import re
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

MARKER = "<!-- ceremo-backend-report -->"
WORKFLOWS = {
    'Backend checks': '.github/workflows/backend.yml',
    'CodeQL': '.github/workflows/codeql.yml',
    'Dependency graph': '.github/workflows/dependency-submission.yml',
    'Publish dependency graph': '.github/workflows/dependency-publish.yml',
    'Dependency review': '.github/workflows/dependency-review.yml',
}
WORKFLOW_DESCRIPTIONS = {
    'Backend checks': 'Workflow validation, redacted secret scan, both Java suites, coverage and Sonar.',
    'CodeQL': 'Analyse Java, Python and workflow code; enforce critical/high findings.',
    'Dependency graph': 'Resolve the complete Gradle inventory with strict verification.',
    'Publish dependency graph': 'Trusted validation and submission of the generated snapshot.',
    'Dependency review': 'Compare base/head snapshots and reject critical/high vulnerabilities.',
}


def workflow_name(run):
    """Validate the workflow path even when GitHub exposes a dynamic run name."""
    expected = next((name for name, path in WORKFLOWS.items() if run.get('path') == path), None)
    if expected and run.get('name') == expected:
        return expected
    if (expected == 'Publish dependency graph' and run.get('event') == 'workflow_run' and
            run.get('name') == run.get('display_title') and
            re.fullmatch(r'Dependency publication [1-9][0-9]* attempt [1-9][0-9]*', run.get('name', ''))):
        return expected
    return None


STAGES = [
    ("source", "Check out application", "Check out the commit under review."),
    ("java", "Set up Java 25", "Configure the Java toolchain."),
    ("reportTests", "Test CI reporting", "Verify summary formatting and safe PR publication."),
    ("linux", "Prepare Linux limits", "Apply SonarQube's required Linux limits."),
    ("helper", "Build token helper", "Build the isolated credential bootstrap image."),
    ("bootstrapTests", "Test token bootstrap", "Check token scope, credential handling and failure paths."),
    ("server", "Start SonarQube", "Wait for the temporary server to become healthy."),
    ("bootstrap", "Create analysis token", "Create the private project and scoped analysis token."),
    ("unit", "Run regular tests", "Compile and run the regular test suite."),
    ("integration", "Run PostgreSQL tests", "Verify database constraints, transactions and security integration."),
    ("coverage", "Verify per-class coverage", "Require zero missed lines and branches in every application class."),
    ("package", "Build application", "Create the executable application JAR."),
    ("sonar", "Run Sonar analysis", "Map both JUnit suites, analyse the tested revision and wait for the server's quality gate."),
    ("gate", "Verify Sonar quality gate", "Require evaluated overall-code conditions, maintainability A and matching imported test totals."),
    ("cleanup", "Remove temporary services", "Delete containers and all data/credential volumes; check for leftovers."),
    ("collect", "Collect report data", "Collect numeric test, coverage and gate results without raw logs."),
    ("artifacts", "Upload quality reports", "Save the allowlisted reports for seven days."),
]
METRICS = {
    "bugs": "Reliability issues", "vulnerabilities": "Vulnerabilities",
    "coverage": "Coverage (%)", "duplicated_lines_density": "Duplication (%)",
    "security_hotspots_reviewed": "Hotspots reviewed (%)",
    "sqale_rating": "Maintainability rating (1=A, 5=E)",
    "new_coverage": "New-code coverage (%)", "new_violations": "New-code issues",
    "new_duplicated_lines_density": "New-code duplication (%)",
    "new_security_hotspots_reviewed": "New-code hotspots reviewed (%)",
}
STATES = {
    "success": "✅ Passed", "failure": "❌ Failed", "skipped": "⏭️ Skipped",
    "cancelled": "⚪ Cancelled", "timed_out": "❌ Timed out",
    "action_required": "⚠️ Action required", "neutral": "⚪ Neutral",
    "pending": "⏳ Pending", "unknown": "⚠️ Unavailable",
}
FAILURES = {
    "source": "Checkout failed; verify the repository/ref and runner access in the logs.",
    "java": "Java setup failed; inspect toolchain download and runtime errors.",
    "reportTests": "A reporting regression failed; the report must be repaired before relying on it.",
    "linux": "The required Linux limits could not be applied; SonarQube was not started.",
    "helper": "The token helper image did not build; inspect the Docker build error.",
    "bootstrapTests": "A credential/bootstrap regression failed; later bootstrap steps were stopped.",
    "server": "The SonarQube startup command failed; inspect container health, image downloads and Linux requirements.",
    "bootstrap": "Project/token bootstrap did not complete; inspect authentication errors and fresh-volume requirements.",
    "unit": "Compilation or regular-test execution failed; test counts below distinguish reported test failures from missing results.",
    "integration": "PostgreSQL integration execution failed; inspect the database/container and test results.",
    "coverage": "Required coverage verification failed; inspect missing execution inputs or uncovered lines/branches below.",
    "package": "The application JAR could not be built; inspect packaging and compilation errors.",
    "sonar": "Test-report conversion, scanning or quality-gate wait failed; inspect the report task and scanner logs.",
    "gate": "The required gate or test-import verification failed; inspect the evaluated conditions and imported totals below.",
    "cleanup": "Cleanup or its residual-resource check failed; temporary data/credential volumes may remain.",
    "collect": "Report data could not be collected or validated; detailed measurements are unavailable.",
    "artifacts": "GitHub did not confirm report upload; inspect the artifact-service error.",
}


def read_json(path, limit=1_000_000):
    data = Path(path).read_bytes()
    if len(data) > limit:
        raise ValueError("Report exceeds its size limit")
    return json.loads(data)


def xml_root(path):
    data = Path(path).read_bytes()
    if len(data) > 10_000_000 or b"<!ENTITY" in data or b"\x00" in data:
        raise ValueError("Unsafe or oversized XML report")
    return ET.fromstring(data)


def test_totals(directory):
    files = list(Path(directory).glob("TEST-*.xml"))
    if not files:
        return None
    totals = dict.fromkeys(("tests", "failures", "errors", "skipped"), 0)
    for path in files:
        root = xml_root(path)
        if root.tag != "testsuite":
            raise ValueError("Unexpected JUnit report")
        for key in totals:
            totals[key] += int(root.attrib[key])
    return totals


def coverage_totals(path):
    if not Path(path).is_file():
        return None
    root = xml_root(path)
    counters = {c.attrib["type"]: c.attrib for c in root.findall("counter")}
    return {
        "lines": {k: int(counters["LINE"][k]) for k in ("covered", "missed")},
        "branches": {k: int(counters["BRANCH"][k]) for k in ("covered", "missed")},
        "classesWithMisses": sum(any(c.attrib["type"] in ("LINE", "BRANCH") and
            int(c.attrib["missed"]) > 0 for c in cls.findall("counter"))
            for cls in root.findall("package/class")),
    }


def validate_results(data, sha):
    if not isinstance(data, dict) or not re.fullmatch(r"[0-9a-f]{40}", sha) or data.get("schemaVersion") not in (1, 2) or data.get("sha") != sha:
        raise ValueError("Report version or commit does not match the run")

    def count(value):
        if type(value) is not int or not 0 <= value <= 100_000_000:
            raise ValueError("Invalid report counter")

    clean = {"schemaVersion": data['schemaVersion'], "sha": sha}
    if data['schemaVersion'] == 2:
        for key in ('testedSha', 'prHeadSha', 'baseSha'):
            value = data.get(key)
            if not isinstance(value, str) or not re.fullmatch(r'[0-9a-f]{40}', value):
                raise ValueError('Missing or invalid tested revision metadata')
            clean[key] = value
        if clean['prHeadSha'] != sha:
            raise ValueError('Reported PR head does not match the run')
    for suite in ("unit", "integration"):
        clean[suite] = None
        if data.get(suite) is not None:
            if not isinstance(data[suite], dict):
                raise ValueError("Invalid test totals")
            clean[suite] = {k: data[suite][k] for k in ("tests", "failures", "errors", "skipped")}
            for value in clean[suite].values():
                count(value)
    clean["coverage"] = None
    if data.get("coverage") is not None:
        coverage = data["coverage"]
        if not isinstance(coverage, dict) or any(not isinstance(coverage.get(k), dict) for k in ("lines", "branches")):
            raise ValueError("Invalid coverage totals")
        clean["coverage"] = {k: {v: coverage[k][v] for v in ("covered", "missed")}
                             for k in ("lines", "branches")}
        clean["coverage"]["classesWithMisses"] = coverage["classesWithMisses"]
        for counter in (coverage["lines"], coverage["branches"]):
            for key in ("covered", "missed"):
                count(counter[key])
        count(coverage["classesWithMisses"])
    clean["sonar"] = None
    if data.get("sonar") is not None:
        gate = data["sonar"]
        if not isinstance(gate, dict) or gate.get("status") not in ("OK", "ERROR", "UNAVAILABLE") or type(gate.get("policyEvaluated")) is not bool:
            raise ValueError("Invalid gate status")
        conditions = []
        if not isinstance(gate.get("conditions", []), list) or len(gate.get("conditions", [])) > len(METRICS):
            raise ValueError("Too many gate conditions")
        for condition in gate.get("conditions", []):
            if not isinstance(condition, dict) or condition.get("metric") not in METRICS or condition.get("status") not in ("OK", "ERROR", "NO_VALUE"):
                raise ValueError("Unknown gate condition")
            item = {"metric": condition["metric"], "status": condition["status"]}
            for key in ("value", "threshold"):
                value = condition.get(key)
                if value is not None and (not isinstance(value, str) or
                        not re.fullmatch(r"[0-9]{1,9}(\.[0-9]{1,6})?", value)):
                    raise ValueError("Invalid gate measurement")
                item[key] = value
            conditions.append(item)
        findings = []
        if not isinstance(gate.get("findings", []), list) or len(gate.get("findings", [])) > 1000:
            raise ValueError("Invalid source findings")
        for finding in gate.get("findings", []):
            if not isinstance(finding, dict) or not re.fullmatch(r"[a-z][a-z0-9_-]*:S[0-9]{1,6}", finding.get("rule", "")):
                raise ValueError("Invalid rule identifier")
            file = finding.get("file", "")
            if not isinstance(file, str) or not re.fullmatch(r"(?:(?:src|scripts|\.github/workflows)/[A-Za-z0-9_./-]{1,300}|(?:build|settings)\.gradle\.kts)", file) or ".." in file.split("/"):
                raise ValueError("Invalid source path")
            line = finding.get("line")
            if line is not None and (type(line) is not int or not 1 <= line <= 1_000_000):
                raise ValueError("Invalid source line")
            item = {"rule": finding["rule"], "file": file, "line": line}
            if "type" in finding:
                if finding["type"] not in ("BUG", "VULNERABILITY", "CODE_SMELL"):
                    raise ValueError("Invalid finding type")
                item["type"] = finding["type"]
            findings.append(item)
        clean["sonar"] = {"status": gate["status"], "policyEvaluated": gate["policyEvaluated"],
                          "conditions": conditions, "findings": findings}
        if "measures" in gate:
            allowed = {"bugs", "vulnerabilities", "code_smells", "sqale_rating", "tests",
                       "test_failures", "test_errors", "skipped_tests", "security_hotspots"}
            if not isinstance(gate["measures"], dict) or set(gate["measures"]) - allowed:
                raise ValueError("Invalid Sonar measures")
            for value in gate["measures"].values():
                if not isinstance(value, str) or not re.fullmatch(r"[0-9]{1,9}(\.[0-9]{1,6})?", value):
                    raise ValueError("Invalid Sonar measurement")
            rating = gate["measures"].get("sqale_rating")
            if rating is not None and rating not in {str(n) for n in range(1, 6)} | {f"{n}.0" for n in range(1, 6)}:
                raise ValueError("Invalid maintainability rating")
            clean["sonar"]["measures"] = gate["measures"].copy()
        if "testImportVerified" in gate:
            if type(gate["testImportVerified"]) is not bool:
                raise ValueError("Invalid test import status")
            clean["sonar"]["testImportVerified"] = gate["testImportVerified"]
    return clean


def collect_results(sha, root=Path("build")):
    gate_path = root / "reports/sonar/quality-gate.json"
    gate = None
    if gate_path.is_file() and gate_path.stat().st_size:
        gate = read_json(gate_path)
    revision = {'schemaVersion': 1, 'sha': sha}
    if os.environ.get('TESTED_SHA'):
        revision.update(schemaVersion=2, testedSha=os.environ['TESTED_SHA'],
                        prHeadSha=os.environ['PR_HEAD_SHA'], baseSha=os.environ['BASE_SHA'])
    return validate_results({**revision,
        "unit": test_totals(root / "test-results/test"),
        "integration": test_totals(root / "test-results/integrationTest"),
        "coverage": coverage_totals(root / "reports/jacoco/combined/combined.xml"),
        "sonar": gate}, sha)


def render_comment(run, steps, results=None, job_id=None):
    sha = run["head_sha"]
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("Invalid commit")
    repo = run["repository"]["full_name"]
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repo):
        raise ValueError("Invalid repository")
    run_id, attempt = int(run["id"]), int(run.get("run_attempt", 1))
    url = f"https://github.com/{repo}/actions/runs/{run_id}"
    lines = [MARKER, f"<!-- ceremo-run:{run_id}:{attempt} -->",
        f"## CEREMO backend checks — {STATES.get(run['conclusion'], STATES['unknown'])}",
        f"Commit `{sha[:12]}` · Attempt {attempt} · [Workflow and reports]({url})", "",
        "| Check | Result | Description | Logs |", "| --- | --- | --- | --- |"]
    if results and results['schemaVersion'] == 2:
        lines[4:4] = [f"Tested revision `{results['testedSha']}` · PR head `{results['prHeadSha']}` · Base `{results['baseSha']}`", '']
    for stage, title, description in STAGES:
        step = steps.get(stage, {"conclusion": "unknown"})
        state = step.get("conclusion", "unknown")
        detail = description
        if state == "failure":
            detail = FAILURES[stage]
        elif state == "skipped":
            detail = "Not run; an earlier prerequisite failed or the stage did not apply."
        elif state in ("cancelled", "timed_out"):
            detail = "Execution was interrupted; no successful result is claimed."
        if results and stage in ("unit", "integration") and results.get(stage):
            total = results[stage]
            detail += f" Reported: {total['tests']} tests, {total['failures']} failures, {total['errors']} errors, {total['skipped']} skipped."
        if results and stage == "coverage" and results.get("coverage"):
            coverage = results["coverage"]
            detail += (f" Missed: {coverage['lines']['missed']} lines, {coverage['branches']['missed']} branches; "
                       f"{coverage['classesWithMisses']} classes below the required coverage.")
        if stage in ("sonar", "gate") and results and results.get("sonar"):
            gate = results["sonar"]
            failed = [f"{METRICS[c['metric']]}: {c['value']} (required threshold {c['threshold']})"
                      for c in gate["conditions"] if c["status"] == "ERROR"]
            if failed:
                detail += " Gate blockers: " + "; ".join(failed) + "."
            elif gate["status"] == "UNAVAILABLE" or not gate["policyEvaluated"]:
                detail += " A completed, evaluated gate result is unavailable."
            if stage == "gate" and gate.get("testImportVerified") is False:
                detail += " Sonar's imported test totals do not match the real JUnit results."
        stage_job = step.get('jobId', job_id)
        log = f"[Logs]({url}/job/{int(stage_job)}#step:{int(step['number'])}:1)" if stage_job and step.get("number") else "—"
        lines.append(f"| {title} | {STATES.get(state, STATES['unknown'])} | {detail} | {log} |")
    gate = (results or {}).get("sonar") or {}
    measures = gate.get("measures", {})
    if measures:
        rating = measures.get("sqale_rating")
        grade = "ABCDE"[int(float(rating)) - 1] if rating else "unavailable"
        lines += ["", "**Sonar analysis:** " + "; ".join(
            f"{label}: {measures.get(key, 'unavailable')}" for key, label in (
                ("bugs", "bugs"), ("vulnerabilities", "vulnerabilities"),
                ("code_smells", "code smells"),
                ("tests", "imported tests"))) + ".",
            f"Maintainability: {grade} (required: A).",
            "Java test import verified against JUnit: " +
            ("✅ Yes" if gate.get("testImportVerified") is True else "⚠️ Not verified") + ".",
            "A passing A-rating gate can still contain code smells; all reported source locations are retained in the artifact."]
    if gate.get("findings"):
        lines += ["", "**Source findings:**"]
        for finding in gate["findings"][:10]:
            anchor = f"#L{finding['line']}" if finding["line"] else ""
            lines.append(f"- [{finding['rule']} — {finding['file']}]"
                         f"(https://github.com/{repo}/blob/{results.get('testedSha', sha)}/{finding['file']}{anchor})")
        if len(gate["findings"]) > 10:
            lines.append(f"Showing 10 of {len(gate['findings'])} open findings. The quality artifact contains the complete list.")
    lines += ["", "**How to investigate:** open the failed stage's logs and the quality report artifact. "
              "A failed test/build may prevent later checks from running. Missing data is not a passing result."]
    if results is None:
        lines += ["", "⚠️ Detailed measurements are unavailable. Results above come from GitHub's recorded step outcomes."]
    return "\n".join(lines) + "\n"


def github_request(path, method="GET", body=None, include_headers=False):
    payload = json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request("https://api.github.com" + path, data=payload, method=method,
        headers={"Authorization": "Bearer " + os.environ["GH_TOKEN"],
                 "Accept": "application/vnd.github+json", "Content-Type": "application/json",
                 "X-GitHub-Api-Version": "2022-11-28"})
    with urllib.request.urlopen(request, timeout=30) as response:
        data = json.load(response)
        if include_headers:
            return data, {key.lower(): value for key, value in response.headers.items()}
        return data


def pages(path, key=None):
    items = []
    for page in range(1, 101):
        result = github_request(f"{path}{'&' if '?' in path else '?'}per_page=100&page={page}")
        batch = result[key] if key else result
        items.extend(batch)
        if len(batch) < 100:
            return items
    raise ValueError("GitHub result exceeds pagination limit")


def download_results(repo, run):
    artifacts = pages(f"/repos/{repo}/actions/runs/{run['id']}/artifacts", "artifacts")
    name = f"backend-quality-{run['id']}-{run['run_attempt']}"
    matches = [a for a in artifacts if a["name"] == name and not a["expired"]]
    if len(matches) != 1:
        return None

    archive = download_archive(repo, matches[0]['id'])
    with zipfile.ZipFile(io.BytesIO(archive)) as files:
        # Read a single bounded data entry; never extract or execute artifact files.
        if files.namelist().count('ci/result.json') != 1:
            raise ValueError('Expected one result entry')
        info = files.getinfo("ci/result.json")
        if info.file_size > 1_000_000:
            raise ValueError("Result entry exceeds its size limit")
        return validate_results(json.loads(files.read(info)), run["head_sha"])


def download_archive(repo, artifact_id):
    """Download bounded data without forwarding credentials to artifact storage."""
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, request, fp, code, msg, headers, newurl):
            return None

    request = urllib.request.Request(
        f"https://api.github.com/repos/{repo}/actions/artifacts/{int(artifact_id)}/zip",
        headers={"Authorization": "Bearer " + os.environ["GH_TOKEN"], "Accept": "application/vnd.github+json"})
    try:
        urllib.request.build_opener(NoRedirect()).open(request, timeout=30)
        raise ValueError("Expected a GitHub artifact redirect")
    except urllib.error.HTTPError as error:
        if error.code != 302:
            raise ValueError(f"Artifact request failed with HTTP {error.code}") from None
        location = error.headers["Location"]
    # Never forward the GitHub token to the signed artifact storage URL.
    if not location.startswith("https://"):
        raise ValueError("Artifact redirect must use HTTPS")
    try:
        with urllib.request.urlopen(location, timeout=30) as response:
            archive = response.read(8_000_001)
    except urllib.error.HTTPError as error:
        raise ValueError(f"Artifact download failed with HTTP {error.code}") from None
    if len(archive) > 8_000_000:
        raise ValueError("Artifact exceeds its download limit")
    return archive


def publish_comment(repo, run_id):
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repo):
        raise ValueError("Invalid repository")
    run = github_request(f"/repos/{repo}/actions/runs/{int(run_id)}")
    if run["name"] != "Backend checks" or run["path"] != ".github/workflows/backend.yml" or run["status"] != "completed":
        raise ValueError("Expected a completed Backend checks run")
    jobs = pages(f"/repos/{repo}/actions/runs/{run['id']}/attempts/{run['run_attempt']}/jobs", "jobs")
    matching = [j for j in jobs if j["name"] == "verify"]
    if len(matching) != 1:
        raise ValueError("Expected exactly one verification job")
    job = matching[0]
    steps = {stage: next((s for s in job["steps"] if s["name"] == title), {"conclusion": "unknown"})
             for stage, title, _ in STAGES}
    try:
        results = download_results(repo, run)
    except (ValueError, KeyError, TypeError, zipfile.BadZipFile, urllib.error.URLError):
        print("Detailed report unavailable or invalid; using GitHub step outcomes.")
        results = None
    body = render_comment(run, steps, results, job["id"])
    prs = pages(f"/repos/{repo}/commits/{run['head_sha']}/pulls")
    published = 0
    for candidate in prs:
        pr = github_request(f"/repos/{repo}/pulls/{int(candidate['number'])}")
        if pr["state"] != "open" or pr["base"]["repo"]["full_name"] != repo or pr["head"]["sha"] != run["head_sha"]:
            continue
        number = int(pr["number"])
        comments = pages(f"/repos/{repo}/issues/{number}/comments")
        existing = next((c for c in comments if c["user"]["login"] == "github-actions[bot]" and
                         c["user"]["type"] == "Bot" and c["body"].startswith(MARKER)), None)
        if existing:
            previous = re.search(r"<!-- ceremo-run:(\d+):(\d+) -->", existing["body"])
            if previous and tuple(map(int, previous.groups())) > (run["id"], run["run_attempt"]):
                continue
        # Recheck after API reads so an older commit cannot overwrite a newer PR result.
        current = github_request(f"/repos/{repo}/pulls/{number}")
        if current["state"] != "open" or current["head"]["sha"] != run["head_sha"]:
            continue
        if existing:
            github_request(f"/repos/{repo}/issues/comments/{int(existing['id'])}", "PATCH", {"body": body})
        else:
            github_request(f"/repos/{repo}/issues/{number}/comments", "POST", {"body": body})
        published += 1
    print(f"Updated {published} current pull request report(s).")
    if published == 0:
        print("No report changed: no matching current PR, or a newer report already exists.")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("collect", "summary", "publish", "resolve"))
    parser.add_argument("--run-id", type=int)
    args = parser.parse_args()
    sha = os.environ.get("REPORT_SHA", "")
    path = Path("build/reports/ci/result.json")
    if args.command == "collect":
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(collect_results(sha), indent=2) + "\n", encoding="utf-8")
    elif args.command == "summary":
        steps = json.loads(os.environ["STEP_RESULTS"])
        outcomes = {key: {"conclusion": value.get("outcome", "unknown")} for key, value in steps.items()}
        failed = any(v["conclusion"] == "failure" for v in outcomes.values())
        run = {"id": int(os.environ["GITHUB_RUN_ID"]), "run_attempt": int(os.environ["GITHUB_RUN_ATTEMPT"]),
               "head_sha": sha, "repository": {"full_name": os.environ["GITHUB_REPOSITORY"]},
               "conclusion": "failure" if failed else "cancelled" if os.environ.get("RUN_CANCELLED") == "true" else "success"}
        results = validate_results(read_json(path), sha) if path.is_file() else None
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
            summary.write(render_comment(run, outcomes, results))
    else:
        if args.run_id is None:
            parser.error("--run-id is required for publication")
        from team_report import publish_team_comment, resolve_pull_request
        operation = resolve_pull_request if args.command == 'resolve' else publish_team_comment
        operation(os.environ["GITHUB_REPOSITORY"], args.run_id)


if __name__ == "__main__":
    main()
