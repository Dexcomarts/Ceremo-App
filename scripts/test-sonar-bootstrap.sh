#!/usr/bin/env bash
# Run inside the token image. No network or real credentials are used.
set -euo pipefail
umask 077
test_dir=$(mktemp -d)
trap 'rm -rf -- "$test_dir"' EXIT
mkdir "$test_dir/bin"
export PATH="$test_dir/bin:$PATH"
export CEREMO_SONAR_URL=http://sonar:9000
export CEREMO_SONAR_PASSWORD=fixture-password
export CEREMO_SONAR_CI_BOOTSTRAP=false
export FAKE_SCENARIO=ok
helper=/usr/local/bin/sonar_generate_token

cat > "$test_dir/bin/curl" <<'CURL'
#!/usr/bin/env bash
set -euo pipefail
# A real secret passed as an argument is a regression, even if logs look clean.
[[ "$*" != *fixture-password* ]] || exit 99
printf '%s\n' "$*" >> "$FAKE_CALLS"
case "$*" in
  */api/system/status*)
    if [[ "$FAKE_SCENARIO" == down ]]; then printf '{"status":"STARTING"}'; else printf '{"status":"UP"}'; fi ;;
  */api/users/change_password*) [[ "$FAKE_SCENARIO" != rejected ]] ;;
  */api/qualitygates/show*) printf '{"conditions":[{"id":"new-coverage","metric":"new_coverage"}]}' ;;
  */api/user_tokens/generate*)
    [[ "$FAKE_SCENARIO" != rejected ]] || exit 22
    if [[ "$FAKE_SCENARIO" == malformed ]]; then printf '{}'; else printf '{"token":"fixture-analysis-token"}'; fi ;;
  */api/ce/component*)
    if [[ "$FAKE_SCENARIO" == queued ]]; then printf '{"queue":[{}]}'; else printf '{"queue":[],"current":{"status":"SUCCESS"}}'; fi ;;
  */api/measures/component*)
    if [[ "$FAKE_SCENARIO" == missing-measures ]]; then printf '{"component":{"measures":[]}}';
    else printf '{"component":{"measures":[{"metric":"security_hotspots","value":"1"},{"metric":"bugs","value":"0"},{"metric":"vulnerabilities","value":"0"},{"metric":"code_smells","value":"2"},{"metric":"sqale_rating","value":"1.0"},{"metric":"tests","value":"10"}]}}'; fi ;;
  */api/issues/search*)
    [[ "$FAKE_SCENARIO" != unavailable-issues ]] || exit 22
    if [[ "$FAKE_SCENARIO" == too-many-issues ]]; then printf '{"paging":{"total":1001},"issues":[]}';
    elif [[ "$FAKE_SCENARIO" == paginated ]]; then
      if [[ "$*" == *'p=2'* ]]; then first=501; last=501; else first=1; last=500; fi
      jq -n --argjson first "$first" --argjson last "$last" '{paging:{total:501},issues:
        [range($first;$last+1) | {rule:"java:S1135",type:"CODE_SMELL",component:"ceremo_backend:src/main/java/Example.java",line:.}]}';
    else printf '{"paging":{"total":1},"issues":[{"rule":"java:S1135","type":"CODE_SMELL","component":"ceremo_backend:src/main/java/Example.java","line":10}]}'; fi ;;
  */api/qualitygates/project_status*)
    status=OK
    [[ "$FAKE_SCENARIO" != failed ]] || status=ERROR
    if [[ "$FAKE_SCENARIO" == missing ]]; then
      printf '{"projectStatus":{"status":"OK","ignoredConditions":false,"conditions":[]}}'
    else
      jq -n --arg status "$status" --arg scenario "$FAKE_SCENARIO" '{projectStatus:{status:$status,ignoredConditions:false,conditions:
        ["bugs","vulnerabilities","coverage","duplicated_lines_density","security_hotspots_reviewed","sqale_rating"] |
        map(. as $metric | {metricKey:$metric,status:$status,
          comparator:(if IN("bugs","vulnerabilities","sqale_rating","duplicated_lines_density") then "GT" else "LT" end),
          actualValue:(if IN("bugs","vulnerabilities","duplicated_lines_density") then "0" elif . == "sqale_rating" then "1.0" else "100" end),
          errorThreshold:(if IN("bugs","vulnerabilities") then "0" elif . == "duplicated_lines_density" then "3" elif . == "sqale_rating" then "1" else "100" end)})}} |
        if $scenario == "maintainability" then .projectStatus.status="ERROR" |
          .projectStatus.conditions |= map(if .metricKey == "sqale_rating" then
            .actualValue="2.0" | .status="ERROR" else . end)
        elif $scenario == "weakened-policy" then .projectStatus.conditions |=
          map(if .metricKey == "sqale_rating" then .errorThreshold="5" else . end)
        else . end'
    fi ;;
  *) printf '{}' ;;
esac
CURL
printf '#!/usr/bin/env bash\nexit 0\n' > "$test_dir/bin/sleep"
chmod +x "$test_dir/bin/"*

new_case() {
  mkdir "$test_dir/$1"
  export CEREMO_SONAR_TOKEN_FILE="$test_dir/$1/sonar-token"
  export FAKE_CALLS="$test_dir/$1/calls"
  : > "$FAKE_CALLS"
  export FAKE_SCENARIO=ok
}
expect_failure() {
  if "$helper" > "$test_dir/output" 2>&1; then
    printf 'Expected rejection, got success.\n' >&2; exit 1
  fi
  [[ ! -s "$CEREMO_SONAR_TOKEN_FILE" ]]
}

new_case missing-password
unset CEREMO_SONAR_PASSWORD
expect_failure
[[ ! -s "$FAKE_CALLS" ]]
printf 'PASS: local mode requires credentials before network access\n'

new_case existing-local
printf 'existing-token' > "$CEREMO_SONAR_TOKEN_FILE"
"$helper" > /dev/null
[[ "$(< "$CEREMO_SONAR_TOKEN_FILE")" == existing-token && ! -s "$FAKE_CALLS" ]]
printf 'PASS: existing local tokens are preserved without network access\n'

new_case wrong-server
export CEREMO_SONAR_CI_BOOTSTRAP=true CEREMO_SONAR_URL=http://example.invalid
expect_failure
[[ ! -s "$FAKE_CALLS" ]]
export CEREMO_SONAR_URL=http://sonar:9000
printf 'PASS: CI bootstrap rejects another server URL\n'

new_case fresh-ci
"$helper" > "$test_dir/output"
[[ "$(< "$CEREMO_SONAR_TOKEN_FILE")" == fixture-analysis-token ]]
[[ "$(stat -c '%a' "$CEREMO_SONAR_TOKEN_FILE")" == 600 ]]
[[ "$(stat -c '%a' "$(dirname "$CEREMO_SONAR_TOKEN_FILE")/sonar-ci-admin-password")" == 600 ]]
grep -q 'type=PROJECT_ANALYSIS_TOKEN' "$FAKE_CALLS"
grep -q 'projectKey=ceremo_backend' "$FAKE_CALLS"
[[ "$(grep -c '/api/qualitygates/create_condition' "$FAKE_CALLS")" == 6 ]]
grep -q 'metric=sqale_rating.*op=GT.*error=1' "$FAKE_CALLS"
if grep -q 'fixture-analysis-token' "$test_dir/output"; then
  printf 'Analysis token appeared in helper output.\n' >&2
  exit 1
fi
if "$helper" > /dev/null 2>&1; then exit 1; fi
printf 'PASS: CI rotates credentials, scopes its token, configures the gate and rejects reuse\n'

for scenario in ok failed maintainability weakened-policy missing queued missing-measures unavailable-issues too-many-issues; do
  export FAKE_SCENARIO="$scenario"
  if "$helper" report > "$test_dir/report.json" 2> "$test_dir/report.err"; then
    [[ "$scenario" == ok ]]
    jq -e '.policyEvaluated and .status == "OK"' "$test_dir/report.json" > /dev/null
    jq -e '.measures.code_smells == "2" and .measures.sqale_rating == "1.0" and
      .findings[0].type == "CODE_SMELL"' "$test_dir/report.json" > /dev/null
  else
    [[ "$scenario" != ok ]]
  fi
done
export FAKE_SCENARIO=paginated
"$helper" report > "$test_dir/report.json"
jq -e '.findings | length == 501' "$test_dir/report.json" > /dev/null
printf 'PASS: reports require completed analysis and evaluated, passing conditions\n'

for scenario in rejected malformed down; do
  new_case "$scenario"
  export FAKE_SCENARIO="$scenario"
  expect_failure
  if [[ "$scenario" == down ]]; then
    [[ "$(grep -c '/api/system/status' "$FAKE_CALLS")" == 60 ]]
  fi
done
printf 'PASS: rejected authentication, malformed tokens and bounded readiness fail without a token\n'
