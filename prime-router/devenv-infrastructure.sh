#!/usr/bin/env bash
# Usage:
#   ./devenv-infrastructure.sh [up|down|stay]
#
# - up: brings the environment(s) up and then leaves
# - down: takes the environment(s) down
# - stay: does the same as 'up' and then drops into the builder container

STATE=${1:-"up"}
DETACH=
if [ "${STATE}" = "up" ] || [ "${STATE}" = "stay" ]; then
  STATE=up
  DETACH=--detach
fi

# Test if this Apple Silicon
PROFILE=amd64
SERVICES=() # empty list means all services for docker-compose
BUILD_SERVICES=()
if [ "$(uname -m)" = "arm64" ] && [[ $(uname -av) == *"Darwin"* ]]; then
  echo The ReportStream service does not work on Apple Silicon. Will run other services. See Apple Silicon note for details.
  PROFILE=apple_silicon
  SERVICES=(sftp azurite vault) # Only these services are compatible
  BUILD_SERVICES=(postgresql)
fi

ORPHAN_CONTAINER_WARNING_MSG="Found orphan containers ("
REMAINING_NETWORK_WARNING_MSG="error while removing network: network prime-router_prime-router_build"

# Bringing things up or down may produce some warnings, some of which we can safely ignore/prevent from being displayed
# Bring up the 'build container' (which has postgresql) up first so that the dependencies can talk to it
docker-compose -f ./docker-compose.build.yml ${STATE?} ${DETACH?} "${BUILD_SERVICES[@]}" 2>&1 |
  grep -v "${ORPHAN_CONTAINER_WARNING_MSG?}"

if [ "${PROFILE}" = "amd64" ]; then
  docker-compose -f "./docker-compose.yml" build
fi
docker-compose -f ./docker-compose.yml ${STATE?} ${DETACH} "${SERVICES[@]}" 2>&1 |
  grep -v "${ORPHAN_CONTAINER_WARNING_MSG?}" |
  grep -v "${REMAINING_NETWORK_WARNING_MSG?}"

if [ "${1}" = "stay" ]; then
  if [ "${PROFILE}" = "amd64" ]; then
    ./build.sh -- bash
  fi
fi
