#!/usr/bin/env sh
# Usage:
#   ./devenv-infrastructure.sh [up|down]

STATE=${1:-up}
DETACH=
if [ "${STATE?}" = "up" ]; then
  DETACH=--detach
fi

ORPHAN_CONTAINER_WARNING_MSG="Found orphan containers ("
REMAINING_NETWORK_WARNING_MSG="error while removing network: network prime-router_prime-router_build"

# Bringing things up or down may produce some warnings, some of which we can safely ignore/prevent from being displayed
docker-compose -f ./docker-compose.yml ${STATE?} ${DETACH?} 2>&1 \
  | grep -v "${ORPHAN_CONTAINER_WARNING_MSG?}" \
  | grep -v "${REMAINING_NETWORK_WARNING_MSG?}"

docker-compose -f ./docker-compose.build.yml ${STATE?} ${DETACH?} 2>&1 \
  | grep -v "${ORPHAN_CONTAINER_WARNING_MSG?}"
