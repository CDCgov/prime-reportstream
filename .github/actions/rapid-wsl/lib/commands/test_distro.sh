#!/bin/bash

FILE=./modules/$2/test_distro.sh
if [[ -f "$FILE" ]]; then
    echo "Running module specific test"
    $FILE $1 $2 $3 $4
    echo "Completed module specific test"
fi
