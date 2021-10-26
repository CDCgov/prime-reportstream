#!/usr/bin/env bash

LOG_FILE="ktlint.log"

function usage() {
    echo "usage: ${0} [OPTION]"
    echo ""
    echo "Checks prime_router for ktlint violations."
    echo ""
    echo "Options:"
    echo "    --format|-F   Fix violations (shorthand: UPPER-case F)"
    echo "    --help|-h     Shows this help and exits successfully"
    echo ""
    echo "Examples:"
    echo ""
    echo "  $ ${0}"
    echo "      Runs ktlint over the files in prime_report."
    echo ""
    echo "  $ ${0} --format"
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

note "Checking for ktlint code violations..."

KTLINT_OPTIONS="ktlintCheck"
PRIME_ROUTER_DIR="../../prime-router"
DIR_NAME=$(dirname "$0")

while [[ ! -z "${1}" ]]; do
    case "${1}" in
    "--format" | "-F")
        KTLINT_OPTIONS="ktlintFormat"
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
   
pushd ${DIR_NAME}
cd ${PRIME_ROUTER_DIR}
./gradlew ${KTLINT_OPTIONS} 
RC=${PIPESTATUS[0]}
LINT_LOGS=$(find build/reports/ktlint -type f -name "*.txt")
cat ${LINT_LOGS} > $LOG_FILE

if [[ ${RC?} != 0 ]]; then
    error "You likely have ktlint violations, check the output or '${LOG_FILE?}' for more information"
    note "You may be able to fix the violations by invoking '${0} --format'."
fi
popd

exit ${RC?} 
