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
TERRAFORM_NEEDS_FMT=0
LOGFILE="terraform-fmt.log"

function terraform_check_fmt() {
    note "Checking Terraform formatting."
    make -C "${REPO_ROOT?}/operations" -f "${REPO_ROOT?}/operations/Makefile" tf-cmd TF_CMD="terraform fmt -check -recursive /app/src" > "${REPO_ROOT?}/${LOGFILE?}" 2>&1
    TERRAFORM_NEEDS_FMT=$?
    return ${TERRAFORM_NEEDS_FMT?}
}

function terraform_fmt() {
    if [[ ${TERRAFORM_NEEDS_FMT?} != 0 ]]; then
        warning "Formatting all Terraform files."
        make -C "${REPO_ROOT?}/operations" -f "${REPO_ROOT?}/operations/Makefile" tf-cmd TF_CMD="terraform fmt -recursive /app/src" >> "${REPO_ROOT?}/${LOGFILE?}" 2>&1
        return $?
    fi
    return 0
}

terraform_check_fmt
RC=$?

terraform_fmt

if [[ ${RC?} != 0 ]]; then
    error "(return code=${RC?}) Your Terraform files are not formatted."
    error "\`terraform fmt\` has been run and the changes can be committed to comply with formatting requirements."
    error "Additional information can be found in the following files"
    error "     - ${REPO_ROOT?}/${LOGFILE?}"
fi

exit ${RC?}
