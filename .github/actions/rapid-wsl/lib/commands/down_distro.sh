#!/bin/bash

FILE=./modules/$2/down_distro.sh
if [[ -f "$FILE" ]]; then
    echo "Running module specific shutdown"
    $FILE $1 $2 $3 $4 &>/dev/null
    echo "Completed module specific shutdown"
fi
