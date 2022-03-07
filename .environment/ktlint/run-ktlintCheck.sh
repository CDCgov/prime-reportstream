#!/usr/bin/env bash

echo "Checking for ktlint code violations in PRIME Router..."
cd prime-router
# If there is an error then those errors will be printed out
./gradlew ktlintCheck >/dev/null
RC=$?

if [[ ${RC?} != 0 ]]; then
    echo "ktlint> ERROR: You likely have ktlint violations, check the KTLint logs in build/reports/ktlint for more information"
    echo "You may be able to fix the violations by invoking './gradlew ktlintFormat'."
fi
echo "Done."
exit ${RC?} 
