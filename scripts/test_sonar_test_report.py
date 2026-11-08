import json
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET

from sonar_test_report import execution_report, prepare, verify


class SonarTestReportTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.project = Path(self.directory.name)
        for suite, source, name in (("test", "test", "One"),
                                    ("integrationTest", "integrationTest", "Two")):
            path = self.project / f"src/{source}/java/example/{name}.java"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(f"package example; class {name} {{}}")
            path = self.project / f"build/test-results/{suite}/TEST-{name}.xml"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(f'<testsuite name="example.{name}" tests="2" failures="1" errors="0" skipped="0">'
                            f'<testcase classname="example.{name}$Nested" name="pass &amp; parameter" time="0.025"/>'
                            f'<testcase classname="example.{name}" name="fail" time="0.1">'
                            '<failure>private failure text</failure></testcase></testsuite>')

    def test_maps_both_suites_nested_classes_and_real_outcomes(self):
        prepare(self.project)
        path = self.project / "build/reports/sonar/test-executions.xml"
        root = ET.parse(path).getroot()
        self.assertEqual(root.tag, "testExecutions")
        self.assertEqual(root.attrib, {"version": "1"})
        self.assertEqual({f.get("path") for f in root}, {
            "src/test/java/example/One.java", "src/integrationTest/java/example/Two.java"})
        self.assertEqual(len(root.findall(".//testCase")), 4)
        self.assertEqual(root.find("file/testCase").get("duration"), "25")
        self.assertEqual(len(root.findall(".//failure")), 2)
        self.assertNotIn("private failure text", path.read_text())

    def test_rejects_missing_reports_sources_invalid_names_and_counter_mismatches(self):
        path = self.project / "build/test-results/test/TEST-One.xml"
        original = path.read_text()
        for value in (original.replace('tests="2"', 'tests="3"'),
                      original.replace("example.One", "../private"),
                      original.replace("example.One", "example.Missing"),
                      original.replace('time="0.025"', 'time="NaN"')):
            path.write_text(value)
            with self.subTest(value=value), self.assertRaises(ValueError):
                execution_report(self.project)
        path.unlink()
        with self.assertRaisesRegex(ValueError, "Missing JUnit"):
            execution_report(self.project)

    def test_keeps_skipped_and_error_counts(self):
        path = self.project / "build/test-results/test/TEST-One.xml"
        path.write_text('<testsuite name="example.One" tests="2" failures="0" errors="1" skipped="1">'
                        '<testcase name="skip"><skipped/></testcase>'
                        '<testcase name="error"><error/></testcase></testsuite>')
        root, totals = execution_report(self.project)
        self.assertEqual(totals, {"tests": 4, "failures": 1, "errors": 1, "skipped": 1})
        self.assertEqual(len(root.findall(".//skipped")), 1)
        self.assertEqual(len(root.findall(".//error")), 1)

    def test_repeated_parameterized_display_names_keep_both_executions(self):
        path = self.project / "build/test-results/test/TEST-One.xml"
        path.write_text('<testsuite name="example.One" tests="2" failures="0" errors="0" skipped="0">'
                        '<testcase name="same display name"/><testcase name="same display name"/></testsuite>')
        root, totals = execution_report(self.project)
        names = [case.get("name") for case in root.find("file")]
        self.assertEqual(len(set(names)), 2)
        self.assertEqual(totals["tests"], 4)

    def test_import_verification_fails_closed_for_missing_or_mismatched_measures(self):
        path = self.project / "build/reports/sonar/quality-gate.json"
        path.parent.mkdir(parents=True)
        for measures in ({}, {"tests": "3", "test_failures": "2", "test_errors": "0", "skipped_tests": "0"}):
            path.write_text(json.dumps({"status": "OK", "measures": measures}))
            with self.subTest(measures=measures), self.assertRaises(ValueError):
                verify(self.project)
            self.assertFalse(json.loads(path.read_text())["testImportVerified"])
        path.write_text(json.dumps({"status": "ERROR", "measures": {
            "tests": "4", "test_failures": "2", "test_errors": "0", "skipped_tests": "0"}}))
        verify(self.project)
        result = json.loads(path.read_text())
        self.assertTrue(result["testImportVerified"])
        self.assertEqual(result["status"], "ERROR")


if __name__ == "__main__":
    unittest.main()
