#!/usr/bin/env bash

RC=0

echo "Checking for ktlint code violations in PRIME Router..."

if [[ -z $(git status --porcelain prime-router/) ]] ; then
    echo "git status is reporting there are no uncommitted changes within 'prime-router'; hence, skipping ktlint checks."
else
  # If there is an error then those errors will be printed out
  ./gradlew :prime-router:ktlintCheck >/dev/null
  RC=$?

  if [[ ${RC?} != 0 ]]; then
      echo "ktlint> ERROR: You likely have ktlint violations, check the KTLint logs in build/reports/ktlint for more information"
      echo "You may be able to fix the violations by invoking './gradlew ktlintFormat'."
  fi
  echo "Done."
fi

exit ${RC?}
