#!/usr/bin/env bash

REPO_ROOT="$(git rev-parse --show-toplevel)"

echo Checking format...
(cd ${REPO_ROOT}/prime-router && ./gradlew ktlintCheck) 1>/dev/null
echo Checking format finished