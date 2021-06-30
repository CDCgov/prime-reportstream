#!/usr/bin/env bash

TITLE='\033[1;4m'
NC='\033[0m' # No Color

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

echo
echo -e "${TITLE}##### Uploading first set of fake data${NC}"
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.a.csv' \
  'http://localhost:7071/api/reports' \
  > ./Test-post.a.json
echo

echo
echo -e "${TITLE}##### Uploading second set of fake data${NC}"
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.b.csv' \
  'http://localhost:7071/api/reports' \
  > ./Test-post.b.json
echo

echo
echo -e "${TITLE}##### Uploading third set of fake data${NC}"
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.c.csv' \
  'http://localhost:7071/api/reports' \
  > ./Test-post.c.json
echo

echo
echo -e "${TITLE}Duplicate upload of second set of fake data${NC}"
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.b.csv' \
  'http://localhost:7071/api/reports' \
  > ./Test-post.bb.json
echo
