#!/usr/bin/env bash

isModified=0

function modified_check() {
    COUNT=$(git status --porcelain | grep "frontend-react/" | wc -l)
    if [[ ${MODIFIED_PACKAGE_COUNT} != 0 ]]; then
        isModified=1
    else
        isModified=0
    fi
}

modified_check
if [[ ${isModified} == 1 ]]; then
    "frontend-react/.husky/pre-commit";
fi
RC=$?

if [[ ${RC?} != 0 ]]; then
    exit ${RC?}
fi
