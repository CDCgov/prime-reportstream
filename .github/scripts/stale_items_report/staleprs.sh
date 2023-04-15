#!/bin/bash
result="$(curl -i -H "Accept: application/json" -H "Content-Type: application/json" -s 'https://api.github.com/repos/CDCgov/prime-reportstream/pulls?state=open')"
# echo "result: '$result'"

# echo ${result[lastupdated]}

# for i in "${result[@]}"
# do
# 	echo "${i[lastupdated]}"
# done
# echo $result | jq '. | map([.updated_at, .number])' | jq @sh

REPORT_ID=$(jq -r ".number" $result)
echo $REPORT_ID
# items=$(echo "$result" | jq -c -r '.[]')
# for item in ${items[@]}; do
#     echo ${item[lastupdated]}
#     # whatever you are trying to do ...
# done
$SHELL