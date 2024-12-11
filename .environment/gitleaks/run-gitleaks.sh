#!/usr/bin/env bash

function usage() {
    echo "usage: ${0} [OPTION]"
    echo ""
    echo "Runs the gitleaks container on top of your code. The default mode (i.e. without any options) scans"
    echo "the files that are staged in git."
    echo ""
    echo "Options:"
    echo "    --no-git          Scans your current working directory as is (i.e. pretends it isn't a git repo)"
    echo "    --depth <int>     Scans the <int> (topologically) last commits of the repository"
    echo "    --since <datespc> Scans the commits since a date (format: 'YYYY-mm-DD[THH:MM:SS-OFFSET]'"
    echo "    --help|-h         Shows this help and exits successfully"
    echo ""
    echo "Examples:"
    echo ""
    echo "  $ ${0}"
    echo "      Runs gitleaks over the files that are currently marked as staging"
    echo ""
    echo "  $ ${0} --no-git"
    echo "      Runs gitleaks over the current state of the repository"
    echo ""
    echo "  $ ${0} --depth 10"
    echo "      Runs gitleaks on the 10 (topologically) last commits"
    echo ""
    echo "  $ VERBOSE=1 ${0} --since '2021-06-01'"
    echo "      Runs gitleaks on the commits in your repository since June 1st, 2021 while setting the VERBOSE flag"
    echo ""
    echo ""
}

function error() {
    echo "Gitleaks> ERROR: ${*}"
}

function warning() {
    echo "Gitleaks> Warning: ${*}"
}

function note() {
    echo "Gitleaks> info: ${*}"
}

# Use a well known, stable version
GITLEAKS_IMG_NAME="zricethezav/gitleaks:v8.18.2"
REPO_ROOT=$(git rev-parse --show-toplevel)
CONTAINER_SOURCE_LOCATION="/repo"
CONTAINER_LOCATION="repo"
VERBOSE=${VERBOSE:-0}
SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

REPORT_JSON="gitleaks.report.json"
LOGFILE="gitleaks.log"

REPO_CONFIG_PATH=".environment/gitleaks/gitleaks-config.toml"
CONFIG_FILE="gitleaks-config.toml"

# Run gitleaks locally if installed
LOCAL_GITLEAKS=0
if command -v gitleaks &> /dev/null; then
    LOCAL_GITLEAKS=1
fi

function base_command() {
    if [[ ${LOCAL_GITLEAKS?} == 1 ]]; then
        echo "running gitleaks locally"
        gitleaks protect \
            --source="${REPO_ROOT?}" \
            --report-path="${REPO_ROOT?}/${REPORT_JSON?}" \
            --config="${SCRIPT_DIR?}/${CONFIG_FILE?}" \
            --report-format "json" \
            $(if [[ ${VERBOSE?} != 0 ]]; then echo "--verbose"; else echo ""; fi) \
            $1 
    else
        echo "running gitleaks in docker"
        docker run \
            -v "${REPO_ROOT?}:${CONTAINER_SOURCE_LOCATION?}" \
            --rm \
            "${GITLEAKS_IMG_NAME?}" protect \
            --source="${CONTAINER_SOURCE_LOCATION?}" \
            --report-path="${CONTAINER_SOURCE_LOCATION?}/${REPORT_JSON?}" \
            --config="${CONTAINER_SOURCE_LOCATION?}/${REPO_CONFIG_PATH?}" \
            --report-format "json" \
            $(if [[ ${VERBOSE?} != 0 ]]; then echo "--verbose"; else echo ""; fi) \
            $1
    fi
    RC=$?

    return ${RC?}
}

function scan_uncommitted() {
    note "Scanning your suggested changes."

    base_command "--staged"

    RC=$?

    return ${RC?}
}

function scan_no_git() {
    note "Scanning the current state of the repository."

    base_command "--no-git"

    RC=$?

    return ${RC?}
}

function scan_x_last_commits() {
    DEPTH=${1}
    note "Scanning the last ${DEPTH?} commits."

    base_command "--depth ${DEPTH?}"

    RC=$?

    return ${RC?}
}

function scan_since() {
    SINCE=${1}
    note "Scanning all commits since ${SINCE?}."

    base_command "--commit-since \"${SINCE?}\""

    RC=$?

    return ${RC?}
}

# Parse arguments
HAS_UNRECOGNIZED=0

# Default type of thing we do
SELECTED_RUNMODE=""
RUNMODE_STAGED_UNCOMMITTED="uncommitted"
RUNMODE_NO_GIT="no-git"
RUNMODE_DEPTH="depth"
RUNMODE_DEPTH_VALUE=""
RUNMODE_SINCE="since"
RUNMODE_SINCE_VALUE=""
while [[ ! -z "${1}" ]]; do
    case "${1}" in
    "--${RUNMODE_DEPTH?}" | "--${RUNMODE_SINCE?}")
        if [[ ! -z "${SELECTED_RUNMODE?}" ]]; then
            warning "The previously specified run-mode '${SELECTED_RUNMODE?}' will be overridden by the latest run-mode '${1:2}'."
        fi
        SELECTED_RUNMODE="${1:2}"

        if [[ -z "${2}" || "${2:0:1}" == "--" ]]; then
            error "The value for run-mode '${1:2}' is not defined."
            exit 1
        else
            case "${SELECTED_RUNMODE?}" in
            "${RUNMODE_DEPTH?}")
                RUNMODE_DEPTH_VALUE="${2}"
                ;;
            "${RUNMODE_SINCE?}")
                RUNMODE_SINCE_VALUE="${2}"
                ;;
            *)
                error "'${SELECTED_RUNMODE}' is not yet handled"
                ;;
            esac

            # We used an extra argument, shift that
            shift
        fi
        ;;
    "--${RUNMODE_NO_GIT?}")
        if [[ ! -z "${SELECTED_RUNMODE?}" ]]; then
            warning "The previously specified run-mode '${SELECTED_RUNMODE?}' will be overridden by the latest run-mode '${1:2}'."
        fi
        SELECTED_RUNMODE="${RUNMODE_NO_GIT?}"
        ;;
    "--help" | "-h")
        usage
        exit 0
        ;;
    *)
        # Keep collecting the unrecognized options
        error "Option \"${1}\" is not a recognized option."
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

RC=1 # Nothing done, fail
case "${SELECTED_RUNMODE?}" in
"${RUNMODE_DEPTH?}")
    scan_x_last_commits ${RUNMODE_DEPTH_VALUE?}
    RC=$?
    ;;
"${RUNMODE_NO_GIT}")
    scan_no_git
    RC=$?
    ;;
"${RUNMODE_SINCE?}")
    scan_since "${RUNMODE_SINCE_VALUE?}"
    RC=$?
    ;;
"${RUNMODE_STAGED_UNCOMMITTED?}")
    # Default action
    scan_uncommitted
    RC=$?
    ;;
*)
    error "The selected run-mode \"${SELECTED_RUNMODE?}\" is not a recognized one."
    exit 1
    ;;
esac

if [[ ${RC?} != 0 ]]; then
    error "(return code=${RC?}) Your code may contain secrets, consult the output above and/or one of the following files for more details:"
    error "     - ${REPO_ROOT?}/${REPORT_JSON?}"
    # no log file currently, check the output of whatever ran this
    # error "     - ${REPO_ROOT?}/${LOGFILE?}"
fi

exit ${RC?}
