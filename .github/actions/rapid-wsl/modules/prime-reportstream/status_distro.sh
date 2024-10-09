#!/bin/bash

wsl -d $1 -u $3 -e bash -c \
' \
cd ~/repos/prime-reportstream/prime-router/; \
./gradlew ; \
'
