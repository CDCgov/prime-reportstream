#!/usr/bin/env bash

isModified=0

function note() {
    echo "yarn-check> info: ${*}"
}

function modified_check() {
    MODIFIED_PACKAGE_COUNT=$(git status --porcelain | grep "package.json$" | wc -l)
    MODIFIED_LOCK_COUNT=$(git status --porcelain | grep "yarn.lock$" | wc -l)
    if [ ${MODIFIED_PACKAGE_COUNT} != 0 ] || [ ${MODIFIED_LOCK_COUNT} != 0 ]; then
        isModified=1
    else
        isModified=0
    fi
}

function yarn_lock_check() {
    note "Checking Yarn lock integrity"
    yarn check --integrity
}

cd frontend-react
modified_check
if [[ ${isModified} == 1 ]]; then
    yarn_lock_check
else
    note "Skipping this check"
fi
RC=$?

if [[ ${RC?} != 0 ]]; then
    note "ERROR: Your yarn lock file is out of sync. Please run yarn install and try again."
fi

exit ${RC?} 
