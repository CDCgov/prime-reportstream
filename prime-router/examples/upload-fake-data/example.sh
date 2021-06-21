#!/usr/bin/env bash

cd ../../../prime-router/

echo
echo "Generating fake data"
./prime data --input-fake 23 \
  --input-schema primedatainput/pdi-covid-19 \
  --output examples/upload-fake-data/Test.csv
echo


cd examples/upload-fake-data/

echo
echo "Uploading fake data"
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.csv' \
  'http://172.17.0.1:7071/api/reports'
echo


echo
echo 'View the download page:'
echo '  http://172.17.0.1:7071/api/download'
echo
