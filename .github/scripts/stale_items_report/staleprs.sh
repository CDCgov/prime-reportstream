#!/bin/bash
result="$(curl -s 'https://api.github.com/repos/CDCgov/prime-reportstream/pulls?state=open')"
echo "result: '$result'"
RESP=$(echo "$result" | grep -oP "^[^a-zA-Z0-9]")
echo "RESP:'$RESP'"