#!/usr/bin/env bash

# Use this variable to point to a different host on which your 'local' API runs
# This can be useful if you are running the end-to-end test in a container
# as opposed to on your actual localhost (e.g. the builder container)
# Default Value (i.e. if unspecified): localhost
PRIME_RS_API_ENDPOINT_HOST=${PRIME_RS_API_ENDPOINT_HOST:-localhost}

for i in $(seq 1000); do
  curl \
    -X POST \
    -H "Content-Type:text/csv" \
    --data-binary @result_files/fake-pdi-covid-19.csv \
    "http://${PRIME_RS_API_ENDPOINT_HOST?}:7071/api/reports?client=simple_report"
  echo "Post $i times"
  sleep .5
done
