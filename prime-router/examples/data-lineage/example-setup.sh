#!/usr/bin/env bash

TITLE='\033[1;4m'
NC='\033[0m' # No Color

# Use this variable to point to a different host on which your 'local' API runs
# This can be useful if you are running the end-to-end test in a container
# as opposed to on your actual localhost (e.g. the builder container)
# Default Value (i.e. if unspecified): localhost
PRIME_RS_API_ENDPOINT_HOST=${PRIME_RS_API_ENDPOINT_HOST:-localhost}

cd ../../../prime-router/
echo
echo -e "${TITLE}##### Generating first set of 200 lines of fake data${NC}"
./prime data --input-fake 200 \
  --input-schema primedatainput/pdi-covid-19 \
  --output examples/data-lineage/Test.a.csv
echo

echo -e "${TITLE}##### Generating second set of 200 lines of fake data${NC}"
./prime data --input-fake 200 \
  --input-schema primedatainput/pdi-covid-19 \
  --output examples/data-lineage/Test.b.csv
echo

echo -e "${TITLE}##### Generating third set of 200 lines of fake data${NC}"
./prime data --input-fake 200 \
  --input-schema primedatainput/pdi-covid-19 \
  --output examples/data-lineage/Test.c.csv
echo


cd examples/data-lineage/

echo "API Host is set to \"${PRIME_RS_API_ENDPOINT_HOST?}\""

echo
echo -e "${TITLE}##### Uploading first set of fake data${NC}"
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.a.csv' \
  "http://${PRIME_RS_API_ENDPOINT_HOST?}:7071/api/reports" \
  > ./Test-post.a.json
echo

echo
echo -e "${TITLE}##### Uploading second set of fake data${NC}"
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.b.csv' \
  "http://${PRIME_RS_API_ENDPOINT_HOST?}:7071/api/reports" \
  > ./Test-post.b.json
echo

echo
echo -e "${TITLE}##### Uploading third set of fake data${NC}"
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.c.csv' \
  "http://${PRIME_RS_API_ENDPOINT_HOST?}:7071/api/reports" \
  > ./Test-post.c.json
echo

echo
echo -e "${TITLE}Duplicate upload of second set of fake data${NC}"
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.b.csv' \
  "http://${PRIME_RS_API_ENDPOINT_HOST?}:7071/api/reports" \
  > ./Test-post.bb.json
echo
