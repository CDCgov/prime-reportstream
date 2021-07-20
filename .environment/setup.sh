#!/usr/bin/env bash
pushd "$(dirname "${0}")" 1>/dev/null 2>&1

REPO_ROOT="$(pwd)/.."
GITLEAKS_IMG_NAME="zricethezav/gitleaks"

echo "> Setting up your commit hooks..."
cp "./pre-commit.hook.sh" "${REPO_ROOT?}/.git/hooks/pre-commit"

echo "> Pulling down the gitleaks docker image..."
docker pull "${GITLEAKS_IMG_NAME?}"

pushd "$(dirname "${0}")" 1>/dev/null 2>&1
