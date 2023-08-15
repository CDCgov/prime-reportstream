#!/usr/bin/env bash

isModified=0

function modified_check() {
    MODIFIED_COUNT=$(git status --porcelain | grep "frontend-react/" | wc -l)
    if [[ ${MODIFIED_COUNT} != 0 ]]; then
        isModified=1
    else
        isModified=0
    fi
}

modified_check
if [[ ${isModified} == 1 ]]; then
    ./frontend-react/.husky/pre-commit;
fi
RC=$?

if [[ ${RC?} != 0 ]]; then
    exit ${RC?}
fi
