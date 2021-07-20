#!/usr/bin/env bash
RC=0

# Figure out the root of your repo; so that you can run this manually as well
REPO_ROOT="$(git rev-parse --show-toplevel)"
if [[ $? != 0 ]]; then
    echo "ERROR> You must run ${0} from within a git repository work tree!"
    exit 1
fi

pushd "${REPO_ROOT?}" 1>/dev/null 2>&1

# Add your executable script here if you want them part of the pre-commit hook execution
CHECKS_TO_RUN=(
    ${REPO_ROOT}/.environment/gitleaks/run-gitleaks.sh
)

echo "> Running pre-commit hooks"
pushd "${REPO_ROOT?}" 1>/dev/null 2>&1
for item in ${CHECKS_TO_RUN[*]}; do
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
    echo "ERROR> One or more pre-commit hooks failed - the changes will NOT committed!"
    echo "ERROR>"
fi

popd 1>/dev/null 2>&1

exit ${RC?}
