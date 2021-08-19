#!/usr/bin/env bash

function error() {
    echo "terraform-fmt> ERROR: ${*}"
}

function warning() {
    echo "terraform-fmt> Warning: ${*}"
}

function note() {
    echo "terraform-fmt> info: ${*}"
}

REPO_ROOT=$(git rev-parse --show-toplevel)
UNCOMMITTED_CHANGES=0
TERRAFORM_NEEDS_FMT=0

function git_stash() {
    if [[ $(git diff-index --quiet HEAD --) ]]; then
        warning "Stashing current changes so we verify formatting with committed files"
        git stash
    fi
    RC=$?

    return ${RC?}
}

function git_unstash() {
    if [[ ${UNCOMMITTED_CHANGES?} != 0 ]]; then
        warning "Restoring stashed changes"
        git stash pop
    fi
    RC=$?

    return ${RC?}
}

function terraform_check_fmt() {
    note "Checking Terraform formatting."
    make -f ${REPO_ROOT?}/operations/Makefile tf-cmd TF_CMD="terraform fmt -check -recursive /app/src"
    RC=$?
    TERRAFORM_NEEDS_FMT=${RC?}

    return ${RC?}
}

function terraform_fmt() {
    if [[ ${TERRAFORM_NEEDS_FMT?} != 0 ]]; then
        warning "Formatting all Terraform files."
        make -f ${REPO_ROOT?}/operations/Makefile tf-cmd TF_CMD="terraform fmt -recursive /app/src"
        RC=$?

        return ${RC?}
    fi

    return 0
}


#git_stash

terraform_check_fmt
RC=$?

#terraform_fmt
#git_unstash

if [[ ${RC?} != 0 ]]; then
    error "(return code=${RC?}) Your Terraform files are not formatted. \`terraform fmt\` has been run and the changes can be committed to comply with formatting requirements."
fi

exit ${RC?}
