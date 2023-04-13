#!/bin/bash
result="$(curl -i -H "Accept: application/json" -H "Content-Type: application/json" -s 'https://api.github.com/repos/CDCgov/prime-reportstream/pulls?state=open')"
echo "result: '$result'"
RESP=$(echo "$result" | grep -oP "^[^a-zA-Z0-9]")
echo "RESP:'$RESP'"

echo "$result" | json access_token