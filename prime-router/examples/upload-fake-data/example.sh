#!/usr/bin/env bash

cd ../../../prime-router/

# Use this variable to point to a different host on which your 'local' API runs
# This can be useful if you are running the end-to-end test in a container
# as opposed to on your actual localhost (e.g. the builder container)
# Default Value (i.e. if unspecified): localhost
PRIME_RS_API_ENDPOINT_HOST=${PRIME_RS_API_ENDPOINT_HOST:-localhost}

echo
echo "Generating fake data"
./prime data --input-fake 23 \
  --input-schema primedatainput/pdi-covid-19 \
  --output examples/upload-fake-data/Test.csv
echo


cd examples/upload-fake-data/

echo "API Host is set to \"${PRIME_RS_API_ENDPOINT_HOST?}\""

echo
echo "Uploading fake data"
curl -X POST -H 'client: simple_report' \
  -H 'Content-Type: text/csv' \
  --data-binary '@./Test.csv' \
  "http://${PRIME_RS_API_ENDPOINT_HOST?}:7071/api/reports"
echo


echo
echo 'View the download page:'
echo "  http://${PRIME_RS_API_ENDPOINT_HOST?}:7071/api/download"
echo
