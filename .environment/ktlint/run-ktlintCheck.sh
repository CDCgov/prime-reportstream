#!/usr/bin/env bash

echo Checking format...
(cd ../../prime-router && ./gradlew ktlintCheck) 1>/dev/null
echo Checking format finished