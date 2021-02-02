#!/usr/bin/env bash

cd ../../../prime-router/

./prime data --input-fake 23 \
  --input-schema primedatainput/pdi-covid-19 \
  --output examples/upload-fake-data/Test.csv


cd examples/upload-fake-data/

curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.csv' \
  'http://localhost:7071/api/reports'


echo 'View the download page:'
echo '  http://localhost:7071/api/download'
echo

