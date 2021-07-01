#!/usr/bin/env bash

# Pretty colors for echo -e
RED="\e[1;91m"
GREEN="\e[1;92m"
YELLOW="\e[1;33m"
PLAIN="\e[0m"
WHITE="\e[1;97m"

HERE="$(dirname "$(realpath "${0}")")"
VAULT_ENV_LOCAL_FILE=".vault/env/.env.local"

SHOW_HELP=0
UNBUILD=0
UNBUILD_TARGETS=(
  "./build/"
)

# This script wipes your (environmental) slate clean

function usage() {
  cat <<EOF
usage: ${0} [OPTIONS]

This script attempts to bring you back to as clean a state as possible by \
resetting and removing any environmental remnant state from previous runs \
or builds.

OPTIONS:
  --unbuild     Removes build artifacts as well (i.e. ${UNBUILD_TARGETS[*]})
  --help|-?     Display this help

EOF
}

# Takes down any docker-compose states we use in development
function decompose_docker() {
  echo "Decomposing Docker..."
  for compose_file in "./docker-compose.yml" "./docker-compose.build.yml"; do
    echo -ne "\t- ${compose_file}..."
    docker-compose --file "${compose_file}" down \
      1>/dev/null \
      2>&1
    echo "DONE"
  done
  docker container prune -f 1>/dev/null
}

# Removes any known docker volumes
function purge_docker_volumes() {
  echo -n "Pruning Docker volumes..."
  docker volume prune -f 1>/dev/null 2>&1
  echo "DONE"
}

# Resets your vault credentials
function reset_vault() {
  echo -n "Resetting your vault..."
  rm -rf .vault/env/{key,.env.local}
  mkdir -p .vault/env
  truncate -s 0 .vault/env/.env.local
  echo "DONE"

  # You explicitly do not need these since you are resetting everyting
  unset VAULT_TOKEN
  unset CREDENTIAL_STORAGE_METHOD
}

function wait_for_vault_creds() {
  # Wait for the vault to fully spin up and populate the vault file
  while [[ $(wc --bytes "${VAULT_ENV_LOCAL_FILE?}" | cut --delim " " --fields 1) == 0 ]]; do
    echo "Waiting for ${VAULT_ENV_LOCAL_FILE?} to be populated..."
    sleep 1
  done

  echo "Your vault credentials have been generated (you can use the token at http://localhost:8200/ui/):"
  export $(cat .vault/env/.env.local | xargs)
  cat "${VAULT_ENV_LOCAL_FILE?}" |
    sed 's/^/\t/g'
}

# Brings any necessary environments up again
function recompose_docker() {
  echo -n "Recomposing Docker..."
  ./devenv-infrastructure.sh \
    1>/dev/null
  echo "DONE"

  wait_for_vault_creds
  # Now that our vault credentials have populated; we need to start the prime_dev container/service
  # back up, this time using those values
  docker-compose --file docker-compose.yml down
  docker-compose --file docker-compose.yml up --detach
}

function configure_prime() {
  wait_for_vault_creds

  echo "Populating credentials into your vault..."
  # export VAULT_API_ADDR=http://localhost:8200
  for p in "IGNORE--HL7" "IGNORE--HL7-BATCH" "IGNORE--CSV"; do
    RC=1

    # Give the vault plenty of time to come up, so retry if we get failures
    while [[ ${RC} != 0 ]]; do
      echo -ne "\t- ${p}..."
      ./prime create-credential --type=UserPass --persist=${p} --user foo --pass pass 1>/dev/null 2>&1
      RC=${PIPESTATUS[0]}
      if [[ ${RC?} == 0 ]]; then
        echo "DONE"
      else
        echo "RETRY"
      fi
    done
  done

  echo -n "Loading organizations into PRIME ReportStream..."
  ./prime multiple-settings set --input settings/organizations-local.yml 1>/dev/null |
    sed 's/^/\t/g'
  echo "DONE"
}

function unbuild() {
  echo "Removing build artifacts..."
  for d in ${UNBUILD_TARGETS[*]}; do
    echo -e "\t- ${d?}"
    rm -rf "${d?}"
  done

  docker image rm \
    prime-router_builder:latest \
    prime-router_settings:latest \
    prime-router_prime_dev:latest
}

function take_ownership() {
  echo "Taking ownership of directories (may require elevation)..."
  TARGETS=(
    "./build/"
    "./docs/"
    "./.gradle/"
    "./.vault/"
  )

  for d in ${TARGETS[*]}; do
    echo -ne "\t- ${d?}..."
    sudo chown -R "${USER?}:${USER?}" "${d?}"
    sudo chmod -R a+w "${d?}"
    echo "DONE"
  done
}

# Parse command line arguments
while [[ ! -z "${1}" ]]; do
  case "${1}" in
  "--help" | "-?")
    SHOW_HELP=1
    ;;
  "--unbuild")
    UNBUILD=1
    ;;
  *) ;;

  esac

  shift
done

if [[ ${SHOW_HELP?} != 0 ]]; then
  usage
  exit 0
fi

pushd "${HERE?}" 2>&1 1>/dev/null

decompose_docker
if [[ ${UNBUILD?} != 0 ]]; then
  unbuild
fi
purge_docker_volumes
reset_vault
recompose_docker
configure_prime
take_ownership

cat <<EOF

Your environment has been reset to as clean a slate as possible. Please run \
the following command to load your credentials:

EOF

echo -e "\t\$ ${WHITE?}export \$(cat "${VAULT_ENV_LOCAL_FILE?}" | xargs)${PLAIN?}"
echo -e "\t\$ ${WHITE?}./gradlew testEnd2End${PLAIN?}\n"

popd 2>&1 1>/dev/null
