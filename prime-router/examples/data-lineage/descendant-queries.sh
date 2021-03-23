#!/usr/bin/env bash

# Run a set of interesting lineage queries.  Pass in one parameter: an id (uuid), as returned in the json, to hub api caller.

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
   1. list all descendants of the given report
   2. list just the descendants that were sent to an external receiver
   3. list just the descendants that were processed, but never sent
"
  echo "$usage"
  exit;
fi

printf "\n\n${RED}How are baby reports made?   Find all the descendants of $REPORT_UUID${NC}\n"
psql prime_data_hub <<EOF
select RF.report_id, A.action_id as ACT_ID, A.action_name as action_taken,
       RF.body_format as FMT, RF.receiving_org AS receiver, receiving_org_svc AS rcvr_service,
       left(A.action_result, 7) AS actn_rslt, left(RF.transport_result, 15) as send_result,
       RF.item_count AS COUNT, RF.next_action as NEXT_ACT
from report_file as RF
join action as A ON A.action_id = RF.action_id
where RF.report_id in (select report_descendants('$REPORT_UUID'))
order by A.action_id, RF.sending_org;
EOF

printf "\n${RED}Now find just the reports that were successfully sent${NC}\n"
psql prime_data_hub <<EOF
select RF.report_id, A.action_id as ACT_ID, A.action_name as action_taken,
       RF.body_format as FMT, RF.receiving_org AS receiver, receiving_org_svc AS rcvr_service, left(A.action_result, 7) AS actn_rslt, left(RF.transport_result, 15) as send_result, RF.item_count AS COUNT, RF.next_action as NEXT_ACT
from report_file as RF
join action as A ON A.action_id = RF.action_id
where RF.report_id in (select find_sent_reports('$REPORT_UUID'))
order by A.action_id, RF.sending_org;
EOF

printf "\n${RED}Now find the reports that were NEVER sent.   These might need investigation.${NC}\n"
psql prime_data_hub <<EOF
select RF.report_id, A.action_id as ACT_ID, A.action_name as action_taken,
       RF.body_format as FMT, RF.receiving_org AS receiver, receiving_org_svc AS rcvr_service,
       left(A.action_result, 7) AS actn_rslt, left(RF.transport_result, 15) as send_result,
       RF.item_count AS COUNT, RF.next_action as NEXT_ACT
from report_file as RF
join action as A ON A.action_id = RF.action_id
where RF.report_id in (select find_withered_reports('$REPORT_UUID'))
order by A.action_id, RF.sending_org;
EOF

exit 0

