#!/usr/bin/env bash

REPO_ROOT="$(git rev-parse --show-toplevel)"
RC=0
errorMessage=""

echo Checking format...
erroMessage=$(cd ${REPO_ROOT}/prime-router/ && ./gradlew ktlintCheck 1>/dev/null) 
echo Checking format finished



if [[ ! -z "$erroMessage" ]]; then
    RC=1
    echo "ktlint found format violations!"
    echo $erromessage
    return ${RC}
fi

exit ${RC}