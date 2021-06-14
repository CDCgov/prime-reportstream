#!/usr/bin/env bash
# Wrapper script to aid in building

HERE="$(dirname "$(realpath "${0}")")"
DOCKER_COMPOSE="${HERE?}/docker-compose.build.yml"

BUILDER_IMAGE_NAME="prime-router_builder"

# Defaults
ACTION=${ACTION:-builder}
REFRESH_BUILDER=${REFRESH_BUILDER:-0}

while [[ -n "${1}" && "${1:0:1}" == "-" ]]; do
  if [[ "${1}" == "--" ]]; then
    shift
    break
  fi

  case "${1}" in
  "--action" | "-a")
    # Overwrites the default, last one wins
    ACTION=${2}
    shift
    ;;
  "--refresh" | "--refresh-builder" | "-r")
    REFRESH_BUILDER=1
    ;;
  *)
    echo "Unrecognized switch \"${1}\""
    exit 1
    ;;
  esac

  shift
done

pushd "${HERE?}" 2>&1 1>/dev/null

# Clean if requested, bring down the docker-compose environment
# so that we can cleanly get rid of the image
if [[ ${REFRESH_BUILDER?} != 0 ]]; then
  docker-compose --file "${DOCKER_COMPOSE?}" down
  docker image rm "${BUILDER_IMAGE_NAME?}"
fi

# Run (which will build if cleaned earlier)
docker-compose \
  --file "${DOCKER_COMPOSE?}" \
  run "${ACTION?}"

popd 2>&1 1>/dev/null
