#!/usr/bin/env bash

ORIGIN_DIR="$(pwd)"
DIR="$(dirname "$0")"
FRONTEND_DIR=""
HOOK_FILE=".husky/_/pre-commit"
isModified=0
set -o pipefail

function modified_check() {
    MODIFIED_COUNT=$(git diff --staged --name-only | grep "frontend-react/" | wc -l | xargs)
    if [[ ${MODIFIED_COUNT} != 0 ]]; then
        isModified=1
    else
        isModified=0
    fi
}

cd "$DIR/../../frontend-react";
FRONTEND_DIR=$(pwd);

if [ ! -f "$HOOK_FILE" ]; then
    echo "$FRONTEND_DIR/$HOOK_FILE does not exist. Please make sure you run yarn first."
    exit 1
fi

modified_check
if [[ ${isModified} == 1 ]]; then
    "./$HOOK_FILE"
fi
RC=$?
cd $ORIGIN_DIR

if [[ ${RC?} != 0 ]]; then
    exit ${RC?}
fi
