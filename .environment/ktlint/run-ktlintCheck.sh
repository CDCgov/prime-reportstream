#!/usr/bin/env bash

REPO_ROOT="$(git rev-parse --show-toplevel)"
RC=0


echo Checking format...
$(cd ${REPO_ROOT}/prime-router/ && ./gradlew ktlintCheck >/dev/null 2>&1)
RC=$?
echo Checking format finished.


if [[ RC -ne 0 ]]; then
    echo "ktlint found format violations!"
    echo $erromessage 
fi

exit ${RC}