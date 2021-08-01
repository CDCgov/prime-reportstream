#!/usr/bin/env bash
# Wrapper script to aid in building

# Pretty colors for echo -e
RED="\e[1;91m"
GREEN="\e[1;92m"
YELLOW="\e[1;33m"
PLAIN="\e[0m"
WHITE="\e[1;97m"

HERE="$(dirname "$(realpath "${0}")")"
DOCKER_COMPOSE="${HERE?}/docker-compose.build.yml"

BUILDER_IMAGE_NAME="prime-router_builder"

function usage() {
  echo "usage: ${0} [--action|-a \"<action>\"] [--clean] [--refresh[-build]|-r] [-- <args>]"
  echo ""
  echo -e "\t--action|-a              Which container to run (default: builder)"
  echo -e "\t--clean                  Cleans up as much as possible from any previous run"
  echo -e "\t--refresh[-builder]|-r   Refreshes your builder container (i.e. removes any previous ones and rebuilds your builder-container image)"
  echo -e "\t<args>                   What you want the container to run (default: \"gradle package\")"
}

function get_gradle_command() {
  if [[ -n "${1}" ]]; then
    case "${1}" in
    "gradle")
      echo $*
      ;;
    *)
      # Lets you bash into the build container
      echo $*
      ;;
    esac
  else
    echo gradle package
  fi
}

function ensure_build_dir() {
  mkdir -p "${HERE?}/build"
  echo "Making sure you own 'build/', 'docs/' and '.gradle/' and everyone has at least +rw... (may require elevation)"
  for d in "build/" "docs/" ".gradle/"; do
    if [[ -d "${d}" ]]; then
      echo "    - $(pwd)/${d}"

      # Take ownership
      sudo chown -R "$(id -u -n):$(id -g -n)" "${d}" | sed "s/^/    /g"
      sudo chmod -R a+rw "${d}" | sed "s/^/    /g"
      # if sudo reports failure (due to whatever reason), then stop trying
      if [[ $? != 0 ]]; then
        break
      fi

      # Make sure everyone can write to anything in the dir
      sudo chmod -R a+w "${d}"
    fi
  done
}

# Defaults
ACTION=${ACTION:-builder}
REFRESH_BUILDER=${REFRESH_BUILDER:-0}
PERFORM_CLEAN=${PERFORM_CLEAN:-0}

while [[ -n "${1}" && "${1:0:1}" == "-" ]]; do

  # Tripwire, anything you pass after a blanket '--' (or -c) will be passed into docker-compose run
  if [[ "${1}" == "--" || "${1}" == "-c" ]]; then
    shift
    break
  fi

  case "${1}" in
  "--action" | "-a")
    # Overwrites the default, last one wins
    ACTION=${2}
    shift
    ;;
  "--clean")
    PERFORM_CLEAN=1
    ;;
  "--refresh" | "--refresh-builder" | "-r")
    REFRESH_BUILDER=1
    ;;
  "--help" | "-h")
    usage
    exit 0
    ;;
  *)
    usage
    echo ""
    echo -e "${RED?}ERROR: ${PLAIN?}Unrecognized switch \"${1}\""
    exit 1
    ;;
  esac

  shift
done

pushd "${HERE?}" 2>&1 1>/dev/null

ensure_build_dir

if [[ ${PERFORM_CLEAN?} != 0 || ${REFRESH_BUILDER?} != 0 ]]; then
  echo -e "${WHITE?}INFO:${PLAIN?} Bringing down builder docker composition"
  docker-compose --force --file "${DOCKER_COMPOSE?}" down

  if [[ ${REFRESH_BUILDER?} != 0 ]]; then
    echo -e "${WHITE?}INFO:${PLAIN?} Refreshing the builder container image"
    docker image rm --force "${BUILDER_IMAGE_NAME?}"
    docker-compose --file "${DOCKER_COMPOSE?}" build
  fi

  if [[ ${PERFORM_CLEAN?} != 0 ]]; then
    echo -e "${WHITE?}INFO:${PLAIN?} Cleaning previous build artifacts (and removing postgres DB)"
    rm -rf "${HERE?}/build"
    if [[ $? != 0 ]]; then
      # Try again with elevated permissions if this failed
      echo -e "${YELLOW?}WARNING: ${PLAIN?} The build directory (${HERE?}/build) could not be removed; try again with elevated permissions."
      sudo rm -rf "${HERE?}/build"
    fi
    ensure_build_dir
    docker volume rm prime-router_vol_postgresql_data
    docker-compose \
      --file "${DOCKER_COMPOSE?}" \
      run builder gradle clean
  fi
fi

# Run (which will build if cleaned earlier)
echo -e "${WHITE?}INFO:${PLAIN?} Running ${ACTION?} $(get_gradle_command $*) through docker-compose"
docker-compose \
  --file "${DOCKER_COMPOSE?}" \
  run "${ACTION?}" $(get_gradle_command $*)
RC=$?

# When you're done, let's make sure you have ownership again
ensure_build_dir

popd 2>&1 1>/dev/null

exit ${RC}