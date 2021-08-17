#!/usr/bin/env bash

REPO_ROOT="$(git rev-parse --show-toplevel)"
RC=0

echo Checking format...
erromessage=$(cd ${REPO_ROOT}/prime-router && ./gradlew ktlintCheck 2>) 
echo Checking format finished



if [[ ! -z "$erromessage" ]]; then
    RC=1
    echo "ktlint found format violations!"
    echo $erromessage
fi

return=${RC}

exit ${RC?}