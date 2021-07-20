#!/usr/bin/env bash

# This script is what gets installed into $REPO_ROOT/.git/hooks/pre-commit
# and all it does is delegate to the "real" script which is located in
# $REPO_ROOT/.environment/pre-commit.runner.sh
# This approach enables us to change what we do during a pre-commit check
# without having to have everyone re-install their hooks on these changes
# We can just change the called file, and their installed hook (which
# calls that file) can remain as is

RC=1

# Make sure we have some certainty of where we are
REPO_ROOT="$(pwd)"
REPORTED_ROOT="$(git rev-parse --show-toplevel)"
if [[ $? != 0 || "${REPORTED_ROOT?}" != "${REPO_ROOT}" ]]; then
    echo "ERROR: Something is wrong, your hooks are not running at the root of your repository."
    exit 1
fi

# Execute the actual hook report its success as our success
${REPO_ROOT?}/.environment/pre-commit.runner.sh
RC=$?

exit ${RC?}