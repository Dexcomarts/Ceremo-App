#!/usr/bin/env bash
set -euo pipefail
umask 077

: "${CEREMO_SONAR_URL:?Set CEREMO_SONAR_URL}"
: "${CEREMO_SONAR_PASSWORD:?Set CEREMO_SONAR_PASSWORD to the current SonarQube password}"
username="${CEREMO_SONAR_USERNAME:-admin}"
token_name="${CEREMO_SONAR_TOKEN_NAME:-ceremo-local}"
token_file="${CEREMO_SONAR_TOKEN_FILE:-/tokens/sonar-token}"
curl_options=(--fail --silent --show-error --connect-timeout 5 --max-time 15)

if [[ -s "$token_file" ]]; then
  printf 'A token file already exists at %s; no new token was created.\n' "$token_file"
  exit 0
fi

ready=false
for ((attempt = 0; attempt < 60; attempt++)); do
  if status=$(curl "${curl_options[@]}" "$CEREMO_SONAR_URL/api/system/status") \
      && [[ "$(jq -r '.status' <<< "$status")" == 'UP' ]]; then
    ready=true
    break
  fi
  sleep 5
done
if [[ "$ready" != true ]]; then
  printf 'SonarQube did not become ready within the retry limit.\n' >&2
  exit 1
fi

response=$(curl "${curl_options[@]}" --user "$username:$CEREMO_SONAR_PASSWORD" \
  --request POST "$CEREMO_SONAR_URL/api/user_tokens/generate" \
  --data-urlencode "name=$token_name")
token=$(jq -er '.token | select(type == "string" and length > 0)' <<< "$response")
printf '%s\n' "$token" > "$token_file"
unset token response
printf 'SonarQube token saved to %s; its value was not logged.\n' "$token_file"
