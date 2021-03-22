#!/usr/bin/env bash

# Run a set of interesting lineage queries.  Pass in one parameter: an id (uuid), as returned in the json, to hub api caller.
# Runs queries that lists the ancestors of the given report.

# Make sure you load to stored procs first:
#      psql prime_data_hub -f lineage-report-stored-procedures.sql

database=prime_data_hub
REPORT_UUID=$1

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

if [ -z $REPORT_UUID ] ; then
usage="
Usage: $(basename $0) <REPORT_UUID>
This runs queries to
   1. list all ancestors of the given report
"
  echo "$usage"
  exit;
fi

printf "\n\n${RED}Where did I come from?  Find all the ancestors of $REPORT_UUID${NC}\n"
psql prime_data_hub <<EOF
select RF.report_id, A.action_name as creating_action,
       RF.body_format, RF.sending_org, RF.receiving_org, left(RF.transport_result, 20) as send_result, RF.item_count
from report_file as RF
join action as A ON A.action_id = RF.action_id
where RF.report_id in (select report_ancestors('$REPORT_UUID'))
order by A.action_id desc;
EOF

printf "\n\n${RED}Where did I come from?  Find my Ur ancestors${NC}\n"
psql prime_data_hub <<EOF
select RF.report_id, A.action_name as creating_action,
       RF.body_format, RF.sending_org, RF.receiving_org, left(RF.transport_result, 20) as send_result, RF.item_count
from report_file as RF
join action as A ON A.action_id = RF.action_id
where RF.report_id in (select report_ur_ancestors('$REPORT_UUID'))
order by A.action_id desc;
EOF

printf "\n\n${RED}Find my cousins - reports from the same ancestry, that went to other locations${NC}\n"
psql prime_data_hub <<EOF
select RF.report_id, A.action_name as creating_action,
       RF.body_format, RF.sending_org, RF.receiving_org, left(RF.transport_result, 20) as send_result, RF.item_count
from report_file as RF
join action as A ON A.action_id = RF.action_id
where RF.report_id in (select find_sent_reports(report_ur_ancestors('$REPORT_UUID')));
EOF


exit 0

