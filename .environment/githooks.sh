#!/usr/bin/env bash

pushd "$(dirname "${0}")" 1>/dev/null 2>&1

REPO_ROOT="$(pwd)/.."

# mapping of "source file" to "destination location"
declare -A GITHOOKS
GITHOOKS["pre-commit.hook.sh"]="${REPO_ROOT}/.git/hooks/pre-commit"

CAPTURE=

function usage(){
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
    echo "> Setting up your git hooks"

    for key in ${!GITHOOKS[@]}; do
        echo "    ${GITHOOKS[${key?}]}"
        ${CAPTURE?} cp "${key}" "${GITHOOKS[${key?}]}"
    done

    echo "> Pulling down the gitleaks docker image (needed for one or more hooks)..."
    ${CAPTURE?} docker pull "zricethezav/gitleaks" 1>/dev/null
}

# ./githooks.sh remove
function remove_hooks() {
    echo "> Removing your git hooks"

    for key in ${!GITHOOKS[@]}; do
        echo "    ${GITHOOKS[${key?}]}"
        ${CAPTURE?} rm -f "${GITHOOKS[${key?}]}"
    done
}

# ./githooks.sh
function _hooks(){
    usage
}

# if you say '--whatif' then instead of really 'doing' things, we just echo what we're about to do
if [[ "${2}" == "--whatif" ]]; then
    CAPTURE="echo"
fi
${1}_hooks

pushd "$(dirname "${0}")" 1>/dev/null 2>&1
