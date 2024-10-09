#!/bin/bash

./lib/commands/docker_distro.sh $1 $2 $3 $4

FILE=./modules/$2/up_distro.sh
if [[ -f "$FILE" ]]; then
    echo "Running module specific startup"
    $FILE $1 $2 $3 $4
    echo "Completed module specific startup"
fi

./lib/commands/enter_distro.sh $1 $2 $3 $4
