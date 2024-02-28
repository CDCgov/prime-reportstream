#!/usr/bin/env bash

# Make sure we are in this script's location; this gives us certainty of what is where
pushd "$(dirname "${0}")" 1>/dev/null 2>&1

REPO_ROOT="$(pwd)/.."
dir=${REPO_ROOT}/.git/hooks

if [[ ! -e $dir ]]; then
    mkdir -p $dir
elif [[ ! -d $dir ]]; then
    echo "$dir already exists" 1>&2
fi

# mapping of "source file" to "destination location"; since we cannot reliably use associative
# arrays (declare -A) we're just keeping 2 regular arrays that we match up by index
declare -a GITHOOKS_SRC
declare -a GITHOOKS_DST

GITHOOKS_SRC[0]="pre-commit.hook.sh"
GITHOOKS_DST[0]="${REPO_ROOT}/.git/hooks/pre-commit"

# Count 'em
_GHSRC_HOOK_COUNT=${#GITHOOKS_SRC[@]}
_GHDST_HOOK_COUNT=${#GITHOOKS_DST[@]}
if [[ ${_GHSRC_HOOK_COUNT?} != ${_GHDST_HOOK_COUNT?} ]]; then
    echo "ERROR: GitHook source (${_GHSRC_HOOK_COUNT?}) and destination (${_GHDST_HOOK_COUNT?}) entries do not have the same amount of items"
    exit 1
fi

CAPTURE=

function usage() {
    echo "usage: ${0} (install|remove) [--whatif]"
    echo ""
    echo "Installs or removes the git hooks we use."
    echo ""
    echo "    install       Sets your system up to use the latest hooks"
    echo "    remove        Removes/disables the hooks"
    echo "    --whatif      Pretend-mode, will show you what it will do"
    echo ""
}

# ./githooks.sh install
function install_hooks() {
    echo "> Installing up your git hooks"

    let _MAX_IX=${_GHSRC_HOOK_COUNT?}-1
    for i in $(seq 0 ${_MAX_IX?}); do
        echo "    ${GITHOOKS_SRC[${i}]} -> ${GITHOOKS_DST[${i}]}"
        ${CAPTURE?} cp "$(pwd)/${GITHOOKS_SRC[${i}]}" "${GITHOOKS_DST[${i}]}" |
            sed "s/^/        /g"
    done
    echo "> Git hooks installed successfully"
}

# ./githooks.sh remove
function remove_hooks() {
    echo "> Removing your git hooks"

    let _MAX_IX=${_GHDST_HOOK_COUNT?}-1
    for i in $(seq 0 ${_MAX_IX?}); do
        echo "    ${GITHOOKS_DST[${i}]}"
        ${CAPTURE?} rm -f "${GITHOOKS_DST[${i}]}" |
            sed "s/^/        /g"
    done
    echo "> Git hooks removed successfully"
}

# ./githooks.sh
function _hooks() {
    usage
}

# if you say '--whatif' then instead of really 'doing' things, we just echo what we're about to do
if [[ "${2}" == "--whatif" ]]; then
    CAPTURE="echo"
fi
${1}_hooks

pushd "$(dirname "${0}")" 1>/dev/null 2>&1
