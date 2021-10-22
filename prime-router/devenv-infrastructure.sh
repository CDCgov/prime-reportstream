#!/usr/bin/env sh
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

ORPHAN_CONTAINER_WARNING_MSG="Found orphan containers ("
REMAINING_NETWORK_WARNING_MSG="error while removing network: network prime-router_prime-router_build"

# Bringing things up or down may produce some warnings, some of which we can safely ignore/prevent from being displayed
# Bring up the 'build container' (which has postgresql) up first so that the dependencies can talk to it
docker-compose -f ./docker-compose.build.yml ${STATE?} ${DETACH?} 2>&1 |
  grep -v "${ORPHAN_CONTAINER_WARNING_MSG?}"

docker-compose -f "./docker-compose.yml" build
docker-compose -f ./docker-compose.yml ${STATE?} ${DETACH} 2>&1 |
  grep -v "${ORPHAN_CONTAINER_WARNING_MSG?}" |
  grep -v "${REMAINING_NETWORK_WARNING_MSG?}"

if [ "${1}" = "stay" ]; then
  ./build.sh -- bash
fi
