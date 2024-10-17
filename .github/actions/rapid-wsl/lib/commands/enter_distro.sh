#!/bin/bash


if [ ! -z "$4" ]
then
    wsl -d $1 -u $3 -e bash -c \
    ' \
    mkdir -p ~/repos/; \
    cd ~/repos/; \
    repo_url='"$4"'; \
    repo_name='"$(basename $4 .git)"'; \
    cd $repo_name; \
    code .; \
    '
fi

wsl -d $1 -u $3 -e bash
