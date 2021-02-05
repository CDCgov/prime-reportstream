#!/usr/bin/env bash

TITLE='\033[1;4m'
NC='\033[0m' # No Color

REPORT_ID=$(jq -r ".id" ./Test-post.a.json)

echo
echo -e "${TITLE}### Query: Full lineage for REPORT_ID=${REPORT_ID}${NC}"
echo
docker exec --user 999:999 \
  -i $(docker ps -f name=db_pgsql -q) \
  psql prime_data_hub prime \
    -v "param_id=$REPORT_ID" \
    < ./lineage-full.sql
echo
