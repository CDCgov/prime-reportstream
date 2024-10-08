#!/bin/bash

echo "$1" >auth.txt
gh auth login --with-token <auth.txt
if [[ $? -ne 0 ]]; then
  echo "GitHub auth failed"
  exit 1
fi

random=$(< /dev/urandom tr -dc A-Za-z0-9_ | head -c1)

repo=$(gh search repos $random \
--sort updated \
--order desc \
--visibility public \
--limit 1 \
--include-forks false \
--stars=">=10" \
--size=">=500" \
--json fullName,updatedAt,url,pushedAt \
--jq '.[].fullName')

echo "repo=$repo" >> $GITHUB_OUTPUT
echo $repo
