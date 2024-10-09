#!/bin/bash

waiting() {
    pid=$!

    spin='-\|/'

    START_TIME=$SECONDS
    i=0
    while kill -0 $pid 2>/dev/null; do
        i=$(((i + 1) % 4))
        printf "\r${spin:$i:1} "
        sleep .1
    done
    ELAPSED_TIME=$(($SECONDS - $START_TIME))
    echo -e "\n$(($ELAPSED_TIME/60)) min $(($ELAPSED_TIME%60)) sec"  
}
