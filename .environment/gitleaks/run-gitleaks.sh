#!/usr/bin/env bash

function usage() {
    echo "usage: ${0}"
    echo ""
    echo "Runs the gitleaks container on top of your code"
}

# NOTE: we are
GITLEAKS_IMG_NAME="zricethezav/gitleaks"
REPO_ROOT=$(git rev-parse --show-toplevel)
CONTAINER_SOURCE_LOCATION="/repo"
VERBOSE=${VERBOSE:-0}

REPORT_JSON="gitleaks.report.json"
LOGFILE="gitleaks.log"

function scan_uncommitted() {
    # NOTE: ironically, the switch to scan your staged (i.e. to be committed) changes is to use the --unstaged switch
    docker run \
        -v "${REPO_ROOT?}:${CONTAINER_SOURCE_LOCATION?}" \
        "${GITLEAKS_IMG_NAME?}" \
        --path="${CONTAINER_SOURCE_LOCATION?}" \
        --repo-config-path=".environment/gitleaks/gitleaks-config.toml" \
        --unstaged \
        --report="${CONTAINER_SOURCE_LOCATION?}/${REPORT_JSON?}" \
        $(if [[ ${VERBOSE?} != 0 ]]; then echo "--verbose"; else echo ""; fi) \
        2>"${LOGFILE?}"

    return $?
}

echo "Gitleaks> Scanning your suggested changes."
scan_uncommitted
RC=$?

if [[ ${RC?} != 0 ]]; then
    echo "Gitleaks> ERROR: Your code may contain secrets, consult ${REPO_ROOT?}/${REPORT_JSON?} and/or ${REPO_ROOT?}/${LOGFILE?} for more details!"
fi

exit ${RC?}
