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
  */api/measures/component*) printf '{"component":{"measures":[{"metric":"security_hotspots","value":"1"}]}}' ;;
  */api/qualitygates/project_status*)
    status=OK
    [[ "$FAKE_SCENARIO" != failed ]] || status=ERROR
    if [[ "$FAKE_SCENARIO" == missing ]]; then
      printf '{"projectStatus":{"status":"OK","ignoredConditions":false,"conditions":[]}}'
    else
      jq -n --arg status "$status" '{projectStatus:{status:$status,ignoredConditions:false,conditions:
        ["bugs","vulnerabilities","coverage","duplicated_lines_density","security_hotspots_reviewed"] |
        map({metricKey:.,actualValue:"100",status:$status,errorThreshold:"100"})}}'
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
  if "$helper" "${@}" > "$test_dir/output" 2>&1; then
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
[[ "$(grep -c '/api/qualitygates/create_condition' "$FAKE_CALLS")" == 5 ]]
! grep -q 'fixture-analysis-token' "$test_dir/output"
if "$helper" > /dev/null 2>&1; then exit 1; fi
printf 'PASS: CI rotates credentials, scopes its token, configures the gate and rejects reuse\n'

for scenario in ok failed missing queued; do
  export FAKE_SCENARIO="$scenario"
  if "$helper" report > "$test_dir/report.json" 2> "$test_dir/report.err"; then
    [[ "$scenario" == ok ]]
    jq -e '.policyEvaluated and .status == "OK"' "$test_dir/report.json" > /dev/null
  else
    [[ "$scenario" != ok ]]
  fi
done
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
