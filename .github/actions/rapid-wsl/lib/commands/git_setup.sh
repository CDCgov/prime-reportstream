#!/bin/bash

echo ""
echo "***"
echo "Setting up GitHub personal access token"

TKN_FILES=(./etc/git.token)
if [[ -e "${TKN_FILES[0]}" ]]; then
    echo "Importing ./etc/git.token file:"
    echo "***"
    echo ""
    wsl -d $1 -u $3 -e bash -c \
    ' \
    path=$(wslpath -u '"$PWD"')/etc/; \
    path=$(echo $path | sed "s3/c/c/3/c/3g"); \
    gh auth login --with-token < $path/git.token; \
    '
    echo ""
    echo "***"
    echo "Import complete"
    echo "***"
    echo ""
else
    echo "No etc/git.token file found"
    echo "GitHub personal access token > etc/git.token"
    echo "***"
    echo ""
fi
