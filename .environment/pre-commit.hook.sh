#!/usr/bin/env bash
RC=0

# Hop into the root of your repo
pushd "$(dirname "${0}")/.." 1>/dev/null 2>&1

RC=0
CHECKS=(
    .environment/gitleaks/run-gitleaks.sh
)

echo "> Running pre-commit hooks"
for item in ${CHECKS[*]}; do
    echo "   - ${item?}"
    ${item?}
    NEW_RC=${?}

    if [ ${RC} == 0 ]; then
        RC=${NEW_RC?}
    fi
done

if [ $RC = 0 ]; then

    echo "OK> pre-commit hooks passed!"
else
    echo "ERROR>"
    echo "ERROR> One or more pre-commit hooks failed - NOT committed!"
    echo "ERROR>"
fi

popd 1>/dev/null 2>&1

exit ${RC?}
