#!/bin/bash

if [ -z "$4" ]
then
    echo "Repository url is empty"
else
    echo "Repository url is $4"

    wsl -d $1 -u $3 -e bash -c \
    ' \
    mkdir -p ~/repos/; \
    cd ~/repos/; \
    repo_url='"$4"'; \
    repo_name='"$(basename $4 .git)"'; \
    git clone $repo_url; \
    git config --global --add safe.directory $repo_name; \
    sudo chown -R '"$3"':'"$3"' .; \
    echo "cd ~/repos/$repo_name/" >>~/.bashrc; \
    '
fi

FILE=./modules/$2/setup_distro.sh
if [[ -f "$FILE" ]]; then
    echo "Running module specific setup"
    $FILE $1 $2 $3 $4 #>/dev/null
    echo "Completed module specific setup"
fi
