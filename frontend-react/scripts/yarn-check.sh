#!/usr/bin/env bash

isModified=0

function modified_check() {
    MODIFIED_PACKAGE_COUNT=$(git status --porcelain | grep "package.json$" | wc -l | xargs)
    MODIFIED_LOCK_COUNT=$(git status --porcelain | grep "yarn.lock$" | wc -l | xargs)
    if [ ${MODIFIED_PACKAGE_COUNT} != 0 ] || [ ${MODIFIED_LOCK_COUNT} != 0 ]; then
        isModified=1
    else
        isModified=0
    fi
}

function yarn_lock_check() {
    echo "Checking Yarn lock integrity"
    yarn --immutable --immutable-cache
}

modified_check
if [[ ${isModified} == 1 ]]; then
    yarn_lock_check
fi
RC=$?

if [[ ${RC?} != 0 ]]; then
    echo "ERROR: Your yarn lock file is out of sync. Please yarn install and stage the yarn.lock file (if changed) and try again."
    exit ${RC?}
fi
