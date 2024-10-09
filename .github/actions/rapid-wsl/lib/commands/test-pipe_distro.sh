#!/bin/bash

FILE=./modules/$2/test-pipe_distro.sh
if [[ -f "$FILE" ]]; then
    echo "Running module specific pipeline test"
    $FILE $1 $2 $3 $4
    echo "Completed module specific pipeline test"
fi
