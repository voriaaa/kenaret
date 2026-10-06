#!/usr/bin/env bash
# Runs a Gradle command and, on failure, surfaces the key error lines as a GitHub annotation
# so the cause is visible on the run summary page without opening the full log.
set -uo pipefail
./gradlew --no-daemon --console=plain "$@" 2>&1 | tee gradle-output.log
status=${PIPESTATUS[0]}
if [ "$status" -ne 0 ]; then
  summary=$(grep -E "^e: |^w: .*error|error:|What went wrong|Could not|Caused by|FAILED|> " gradle-output.log \
    | grep -v "^> Task .*UP-TO-DATE" | grep -v "^> Task .*NO-SOURCE" | grep -v "^> Task .*FROM-CACHE" \
    | head -80)
  # One annotation, newlines encoded, so we stay under the per-step annotation limit.
  encoded=$(printf '%s' "$summary" | sed 's/%/%25/g' | awk 'BEGIN{ORS="%0A"} {print}')
  echo "::error title=Gradle $*::${encoded}"
  {
    echo "### Gradle failure: $*"
    echo '```'
    printf '%s\n' "$summary"
    echo '```'
  } >> "$GITHUB_STEP_SUMMARY"
fi
exit "$status"
