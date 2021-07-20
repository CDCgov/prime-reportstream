#!/usr/bin/env bash
RC=0

# Hop into the root of your repo
REPO_ROOT="$(pwd)"
REPORTED_ROOT="$(git rev-parse --show-toplevel)"
if [[ $? != 0 || "${REPORTED_ROOT?}" != "${REPO_ROOT}" ]]; then
    echo "ERROR: Something is wrong, the pre-commit hook is not running at the root of your repository."
    exit 1
fi

RC=0
CHECKS=(
    ${REPO_ROOT}/.environment/gitleaks/run-gitleaks.sh
)

echo "> Running pre-commit hooks"
pushd "${REPO_ROOT?}" 1>/dev/null 2>&1
for item in ${CHECKS[*]}; do
    echo "   - ${item?}"
    ${item?}
    NEW_RC=${?}

    if [ ${RC} == 0 ]; then
        RC=${NEW_RC?}
    fi
done

if [ ${RC?} == 0 ]; then
    echo "OK> pre-commit hooks passed!"
else
    echo "ERROR>"
    echo "ERROR> One or more pre-commit hooks failed - NOT committed!"
    echo "ERROR>"
fi

exit ${RC?}
