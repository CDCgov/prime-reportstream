#!/usr/bin/env bash

REPORT_ID=$(jq -r ".id" ./Test-post.a.json)

echo
echo "### Query: Full lineage for REPORT_ID=${REPORT_ID}"
echo
docker exec --user 999:999 \
  -i $(docker ps -f name=db_pgsql -q) \
  psql prime_data_hub prime \
    -v "param_id=$REPORT_ID" \
    < ./lineage-full.sql
echo
