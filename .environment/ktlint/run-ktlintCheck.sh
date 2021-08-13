#!/usr/bin/env bash

echo ------------------  Checking format...
(cd ../../prime-router && ./gradlew ktlintCheck) 2>/dev/null
echo ------------------  Checking finished