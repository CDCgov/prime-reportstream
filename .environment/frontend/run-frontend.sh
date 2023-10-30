#!/usr/bin/env bash

if [[ -z $(git status --porcelain frontend-react/) ]] ; then
    echo "frontend-react> info: git status is reporting there are no uncommitted changes within 'frontend-react'; hence, skipping frontend checks."
else
    echo "frontend-react> running frontend checks..."
    ./frontend-react/.husky/pre-commit;
fi
RC=$?

if [[ ${RC?} != 0 ]]; then
    exit ${RC?}
fi
