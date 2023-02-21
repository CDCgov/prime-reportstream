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
    #${REPO_ROOT}/.environment/gitleaks/run-gitleaks.sh
    ${REPO_ROOT}/.environment/ktlint/run-ktlintCheck.sh
    ${REPO_ROOT}/.environment/terraform-fmt/run-terraform-fmt.sh
    ${REPO_ROOT}/.environment/eslint/run-eslintCheck.sh
    ${REPO_ROOT}/.environment/yarn/run-yarnCheck.sh
)

function error() {
    echo "ERROR>"
    echo "ERROR> ${1}"
    echo "ERROR>"

    return 1
}

# Run the docker check first and bail immediately if it fails, as some other
# checks depend on it.
${REPO_ROOT}/.environment/docker/check-docker.sh
DOCKER_RC=$?
if [[ $DOCKER_RC -ne 0 ]]; then
  exit 1
fi

echo "> Running pre-commit hooks"

for item in ${CHECKS_TO_RUN[*]}; do
    echo "   - ${item?}"
    if [[ ! -x "${item?}" ]]; then
        error "The pre-commit hook script \"${item?}\" is not marked as executable."

        # contaminate the entire run
        RC=1
    else
        # Invoke the script and capture return code
        ${item?}
        NEW_RC=${?}

        # If _our_ return code indicates success (i.e. up and until now everything went well)
        # then use the last status code as the 'how are we doing so far'-code; which may result
        # in a contamination.
        # If we have encountered failures before (i.e. RC != 0), then maintain that contamination
        if [[ ${RC} == 0 ]]; then
            RC=${NEW_RC?}
        fi
    fi
done

if [ ${RC?} == 0 ]; then
    echo "OK> pre-commit hooks passed!"
else
    error "One or more pre-commit hooks failed - the changes will NOT committed!"
fi

popd 1>/dev/null 2>&1

exit ${RC?}
