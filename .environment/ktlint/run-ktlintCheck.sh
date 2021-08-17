#!/usr/bin/env bash

REPO_ROOT="$(git rev-parse --show-toplevel)"
RC=0
errorMessage=""

echo Checking format...
errorMessage=$(cd ${REPO_ROOT}/prime-router/ && ./gradlew ktlintCheck 2> 1>/dev/null) 
echo Checking format finished



if [[ ! -z "$errorMessage" ]]; then
    RC=1
    echo "ktlint found format violations!"
    echo $erromessage
    
fi

exit ${RC}