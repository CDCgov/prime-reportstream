#!/bin/bash

wsl -d $1 -u $3 -e bash -c \
' \
cd ~/repos/prime-reportstream/; \
sudo chown -R '"$3"':'"$3"' .git; \
act -P ubuntu-18.04=nektos/act-environments-ubuntu:18.04 -W .github/workflows/release.yml --graph; \
'
