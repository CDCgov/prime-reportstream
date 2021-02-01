#!/usr/bin/env bash

if [ -z "$SENT_REPORT_ID" ]
then
  echo
  echo "Expected SENT_REPORT_ID to be set."
  echo "Run example-sent.sh or example-never-sent.sh and copy a 'report_id'."
  echo
  echo "  export SENT_REPORT_ID=\"<<copied report id>>\""
  echo
  exit 1
fi

echo
echo "### Query: Ancestors for SENT_REPORT_ID=${SENT_REPORT_ID}"
echo
docker exec --user 999:999 \
  -i $(docker ps -f name=db_pgsql -q) \
  psql prime_data_hub prime \
    -v "param_id=$SENT_REPORT_ID" \
    < ./lineage-ancestors.sql
echo
