#!/usr/bin/env bash

# Pretty colors for echo -e
RED="\e[1;91m"
GREEN="\e[1;92m"
YELLOW="\e[1;33m"
PLAIN="\e[0m"
WHITE="\e[1;97m"

HERE="$(dirname "${0}")"
VAULT_ENV_LOCAL_FILE=".vault/env/.env.local"

SHOW_HELP=0
UNBUILD=0
RUN_E2E=0
UNBUILD_TARGETS=(
  "./build/"
)

function usage() {
  cat <<EOF
usage: ${0} [OPTIONS]

This script sets up a functional development environment that is as pristine \
as possible. It will get you there from a clean clone or get you back there \
if your environment got messed up.

OPTIONS:
  --unbuild     Removes build artifacts as well (i.e. ${UNBUILD_TARGETS[*]})
  --e2e         Runs the end-to-end tests on successfully cleaning your slate
  --help|-?     Display this help

EOF
}

# Takes down any docker-compose states we use in development
function decompose_docker() {
  echo "Decomposing Docker..."
  for compose_file in "./docker-compose.yml" "./docker-compose.build.yml"; do
    echo -ne "    - ${compose_file}..."
    docker-compose --file "${compose_file}" down \
      1>/dev/null \
      2>&1
    echo "DONE"
  done
  docker container prune -f 1>/dev/null
}

function pull_prebaked_images() {
  echo -n "Pulling pre-baked images..."
  for df in "docker-compose.yml" "docker-compose.build.yml"; do
    grep "image: " "${df}" |
      cut -d ":" -f 2- |
      xargs -n 1 docker pull 1>/dev/null
  done

  echo "DONE"
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
  cat /dev/null >.vault/env/.env.local
  echo "DONE"

  # You explicitly do not need these since you are resetting everyting
  unset VAULT_TOKEN
  unset CREDENTIAL_STORAGE_METHOD
}

function wait_for_vault_creds() {
  # Wait for the vault to fully spin up and populate the vault file
  while [[ $(wc -c "${VAULT_ENV_LOCAL_FILE?}" | cut -d " " -f 1) == 0 ]]; do
    echo "Waiting for ${VAULT_ENV_LOCAL_FILE?} to be populated..."
    sleep 2
  done

  echo "Your vault credentials have been generated (vault: http://localhost:8200/ui/):"
  export $(cat .vault/env/.env.local | xargs)
  cat "${VAULT_ENV_LOCAL_FILE?}" |
    sed 's/^/    /g'
}

function docker_compose_build() {
  DCFILE=${1:-docker-compose.yml}
  echo -e "${WHITE?}INFO:${PLAIN?} Building \"${DCFILE?}\"..."
  docker-compose --file "${DCFILE?}" build
  if [[ $? != 0 ]]; then
    echo -e "${RED?}ERROR:${PLAIN?} The docker-compose build of the \"${DCFILE?}\" failed... terminating!"
    exit 1
  fi
}

function recompose_docker() {
  echo -e "${WHITE?}INFO:${PLAIN?} Building the docker-compose environments..."
  docker_compose_build "docker-compose.build.yml"
  ensure_binaries
  docker_compose_build "docker-compose.yml"

  echo -e "${WHITE?}INFO:${PLAIN?} Bringing your docker-compose environments up..."
  docker-compose --file docker-compose.build.yml up --detach |
    sed 's/^/    /g'
  docker-compose --file docker-compose.yml up --detach |
    sed 's/^/    /g'

  wait_for_vault_creds

  # Now that we have vault credentials, make everything pick it up
  echo -e "${WHITE?}INFO:${PLAIN?} Restarting prime_dev"
  docker-compose --file docker-compose.yml restart

  while [[ $(curl -s -o "/dev/null" -w "%{http_code}" "localhost:7071") != 200 ]]; do
    echo -e "${WHITE?}INFO:${PLAIN?} Waiting for prime_dev to report HTTP_OK"
    sleep 1
  done
}

function ensure_binaries() {
  if [[ ! -f "./build/azure-functions/prime-data-hub-router/prime-router-0.1-SNAPSHOT.jar" ]]; then
    echo "You do not yet have any binaries, building them for you..."
    ./build.sh 2>&1 | sed 's/^/        /g'
    if [[ ${PIPESTATUS[0]} != 0 ]]; then
      echo -e "${RED?}ERROR:${PLAIN?} The build itself failed... exiting!"
      exit 1
    fi
  fi
}

function configure_prime() {
  wait_for_vault_creds
  ensure_binaries

  echo "Populating credentials into your vault..."
  ITEMS=(
    "IGNORE--HL7"
    "IGNORE--HL7-BATCH"
    "IGNORE--CSV"
    "DEFAULT-SFTP"
  )
  for p in ${ITEMS[*]}; do
    RC=1

    # Give the vault plenty of time to come up, so retry if we get failures
    while [[ ${RC} != 0 ]]; do
      echo -ne "    - ${p}..."
      ./prime create-credential --type=UserPass --persist=${p} --user foo --pass pass 1>/dev/null
      RC=${PIPESTATUS[0]}
      if [[ ${RC?} == 0 ]]; then
        echo "DONE"
      else
        echo "RETRY"
        sleep 1
      fi
    done
  done

  echo -n "Loading organizations into PRIME ReportStream..."
  ./prime multiple-settings set --input settings/organizations.yml 1>/dev/null |
    sed 's/^/    /g'
  echo "DONE"
}

# This functions loads in the local organizations' receivers' credentials
function configure_receiver_creds() {
  echo "Populating receiver credentials into your vault (be patient)..."
  if [[ -z "$(which python3)" ]]; then
    echo "${YELLOW?}WARNING:${PLAIN?} It appears you do not have python3; the receiver credentials from organizations.yml cannot be loaded automatically (at this point)..."
  else
    VENV_ROOT=$(mktemp -d)
    pushd "${VENV_ROOT?}" 2>&1 1>/dev/null

    python3 -m venv venv 1>/dev/null 2>&1
    source ./venv/bin/activate 1>/dev/null 2>&1
    pip install pyyaml 1>/dev/null 2>&1
    popd 2>&1 1>/dev/null

    python3 -c "import yaml;
with open(\"${HERE?}/settings/organizations.yml\") as input:
    loaded = yaml.load(input, Loader=yaml.SafeLoader)

    for org in (o for o in loaded if o.get('receivers', None) is not None):
        receivers = org['receivers']
        for rcvr in receivers:
            ORGNAME=rcvr['organizationName']
            NAME=rcvr['name']
            if ORGNAME and NAME and '_' not in NAME:
                print('%(orgName)s--%(name)s' % {'orgName': ORGNAME, 'name': NAME})
" | xargs -I_ -P 2 "${HERE}/prime" create-credential --type=UserPass --persist=_ --user foo --pass pass 1>/dev/null
    deactivate

    rm -rf "${VENV_ROOT?}"

  fi
}

function unbuild() {
  take_ownership
  echo "Removing build artifacts..."
  for d in ${UNBUILD_TARGETS[*]}; do
    echo -e "    - ${d?}"
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
    echo -ne "    - ${d?}..."
    if [[ -d "${d?}" ]]; then
      sudo chown -R "$(id -u -n):$(id -g -n)" "${d?}"
      sudo chmod -R a+w "${d?}"
      echo "DONE"
    else
      echo "NOT_THERE"
    fi
  done
}

# Parse command line arguments
while [[ ! -z "${1}" ]]; do
  case "${1}" in
  "--e2e")
    RUN_E2E=1
    ;;
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
pull_prebaked_images
if [[ ${UNBUILD?} != 0 ]]; then
  unbuild
fi
purge_docker_volumes
reset_vault
recompose_docker
configure_prime
configure_receiver_creds
take_ownership

if [[ ${RUN_E2E?} != 0 ]]; then
  echo -e "${WHITE?}INFO:${PLAIN?} Running the end-to-end tests..."
  ensure_binaries
  wait_for_vault_creds

  export $(xargs <"${VAULT_ENV_LOCAL_FILE?}")
  ./gradlew testEnd2End |
    sed "s/^/    /g"
fi

cat <<EOF

Your environment has been reset to as clean a slate as possible. Please run \
the following command to load your credentials:

EOF

echo -e "    \$ ${WHITE?}export \$(xargs < "${VAULT_ENV_LOCAL_FILE?}")${PLAIN?}"
echo -e "    \$ ${WHITE?}./gradlew testEnd2End${PLAIN?}\n"

popd 2>&1 1>/dev/null
