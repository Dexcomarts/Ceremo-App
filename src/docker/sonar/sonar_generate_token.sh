#!/usr/bin/env bash
set -euo pipefail
umask 077

: "${CEREMO_SONAR_URL:?Set CEREMO_SONAR_URL}"
username="${CEREMO_SONAR_USERNAME:-admin}"
token_name="${CEREMO_SONAR_TOKEN_NAME:-ceremo-local}"
token_file="${CEREMO_SONAR_TOKEN_FILE:-/tokens/sonar-token}"
ci_bootstrap="${CEREMO_SONAR_CI_BOOTSTRAP:-false}"
project_key=ceremo_backend
gate_name=ceremo-ci-overall
curl_options=(--fail --silent --show-error --connect-timeout 5 --max-time 15)

case "$ci_bootstrap" in
  true|false) ;;
  *) printf 'CEREMO_SONAR_CI_BOOTSTRAP must be true or false.\n' >&2; exit 1 ;;
esac
if [[ "$ci_bootstrap" == true && "$CEREMO_SONAR_URL" != http://sonar:9000 ]]; then
  printf 'CI bootstrap is restricted to the isolated Compose service.\n' >&2
  exit 1
fi

work_dir=$(mktemp -d)
trap 'rm -rf -- "$work_dir"' EXIT
admin_password_file="$(dirname "$token_file")/sonar-ci-admin-password"

# Credentials are read from a private curl config, never passed as process arguments.
authenticate() {
  local encoded
  encoded=$(printf '%s:%s' "$username" "$1" | base64 | tr -d '\n')
  printf 'header = "Authorization: Basic %s"\n' "$encoded" > "$work_dir/auth"
}
api() {
  curl "${curl_options[@]}" --config "$work_dir/auth" "$@"
}

report() {
  [[ "$ci_bootstrap" == true && -s "$admin_password_file" ]] || {
    printf 'The CI bootstrap credential is missing.\n' >&2; exit 1;
  }
  authenticate "$(< "$admin_password_file")"
  api --get "$CEREMO_SONAR_URL/api/ce/component" \
    --data-urlencode "component=$project_key" > "$work_dir/task.json"
  if ! jq -e '.queue == [] and .current.status == "SUCCESS"' "$work_dir/task.json" > /dev/null; then
    printf '{"status":"UNAVAILABLE","policyEvaluated":false,"reason":"No completed successful analysis"}\n'
    return 1
  fi
  api --get "$CEREMO_SONAR_URL/api/qualitygates/project_status" \
    --data-urlencode "projectKey=$project_key" > "$work_dir/gate.json"
  api --get "$CEREMO_SONAR_URL/api/measures/component" \
    --data-urlencode "component=$project_key" \
    --data-urlencode 'metricKeys=security_hotspots,bugs,vulnerabilities,code_smells,sqale_rating,tests,test_failures,test_errors,skipped_tests' > "$work_dir/measures.json"
  # Retain every open finding, but never raw messages or source snippets.
  printf '[]' > "$work_dir/findings.json"
  local page=1 total
  while true; do
    api --get "$CEREMO_SONAR_URL/api/issues/search" \
      --data-urlencode "componentKeys=$project_key" --data-urlencode 'types=BUG,VULNERABILITY,CODE_SMELL' \
      --data-urlencode 'resolved=false' --data-urlencode 'ps=500' --data-urlencode "p=$page" > "$work_dir/issues.json"
    total=$(jq -er '.paging.total | numbers' "$work_dir/issues.json")
    if ((total > 1000)); then
      printf 'Issue count exceeds the bounded report limit; complete reporting was not verified.\n' >&2
      return 1
    fi
    jq --slurpfile next "$work_dir/issues.json" --arg prefix "$project_key:" \
      '. + [$next[0].issues[] | {rule, type, file: (.component | ltrimstr($prefix)), line}]' \
      "$work_dir/findings.json" > "$work_dir/next.json"
    mv "$work_dir/next.json" "$work_dir/findings.json"
    ((page * 500 >= total)) && break
    page=$((page + 1))
  done
  # No hotspot measure means no hotspots. Other missing gate conditions fail closed.
  jq -n --slurpfile gate "$work_dir/gate.json" --slurpfile measures "$work_dir/measures.json" \
    --slurpfile findings "$work_dir/findings.json" '
    ($gate[0].projectStatus) as $s |
    ([ $measures[0].component.measures[] | select(.metric == "security_hotspots") |
      .value | tonumber ][0] // 0) as $hotspots |
    [{metric:"bugs",op:"GT",threshold:"0"}, {metric:"vulnerabilities",op:"GT",threshold:"0"},
     {metric:"coverage",op:"LT",threshold:"100"}, {metric:"duplicated_lines_density",op:"GT",threshold:"3"},
     {metric:"sqale_rating",op:"GT",threshold:"1"}] as $required |
    ($measures[0].component.measures | map({key: .metric, value: .value}) | from_entries) as $m |
    {
      status: $s.status,
      scope: "overall code; temporary server, no retained review history",
      hotspots: $hotspots,
      findings: $findings[0],
      measures: ({test_failures:"0", test_errors:"0", skipped_tests:"0", security_hotspots:"0"} + $m),
      conditions: [$s.conditions[] | {metric: .metricKey, status, value: .actualValue, threshold: .errorThreshold}],
      policyEvaluated: (
        ($s.ignoredConditions == false) and
        (all(["bugs","vulnerabilities","code_smells","sqale_rating","tests"][]; . as $metric | $m[$metric] != null)) and
        (all($required[]; . as $rule | any($s.conditions[]; .metricKey == $rule.metric and
          .actualValue != null and .comparator == $rule.op and .errorThreshold == $rule.threshold))) and
        ($hotspots == 0 or any($s.conditions[]; .metricKey == "security_hotspots_reviewed" and
          .actualValue != null and .comparator == "LT" and .errorThreshold == "100"))
      )
    }' > "$work_dir/summary.json"
  cat "$work_dir/summary.json"
  jq -e '.policyEvaluated and .status == "OK"' "$work_dir/summary.json" > /dev/null
}

case "${1:-generate}" in
  report) report; exit ;;
  generate) ;;
  *) printf 'Expected generate or report.\n' >&2; exit 1 ;;
esac

if [[ -s "$token_file" ]]; then
  if [[ "$ci_bootstrap" == true ]]; then
    printf 'CI bootstrap requires an empty token volume.\n' >&2
    exit 1
  fi
  printf 'A token file already exists at %s; no new token was created.\n' "$token_file"
  exit 0
fi
if [[ "$ci_bootstrap" == true && -e "$admin_password_file" ]]; then
  printf 'CI bootstrap was already started; use a new isolated volume.\n' >&2
  exit 1
fi
if [[ "$ci_bootstrap" != true ]]; then
  : "${CEREMO_SONAR_PASSWORD:?Set CEREMO_SONAR_PASSWORD to the current SonarQube password}"
fi

ready=false
for ((attempt = 0; attempt < 60; attempt++)); do
  if status=$(curl "${curl_options[@]}" "$CEREMO_SONAR_URL/api/system/status") \
      && [[ "$(jq -r '.status' <<< "$status")" == UP ]]; then
    ready=true
    break
  fi
  sleep 5
done
if [[ "$ready" != true ]]; then
  printf 'SonarQube did not become ready within the retry limit.\n' >&2
  exit 1
fi

if [[ "$ci_bootstrap" == true ]]; then
  # This mode is explicitly enabled only for a fresh, disposable CI instance.
  username='admin'
  authenticate admin
  printf 'Ci_9!%s' "$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')" > "$admin_password_file"
  api --request POST "$CEREMO_SONAR_URL/api/users/change_password" \
    --data-urlencode 'login=admin' --data-urlencode 'previousPassword=admin' \
    --data-urlencode "password@$admin_password_file" > /dev/null
  authenticate "$(< "$admin_password_file")"
  api --request POST "$CEREMO_SONAR_URL/api/projects/create" \
    --data-urlencode "project=$project_key" --data-urlencode 'name=CEREMO Backend' \
    --data-urlencode 'visibility=private' > /dev/null
  api --request POST "$CEREMO_SONAR_URL/api/qualitygates/create" \
    --data-urlencode "name=$gate_name" > /dev/null
  # Sonar also adds its standard new-code conditions to custom gates.
  new_coverage_id=$(api --get "$CEREMO_SONAR_URL/api/qualitygates/show" \
    --data-urlencode "name=$gate_name" | \
    jq -er '.conditions[] | select(.metric == "new_coverage") | .id')
  api --request POST "$CEREMO_SONAR_URL/api/qualitygates/update_condition" \
    --data-urlencode "id=$new_coverage_id" --data-urlencode 'metric=new_coverage' \
    --data-urlencode 'op=LT' --data-urlencode 'error=100' > /dev/null
  # Overall-code conditions remain meaningful on the very first analysis.
  while read -r metric operator threshold; do
    api --request POST "$CEREMO_SONAR_URL/api/qualitygates/create_condition" \
      --data-urlencode "gateName=$gate_name" --data-urlencode "metric=$metric" \
      --data-urlencode "op=$operator" --data-urlencode "error=$threshold" > /dev/null
  done <<'CONDITIONS'
bugs GT 0
vulnerabilities GT 0
coverage LT 100
duplicated_lines_density GT 3
security_hotspots_reviewed LT 100
sqale_rating GT 1
CONDITIONS
  api --request POST "$CEREMO_SONAR_URL/api/qualitygates/select" \
    --data-urlencode "gateName=$gate_name" --data-urlencode "projectKey=$project_key" > /dev/null
  response=$(api --request POST "$CEREMO_SONAR_URL/api/user_tokens/generate" \
    --data-urlencode "name=$token_name" --data-urlencode 'type=PROJECT_ANALYSIS_TOKEN' \
    --data-urlencode "projectKey=$project_key" \
    --data-urlencode "expirationDate=$(date -u -d '+2 days' +%F)")
else
  authenticate "$CEREMO_SONAR_PASSWORD"
  response=$(api --request POST "$CEREMO_SONAR_URL/api/user_tokens/generate" \
    --data-urlencode "name=$token_name")
fi
token=$(jq -er '.token | select(type == "string" and length > 0)' <<< "$response")
printf '%s\n' "$token" > "$work_dir/token"
mv "$work_dir/token" "$token_file"
unset token response CEREMO_SONAR_PASSWORD
printf 'SonarQube token saved to %s; its value was not logged.\n' "$token_file"
