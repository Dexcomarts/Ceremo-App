"""Map real Gradle test results to Sonar's supported execution-report format."""

import argparse
from decimal import Decimal, InvalidOperation
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

from ci_report import read_json, xml_root

SUITES = ("test", "integrationTest")
COUNTERS = ("tests", "failures", "errors", "skipped")


def execution_report(project):
    project = Path(project)
    output = ET.Element("testExecutions", version="1")
    files = {}
    seen = set()
    totals = dict.fromkeys(COUNTERS, 0)
    for suite in SUITES:
        reports = sorted((project / "build/test-results" / suite).glob("TEST-*.xml"))
        if not reports:
            raise ValueError(f"Missing JUnit reports for {suite}")
        for path in reports:
            root = xml_root(path)
            if root.tag != "testsuite":
                raise ValueError("Expected a JUnit testsuite")
            actual = dict.fromkeys(COUNTERS, 0)
            for case_index, case in enumerate(root.findall("testcase"), 1):
                class_name = case.get("classname", root.get("name", "")).split("$", 1)[0]
                if not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*", class_name):
                    raise ValueError("Invalid Java test class name")
                relative = class_name.replace(".", "/") + ".java"
                matches = [project / source / relative for source in
                           ("src/test/java", "src/integrationTest/java")
                           if (project / source / relative).is_file()]
                if len(matches) != 1:
                    raise ValueError(f"Test must map to one Java source: {class_name}")
                source = matches[0].relative_to(project).as_posix()
                if source not in files:
                    files[source] = ET.SubElement(output, "file", path=source)
                try:
                    seconds = Decimal(case.get("time", "0"))
                    if not seconds.is_finite() or not 0 <= seconds <= 86_400:
                        raise ValueError("Invalid test duration")
                    duration = str(int(seconds * 1000))
                except InvalidOperation as error:
                    raise ValueError("Invalid test duration") from error
                name = case.get("name")
                if not name:
                    raise ValueError("Missing test case name")
                # JUnit display names can repeat across parameterized methods.
                name = case.get("classname", root.get("name", "")) + f"#{case_index}:" + name
                if (source, name) in seen:
                    raise ValueError("Duplicate test case identity")
                seen.add((source, name))
                test = ET.SubElement(files[source], "testCase", name=name, duration=duration)
                actual["tests"] += 1
                statuses = [status for status in ("failure", "error", "skipped")
                            if case.find(status) is not None]
                if len(statuses) > 1:
                    raise ValueError("Conflicting test outcomes")
                if statuses:
                    status = statuses[0]
                    actual[{"failure": "failures", "error": "errors", "skipped": "skipped"}[status]] += 1
                    ET.SubElement(test, status, message="See the Gradle test report")
            expected = {key: int(root.attrib[key]) for key in COUNTERS}
            if actual != expected:
                raise ValueError(f"JUnit counters disagree with test cases: {path.name}")
            for key in COUNTERS:
                totals[key] += actual[key]
    if not totals["tests"]:
        raise ValueError("No executed or skipped test cases found")
    return output, totals


def prepare(project):
    report, totals = execution_report(project)
    directory = Path(project) / "build/reports/sonar"
    directory.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(report).write(directory / "test-executions.xml", encoding="utf-8", xml_declaration=True)
    print(f"Prepared {totals['tests']} test cases in {len(report)} Java test files.")


def verify(project):
    _, expected = execution_report(project)
    path = Path(project) / "build/reports/sonar/quality-gate.json"
    gate = read_json(path, limit=1_000_000)
    measures = gate.get("measures", {})
    wanted = {"tests": expected["tests"] - expected["skipped"],
              "test_failures": expected["failures"], "test_errors": expected["errors"],
              "skipped_tests": expected["skipped"]}
    gate["testImportVerified"] = all(
        str(measures.get(key)) == str(value) for key, value in wanted.items())
    path.write_text(json.dumps(gate, indent=2) + "\n", encoding="utf-8")
    if not gate["testImportVerified"]:
        raise ValueError("Sonar test totals do not match the real JUnit results")
    print(f"Verified Sonar import against all {expected['tests']} JUnit test cases.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("prepare", "verify"))
    parser.add_argument("--project", type=Path, default=Path.cwd())
    args = parser.parse_args()
    (prepare if args.command == "prepare" else verify)(args.project)
