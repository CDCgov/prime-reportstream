#!/usr/bin/env bash

echo
echo "Creates the stored procedures that do lineage queries."
docker exec --user 999:999 \
  -i $(docker ps -f name=db_pgsql -q) \
  psql prime_data_hub prime \
    < ./lineage-report-stored-procedures.sql
echo


cd ../../../prime-router/
echo
echo "Generating first set of 200 lines of fake data"
./prime data --input-fake 200 \
  --input-schema primedatainput/pdi-covid-19 \
  --output examples/data-lineage/Test.a.csv
echo

echo "Generating second set of 200 lines of fake data"
./prime data --input-fake 200 \
  --input-schema primedatainput/pdi-covid-19 \
  --output examples/data-lineage/Test.b.csv
echo

echo "Generating third set of 200 lines of fake data"
./prime data --input-fake 200 \
  --input-schema primedatainput/pdi-covid-19 \
  --output examples/data-lineage/Test.c.csv
echo


cd examples/data-lineage/

echo
echo "Uploading first set of fake data"
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.a.csv' \
  'http://localhost:7071/api/reports' \
  > ./Test-post.a.json
echo

echo
echo "Uploading second set of fake data"
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.b.csv' \
  'http://localhost:7071/api/reports' \
  > ./Test-post.b.json
echo

echo
echo "Uploading third set of fake data"
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.c.csv' \
  'http://localhost:7071/api/reports' \
  > ./Test-post.c.json
echo

echo
echo "Duplicate upload of second set of fake data"
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.b.csv' \
  'http://localhost:7071/api/reports' \
  > ./Test-post.bdup.json
echo

