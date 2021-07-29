#!/usr/bin/env bash

# Run a nice end to end test, covering all our formats, and all our transports,
# and many different schemas.
#
# This assumes a prime/router is already running and available on your local host
# This assumes batch is on a 1-minute timer.
#
# This script is very ugly and repetitive.  Replace with kotlin.
#

RED='\033[0;31m'
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Use this variable to point to a different host on which your 'local' API runs
# This can be useful if you are running the end-to-end test in a container
# as opposed to on your actual localhost (e.g. the builder container)
# Default Value (i.e. if unspecified): localhost
PRIME_RS_API_ENDPOINT_HOST=${PRIME_RS_API_ENDPOINT_HOST:-localhost}

outputdir=./build/csv_test_files
starter_schema=primedatainput/pdi-covid-19
database=prime_data_hub
testfile1=$outputdir/prime1.csv
testfile2=$outputdir/prime2.csv
rows=20

printf "${BLUE}End to end test: generate 2 fake data files, split/transform to 4 schemas, in 3 formats, then merge, transport using 2 transports${NC}\n\n"

mkdir -p $outputdir
# Generate two fake data files
# Dev note:  early on, I had problems with parsing output from ./prime before the file was actually created.
#   So I got in the habit of grabbing the output, and parsing it separately.
printf "${BLUE}Generating fake data with $rows rows${NC}\n"
fake1="./prime data --input-fake $rows --input-schema $starter_schema --output $testfile1 --target-counties=CSV,HL7_BATCH,HL7,REDOX --target-states=PM"
echo $fake1
text=$($fake1)
printf "$text\n"

printf "${BLUE}Generating second fake data file with $rows rows${NC}\n"
fake2="./prime data --input-fake $rows --input-schema $starter_schema --output $testfile2 --target-counties=CSV,HL7_BATCH,HL7,REDOX --target-states=PM"
echo $fake2
text=$($fake2)
printf "$text\n"

printf "${BLUE}Post first fake report to prime hub${NC}\n"
boilerplate_front="curl --silent -X POST -H client:simple_report -H Content-Type:text/csv "
boilerplate_back="http://${PRIME_RS_API_ENDPOINT_HOST?}:7071/api/reports"
echo Posting $testfile1 to reports endpoint
$boilerplate_front --data-binary @$testfile1 $boilerplate_back | cat > $testfile1.json
# Get the report_id from the output
report_id1=$(cat $testfile1.json | python <( echo '
import sys, json
print json.loads(sys.stdin.read())["id"]
' ))
if [ -z $report_id1 ] ; then
  printf "${RED}Post to prime hub failed, json response in $testfile1.json:${NC}\n"
  cat $testfile1.json
  exit 1
else
  printf "${GREEN}SUCCESS: Submitted report_id=$report_id1 ${NC}(json response in $testfile1.json)\n"
fi

printf "${BLUE}Post second fake report to prime hub${NC}\n"
echo Posting $testfile2 to reports endpoint
$boilerplate_front --data-binary @$testfile2 $boilerplate_back | cat > $testfile2.json
# Get the report_id from the output
report_id2=$(cat $testfile2.json | python <( echo '
import sys, json
print json.loads(sys.stdin.read())["id"]
' ))
if [ -z $report_id2 ] ; then
  printf "${RED}Post to prime hub failed, json response in $testfile2.json:${NC}\n"
  cat $testfile2.json
  exit 1
else
  printf "${GREEN}SUCCESS: Submitted report_id=$report_id2 ${NC}(json response in $testfile2.json)\n"
fi

# Assume Batch step is on a 1 minute timer.
printf "${BLUE}Sleeping for 75 seconds to test Batching timer${NC}\n"
sleep 75

(( successCount = rows / 4))

printf "\n\n${BLUE}Count item lineages from $report_id1 ${NC}\n"
printf "${BLUE}SUCCESS is 9 rows, all with item_count = $successCount${NC}\n"
psql prime_data_hub <<EOF
select count(*) item_count, IL.child_report_id, RF.receiving_org, RF.receiving_org_svc, A.action_name, schema_name
from item_lineage as IL
join report_file as RF on IL.child_report_id = RF.report_id
join action as A on A.action_id = RF.action_id
where receiving_org_svc != 'hl7-test'
  and item_lineage_id in (select item_descendants('$report_id1'))
group by IL.child_report_id, RF.receiving_org, RF.receiving_org_svc, A.action_name, schema_name
order by RF.receiving_org_svc, A.action_name;
EOF

printf "\n${BLUE}Count item lineages for hl7-test from $report_id1 ${NC}\n"
printf "${BLUE}SUCCESS is 3 rows, all with item_count = $successCount${NC}\n"
psql prime_data_hub <<EOF
select count(*) item_count, A.action_name, schema_name
from item_lineage as IL
join report_file as RF on IL.child_report_id = RF.report_id
join action as A on A.action_id = RF.action_id
where receiving_org_svc = 'hl7-test'
  and item_lineage_id in (select item_descendants('$report_id1'))
group by A.action_name, schema_name
order by A.action_name;
EOF

printf "\n${BLUE}Count item lineages from $report_id2 ${NC}\n"
printf "${BLUE}SUCCESS is 9 rows, all with item_count = $successCount${NC}\n"
psql prime_data_hub <<EOF
select count(*) item_count, IL.child_report_id, RF.receiving_org, RF.receiving_org_svc, A.action_name, schema_name
from item_lineage as IL
join report_file as RF on IL.child_report_id = RF.report_id
join action as A on A.action_id = RF.action_id
where receiving_org_svc != 'hl7-test'
  and item_lineage_id in (select item_descendants('$report_id2'))
group by IL.child_report_id, RF.receiving_org, RF.receiving_org_svc, A.action_name, schema_name
order by RF.receiving_org_svc, A.action_name;
EOF

printf "\n${BLUE}Count item lineages for hl7-test from $report_id2 ${NC}\n"
printf "${BLUE}SUCCESS is 3 rows, all with item_count = $successCount${NC}\n"
psql prime_data_hub <<EOF
select count(*) item_count, A.action_name, schema_name
from item_lineage as IL
join report_file as RF on IL.child_report_id = RF.report_id
join action as A on A.action_id = RF.action_id
where receiving_org_svc = 'hl7-test'
  and item_lineage_id in (select item_descendants('$report_id2'))
group by A.action_name, schema_name
order by A.action_name;
EOF

(( mergeSuccessCount = successCount * 2 ))
printf "\n${BLUE}Now show merging, by looking at level descendants for $report_id1${NC}\n"
printf "${BLUE}SUCCESS is 8 rows, all with item_count = $mergeSuccessCount${NC}\n"
psql prime_data_hub <<EOF
select sum(item_count) item_count, A.action_name, receiving_org_svc AS rcvr_service, schema_name
from report_file as RF
join action as A ON A.action_id = RF.action_id
where RF.report_id in (select report_descendants('$report_id1'))
  and action_name in ('send', 'batch')
group by receiving_org_svc, A.action_name, schema_name
order by receiving_org_svc, A.action_name;
EOF

printf "\n${BLUE}Now print out report level descendants for $report_id2${NC}\n"
printf "${BLUE}SUCCESS is 8 rows, all with item_count = $mergeSuccessCount${NC}\n"
psql prime_data_hub <<EOF
select sum(item_count) item_count, A.action_name, receiving_org_svc AS rcvr_service, schema_name
from report_file as RF
join action as A ON A.action_id = RF.action_id
where RF.report_id in (select report_descendants('$report_id2'))
  and action_name in ('send', 'batch')
group by receiving_org_svc, A.action_name, schema_name
order by receiving_org_svc, A.action_name;
EOF
exit 0
