#!/bin/bash

FILE=./modules/$2/fix_distro.sh
if [[ -f "$FILE" ]]; then
    echo "Running module specific fixes"
    $FILE $1 $2 $3 $4
    echo "Completed module specific fixes"
fi
