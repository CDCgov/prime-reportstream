#!/usr/bin/env bash

REDBOLD='\033[1;31m'
TITLE='\033[1;4m'
NC='\033[0m' # No Color

if [ -z "$SENT_REPORT_ID" ]
then
  echo
  echo -e "${REDBOLD}Expected SENT_REPORT_ID to be set.${NC}"
  echo "Run example-sent.sh or example-never-sent.sh and copy a 'report_id'."
  echo
  echo "  export SENT_REPORT_ID=<<copied report id>>"
  echo "  # export SENT_REPORT_ID=afdbad7e-1b08-41d0-8e49-86d12b39fd62 # explicit example"
  echo
  exit 1
fi

echo
echo -e "${TITLE}### Query: Ancestors for SENT_REPORT_ID=${SENT_REPORT_ID}${NC}"
echo
docker exec --user 999:999 \
  -i $(docker ps -f name=db_pgsql -q) \
  psql prime_data_hub prime \
    -v "param_id=$SENT_REPORT_ID" \
    < ./lineage-ancestors.sql
echo
