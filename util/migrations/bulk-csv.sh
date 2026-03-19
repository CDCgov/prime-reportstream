#!/bin/bash

# For creating HL7 files using the COVID pipeline with the bulk-upload CSV files

# Example file structure:
# bulk-csv.sh
# bulk-entry/
#   |
#   ms-doh/
#     |
#     1_input.csv
#     1_input.fhir

# Directory paths for where the input files are
BULK_PATH="</path/to/bulk-entry>"

# Directory of prime router
PRIME_DIR="</path/to/>prime-router"

# Input files to be converted through the pipelines
INPUT_CSV="</path/to/>input.csv"
#INPUT_FHIR="</path/to/>input.fhir"

# Change org and topic as needed
ORG="ms-doh"
TOPIC="elr"
cd ${BULK_PATH}

# Make HL7 from CSV
i=1
cd ${BULK_PATH}
mkdir -p "${i}"
cd ${PRIME_DIR}
./prime data --input ${INPUT_CSV} --input-schema upload-covid-19 --route-to ${ORG}.${TOPIC} --output ${BULK_PATH}/${ORG}/${i}_from_csv.hl7

# Make HL7 from FHIR
# Ended up manually sending an HTTP POST to /reports with the contents of the FHIR file
# #could probably make a cURL command out of it to insert here...maybe something like:
# curl --location 'http://localhost:7071/api/reports?processing=async' --headers <insert for each required header> --data <some path to the FHIR file>
