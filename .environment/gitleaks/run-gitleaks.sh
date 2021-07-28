#!/usr/bin/env bash

function usage() {
    echo "usage: ${0} [OPTION]"
    echo ""
    echo "Runs the gitleaks container on top of your code. The default mode scans the files that are staged in git."
    echo ""
    echo "Options:"
    echo "    --no-git      Scans your current working directory as is (i.e. pretends it isn't a git repo)"
    echo "    --help|-h     Shows this help and exits successfully"
    echo ""
}

# Use a well known, stable version
GITLEAKS_IMG_NAME="zricethezav/gitleaks:v7.5.0"
REPO_ROOT=$(git rev-parse --show-toplevel)
CONTAINER_SOURCE_LOCATION="/repo"
VERBOSE=${VERBOSE:-0}

REPORT_JSON="gitleaks.report.json"
LOGFILE="gitleaks.log"

REPO_CONFIG_PATH=".environment/gitleaks/gitleaks-config.toml"

function scan_uncommitted() {
    # NOTE: ironically, the switch to scan your staged (i.e. to be committed) changes is to use the --unstaged switch
    docker run \
        -v "${REPO_ROOT?}:${CONTAINER_SOURCE_LOCATION?}" \
        "${GITLEAKS_IMG_NAME?}" \
        --path="${CONTAINER_SOURCE_LOCATION?}" \
        --repo-config-path="${REPO_CONFIG_PATH?}" \
        --report="${CONTAINER_SOURCE_LOCATION?}/${REPORT_JSON?}" \
        $(if [[ ${VERBOSE?} != 0 ]]; then echo "--verbose"; else echo ""; fi) \
        --unstaged \
        2>"${LOGFILE?}"
    RC=$?

    return ${RC?}
}

function scan_no_git() {
    docker run \
        -v "${REPO_ROOT?}:${CONTAINER_SOURCE_LOCATION?}" \
        "${GITLEAKS_IMG_NAME?}" \
        --path="${CONTAINER_SOURCE_LOCATION?}" \
        --config-path="${CONTAINER_SOURCE_LOCATION?}/${REPO_CONFIG_PATH?}" \
        --report="${CONTAINER_SOURCE_LOCATION?}/${REPORT_JSON?}" \
        $(if [[ ${VERBOSE?} != 0 ]]; then echo "--verbose"; else echo ""; fi) \
        --no-git \
        2>"${LOGFILE?}"

    RC=$?

    return ${RC?}
}

# Parse arguments
HAS_UNRECOGNIZED=0

# Default type of thing we do
SELECTED_RUNMODE=""
RUNMODE_STAGED_UNCOMMITTED="uncommitted"
RUNMODE_NO_GIT="no-git"
while [[ ! -z "${1}" ]]; do
    case "${1}" in
    "--no-git")
        if [[ ! -z "${SELECTED_RUNMODE?}" ]]; then
            echo "Warning: Previous run-mode '${SELECTED_RUNMODE?}' will be overridden by latest run-mode '${RUNMODE_NO_GIT?}'"
        fi
        SELECTED_RUNMODE="${RUNMODE_NO_GIT?}"
        ;;
    "--help" | "-h")
        usage
        exit 0
        ;;
    *)
        # Keep collecting the unrecognized options
        echo "ERROR> Option \"${1}\" is not a recognized option."
        HAS_UNRECOGNIZED=1
        ;;
    esac

    shift
done

# Exit in error if you provided any invalid option
if [[ ${HAS_UNRECOGNIZED?} != 0 ]]; then
    echo ""
    usage
    exit 1
fi

# Set default run-mode if none was select
if [[ -z "${SELECTED_RUNMODE?}" ]]; then
    SELECTED_RUNMODE="${RUNMODE_STAGED_UNCOMMITTED?}"
fi

echo "Gitleaks> Scanning your suggested changes."
RC=1 # Nothing done, fail
case "${SELECTED_RUNMODE?}" in
"${RUNMODE_STAGED_UNCOMMITTED?}")
    # Default action
    scan_uncommitted
    RC=$?
    ;;
"${RUNMODE_NO_GIT}")
    scan_no_git
    RC=$?
    ;;
*)
    echo "The selected run-mode \"${SELECTED_RUNMODE?}\" is not a recognized one."
    exit 1
    ;;
esac

if [[ ${RC?} != 0 ]]; then
    echo "Gitleaks> ERROR (${RC?}): Your code may contain secrets, consult the output above and/or one of the following files for more details:"
    echo "Gitleaks>     - ${REPO_ROOT?}/${REPORT_JSON?}"
    echo "Gitleaks>     - ${REPO_ROOT?}/${LOGFILE?}"
fi

exit ${RC?}
