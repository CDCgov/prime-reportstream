#!/usr/bin/env bash

KTLINT_IMAGE_NAME="reportstream-ktlint:latest"
REPO_ROOT=$(git rev-parse --show-toplevel)
CONTAINER_SOURCE_LOCATION="/src"
LOG_FILE="ktlint.log"

function usage() {
    echo "usage: ${0} [OPTION]"
    echo ""
    echo "Checks your code for ktlint violations."
    echo "By default, this script will only check files that you added or changed (staged and unstaged)."
    echo ""
    echo "Options:"
    echo "    --all|-a     Scans all files (shorthand: lower-case a)"
    echo "    --format|-F   Fix violations (shorthand: UPPER-case F)"
    echo "    --help|-h     Shows this help and exits successfully"
    echo ""
    echo "Examples:"
    echo ""
    echo "  $ ${0}"
    echo "      Runs ktlint over the files that are new or modified (ACM in staging or modified)"
    echo ""
    echo "  $ ${0} --all"
    echo "      Runs ktlint over all Kotlin (*.kt) files"
    echo ""
    echo "  $ ${0} --all --format"
    echo "      Uses ktlint to fix all violations it finds in all kotlin (*.kt) files"
    echo ""
    echo ""
}

function error() {
    echo "ktlint> ERROR: ${*}"
}

function warning() {
    echo "ktlint> Warning: ${*}"
}

function note() {
    echo "ktlint> info: ${*}"
}

function ensure_ktlint() {
    # note "Making sure you have the container image."
    pushd "$(dirname "${0}")" 1>/dev/null 2>&1
    docker build -t "${KTLINT_IMAGE_NAME?}" . 1>/dev/null
    popd 1>/dev/null 2>&1
}

note "Checking for ktlint code violations..."

DO_FORMAT=
# Default targets, since we assume default runmode
TARGETS=(
    $(git diff --cached --name-only --diff-filter=ACM | grep '\.kt\(s\)\?$')
    $(git diff --name-only --diff-filter=ACM | grep '\.kt\(s\)\?$')
)

while [[ ! -z "${1}" ]]; do
    case "${1}" in
    "--all" | "-a")
        TARGETS=(
            $(find ./ -type f -iname "*.kt")
            $(find ./ -type f -iname "*.kts")
        )
        ;;
    "--format" | "-F")
        DO_FORMAT="${1}"
        ;;
    "--help" | "-h")
        usage
        exit 0
        ;;
    *) ;;

    esac

    shift
done

RC=1

if [[ ${#TARGETS[@]} > 0 ]]; then
    ensure_ktlint
    docker run \
        -v "${REPO_ROOT?}:${CONTAINER_SOURCE_LOCATION?}" \
        --rm \
        "${KTLINT_IMAGE_NAME?}" \
        ktlint ${DO_FORMAT?} ${TARGETS[*]} | sed "s/\\${CONTAINER_SOURCE_LOCATION}/\./g" | tee "${LOG_FILE?}" | sed "s/^/    /g"
    RC=${PIPESTATUS[0]}

    if [[ ${RC?} != 0 ]]; then
        error "You likely have ktlint violations, check the output or '${LOG_FILE?}' for more information"
        note "You may be able to fix the violations by invoking '${0} --format'."
    fi
else
    note "Skipping this check, there are no target files to scan."
    RC=0
fi

exit ${RC?}
