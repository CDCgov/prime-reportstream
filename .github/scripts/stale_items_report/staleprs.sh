#!/bin/bash
result="$(curl -i -H "Accept: application/json" -H "Content-Type: application/json" -s 'https://api.github.com/repos/CDCgov/prime-reportstream/pulls?state=open')"
echo "result: '$result'"
items=$(echo "$result" | jq -c -r '.[]')
for item in ${items[@]}; do
    echo $item
    # whatever you are trying to do ...
done
$SHELL