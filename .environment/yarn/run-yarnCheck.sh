#!/usr/bin/env bash

function note() {
    echo "yarn-check> info: ${*}"
}

function yarn_lock_check() {
    note "Checking Yarn lock integrity"
    yarn check --integrity
}

cd frontend-react
yarn_lock_check
RC=$?

if [[ ${RC?} != 0 ]]; then
    echo "yarn-check> ERROR: Your yarn lock file is out of sync. Please run yarn install and try again."
fi

exit ${RC?} 
