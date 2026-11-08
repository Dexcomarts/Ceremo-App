import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch, MagicMock
import urllib.error
import zipfile

import ci_report as report

SHA = "a" * 40
REPO = "owner/project"


def results():
    return {"schemaVersion": 1, "sha": SHA,
            "unit": {"tests": 10, "failures": 0, "errors": 0, "skipped": 0},
            "integration": None, "coverage": None, "sonar": None}


def run():
    return {"id": 123, "run_attempt": 1, "head_sha": SHA, "name": "Backend checks",
            "path": ".github/workflows/backend.yml", "status": "completed",
            "conclusion": "failure", "repository": {"full_name": REPO}}


def pull(number=7, sha=SHA, state="open"):
    return {"number": number, "state": state, "head": {"sha": sha},
            "base": {"repo": {"full_name": REPO}}}


class ReportTest(unittest.TestCase):
    def test_collects_junit_and_per_class_coverage_without_failure_text(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            tests = root / "test-results/test"
            tests.mkdir(parents=True)
            (tests / "TEST-example.xml").write_text('<testsuite tests="3" failures="1" errors="0" skipped="1">'
                '<testcase><failure>private diagnostic text</failure></testcase></testsuite>')
            coverage = root / "reports/jacoco/combined"
            coverage.mkdir(parents=True)
            (coverage / "combined.xml").write_text('<!DOCTYPE report SYSTEM "report.dtd"><report><package>'
                '<class name="Example"><counter type="LINE" missed="0" covered="2"/>'
                '<counter type="BRANCH" missed="1" covered="1"/></class></package>'
                '<counter type="LINE" missed="0" covered="2"/>'
                '<counter type="BRANCH" missed="1" covered="1"/></report>')
            data = report.collect_results(SHA, root)
            self.assertEqual(data["unit"]["tests"], 3)
            self.assertEqual(data["unit"]["failures"], 1)
            self.assertEqual(data["coverage"]["classesWithMisses"], 1)
            self.assertEqual(data["coverage"]["branches"], {"covered": 1, "missed": 1})
            self.assertIsNone(data["integration"])
            self.assertNotIn("private diagnostic text", json.dumps(data))

    def test_no_reports_are_not_invented_as_zero_failures(self):
        with tempfile.TemporaryDirectory() as directory:
            data = report.collect_results(SHA, Path(directory))
            self.assertIsNone(data["unit"])
            self.assertIsNone(data["coverage"])
            self.assertIsNone(data["sonar"])

    def test_xml_entity_declarations_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "report.xml"
            path.write_text('<!DOCTYPE x [<!ENTITY secret SYSTEM "file:///private">]><x/>')
            with self.assertRaises(ValueError):
                report.xml_root(path)

    def test_render_distinguishes_passed_failed_and_skipped_checks(self):
        data = results()
        data["sonar"] = {"status": "ERROR", "policyEvaluated": True, "conditions": [
            {"metric": "bugs", "status": "ERROR", "value": "1", "threshold": "0"},
            {"metric": "security_hotspots_reviewed", "status": "ERROR", "value": "0.0", "threshold": "100"}]}
        steps = {"unit": {"conclusion": "success", "number": 9},
                 "gate": {"conclusion": "failure", "number": 14},
                 "package": {"conclusion": "skipped"}}
        body = report.render_comment(run(), steps, report.validate_results(data, SHA), 456)
        self.assertIn("Run regular tests | ✅ Passed", body)
        self.assertIn("Verify Sonar quality gate | ❌ Failed", body)
        self.assertIn("Build application | ⏭️ Skipped", body)
        self.assertIn("Reliability issues: 1 (required threshold 0)", body)
        self.assertIn("Hotspots reviewed (%): 0.0 (required threshold 100)", body)
        self.assertIn("/job/456#step:9:1", body)

    def test_missing_measurements_and_interrupted_steps_are_explicit(self):
        body = report.render_comment(run(), {"unit": {"conclusion": "cancelled"},
                                            "sonar": {"conclusion": "timed_out"}})
        self.assertIn("⚪ Cancelled", body)
        self.assertIn("❌ Timed out", body)
        self.assertIn("Detailed measurements are unavailable", body)

    def test_invalid_or_cross_commit_data_cannot_enter_comments(self):
        bad = []
        data = results(); data["sha"] = "b" * 40; bad.append(data)
        data = results(); data["unit"]["tests"] = True; bad.append(data)
        data = results(); data["unit"]["tests"] = -1; bad.append(data)
        data = results(); data["unit"] = "@someone"; bad.append(data)
        data = results(); data["coverage"] = {"lines": "bad"}; bad.append(data)
        for metric, value in (("unknown", "1"), ("bugs", "@someone"), ("bugs", "[click](https://invalid)")):
            data = results()
            data["sonar"] = {"status": "ERROR", "policyEvaluated": True,
                             "conditions": [{"metric": metric, "status": "ERROR", "value": value, "threshold": "0"}]}
            bad.append(data)
        for data in bad:
            with self.subTest(data=data), self.assertRaises((ValueError, KeyError)):
                report.validate_results(data, SHA)

    def test_unrecognised_fields_are_not_rendered(self):
        data = results(); data["rawLog"] = "@someone private text"
        clean = report.validate_results(data, SHA)
        self.assertNotIn("rawLog", clean)
        self.assertNotIn("@someone", report.render_comment(run(), {}, clean))

    def test_source_findings_link_to_the_exact_tested_revision(self):
        data = results()
        data["sonar"] = {"status": "ERROR", "policyEvaluated": True, "conditions": [],
            "findings": [{"rule": "java:S2259", "file": "src/main/java/Example.java", "line": 30}]}
        body = report.render_comment(run(), {}, report.validate_results(data, SHA))
        self.assertIn(f"/blob/{SHA}/src/main/java/Example.java#L30", body)
        for file in ("src/../private.txt", "https://untrusted.example", "src/@someone.java"):
            data["sonar"]["findings"][0]["file"] = file
            with self.subTest(file=file), self.assertRaises(ValueError):
                report.validate_results(data, SHA)


class PublisherTest(unittest.TestCase):
    def setUp(self):
        self.run = run()
        self.pull = pull()
        self.comments = []
        self.writes = []

    def api(self, path, method="GET", body=None):
        if method in ("POST", "PATCH"):
            self.writes.append((path, method, body))
            return {}
        if path.endswith("/actions/runs/123"):
            return self.run
        if "/jobs?" in path:
            return {"jobs": [{"id": 456, "name": "verify", "steps": [
                {"name": "Run regular tests", "conclusion": "failure", "number": 9}]}]}
        if "/commits/" in path:
            return [self.pull]
        if "/comments?" in path:
            return self.comments
        if path.endswith("/pulls/7"):
            return self.pull
        raise AssertionError("Unexpected API request: " + path)

    def publish(self, data=None):
        with patch.object(report, "github_request", side_effect=self.api), \
                patch.object(report, "download_results", return_value=data):
            report.publish_comment(REPO, 123)

    def test_creates_comment_using_github_outcome_even_when_measurements_look_good(self):
        self.publish(results())
        self.assertEqual(len(self.writes), 1)
        self.assertEqual(self.writes[0][1], "POST")
        self.assertIn("Run regular tests | ❌ Failed", self.writes[0][2]["body"])

    def test_updates_its_bot_comment_without_duplicate_posts(self):
        self.comments = [{"id": 99, "body": report.MARKER + "\n<!-- ceremo-run:122:1 -->",
                          "user": {"login": "github-actions[bot]", "type": "Bot"}}]
        self.publish()
        self.assertEqual(self.writes[0][:2], (f"/repos/{REPO}/issues/comments/99", "PATCH"))

    def test_does_not_edit_a_human_comment_with_the_same_marker(self):
        self.comments = [{"id": 99, "body": report.MARKER,
                          "user": {"login": "someone", "type": "User"}}]
        self.publish()
        self.assertEqual(self.writes[0][1], "POST")

    def test_closed_changed_and_wrong_target_prs_are_not_written(self):
        for pr in (pull(state="closed"), pull(sha="b" * 40), pull()):
            if pr["head"]["sha"] == SHA and pr["state"] == "open":
                pr["base"]["repo"]["full_name"] = "other/repository"
            with self.subTest(pr=pr):
                self.pull = pr
                self.publish()
                self.assertEqual(self.writes, [])

    def test_older_runs_do_not_replace_a_newer_report(self):
        self.comments = [{"id": 99, "body": report.MARKER + "\n<!-- ceremo-run:124:1 -->",
                          "user": {"login": "github-actions[bot]", "type": "Bot"}}]
        self.publish()
        self.assertEqual(self.writes, [])

    def test_pr_changed_or_closed_during_reads_is_not_written(self):
        for replacement in (pull(sha="b" * 40), pull(state="closed")):
            reads = 0

            def changing_api(path, method="GET", body=None):
                nonlocal reads
                if path.endswith("/pulls/7"):
                    reads += 1
                    if reads == 2:
                        return replacement
                return self.api(path, method, body)

            with self.subTest(replacement=replacement), \
                    patch.object(report, "github_request", side_effect=changing_api), \
                    patch.object(report, "download_results", return_value=None):
                report.publish_comment(REPO, 123)
            self.assertEqual(self.writes, [])

    def test_unrelated_or_incomplete_workflow_is_rejected(self):
        for field, value in (("name", "Other checks"), ("path", "other.yml"), ("status", "in_progress")):
            self.run = run(); self.run[field] = value
            with self.subTest(field=field), self.assertRaises(ValueError):
                self.publish()
        self.assertEqual(self.writes, [])

    def test_malformed_artifact_falls_back_to_recorded_step_results(self):
        with patch.object(report, "github_request", side_effect=self.api), \
                patch.object(report, "download_results", side_effect=ValueError("Invalid artifact")):
            report.publish_comment(REPO, 123)
        self.assertIn("Detailed measurements are unavailable", self.writes[0][2]["body"])
        self.assertIn("Run regular tests | ❌ Failed", self.writes[0][2]["body"])


class ArtifactTest(unittest.TestCase):
    def download(self, entries):
        archive = io.BytesIO()
        with zipfile.ZipFile(archive, "w") as files:
            for name, content in entries.items():
                files.writestr(name, content)
        redirect = urllib.error.HTTPError("https://api.github.com/artifact", 302, "Found",
                                         {"Location": "https://storage.example/artifact"}, None)
        opener = MagicMock(); opener.open.side_effect = redirect
        with patch.object(report, "pages", return_value=[{"id": 9, "name": "backend-quality-123-1", "expired": False}]), \
                patch.dict("os.environ", {"GH_TOKEN": "fixture-api-token"}), \
                patch.object(report.urllib.request, "build_opener", return_value=opener), \
                patch.object(report.urllib.request, "urlopen", return_value=io.BytesIO(archive.getvalue())) as storage:
            data = report.download_results(REPO, run())
            self.assertEqual(storage.call_args.args, ("https://storage.example/artifact",))
            return data

    def test_reads_only_json_without_extracting_other_artifact_entries(self):
        data = self.download({"ci/result.json": json.dumps(results()), "../../untrusted.py": "raise Exception('never execute')"})
        self.assertEqual(data["sha"], SHA)

    def test_oversized_and_cross_commit_json_are_rejected(self):
        with self.assertRaises(ValueError):
            self.download({"ci/result.json": " " * 65537})
        data = results(); data["sha"] = "b" * 40
        with self.assertRaises(ValueError):
            self.download({"ci/result.json": json.dumps(data)})


if __name__ == "__main__":
    unittest.main()
