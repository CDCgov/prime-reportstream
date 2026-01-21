#!/bin/sh

# ==========

# Very rough script to call the Prime CLI to use the `fhirdata` (for FHIR files) or `data` (for CSV files) commands
# to process reports through the universal or COVID pipelines

# ==========

# This script has mainly been written for tested using zsh
# This script requires: awk, jq, prime CLI

if ! command -v awk >/dev/null; then
    echo "Please install 'awk' to use this script"
    exit
fi

if ! command -v jq >/dev/null; then
    echo "Please install 'jq' to use this script"
    exit
fi

# ==========

## Expected directory setup:
## FHIR files for UP in fhir directory
## CSV files for CP in csv directory

# process-reports.sh
# fhir/
#   |
#   1/
#     |
#     1.fhir
#   2/
#     |
#     2.fhir
# csv/
#   1/
#     |
#     1.csv
#   2/
#     |
#     2.csv

# ==========

# Output format: 'HL7' or 'FHIR'
OUT_FORMAT="HL7"

# classpath to simple-report-sender-transform
TRANSFORM="classpath:/metadata/fhir_transforms/senders/SimpleReport/simple-report-sender-transform.yml"

# absolute path to prime router directory
PRIME_ROUTER_DIR="<path>/<to>/<prime router>"

# Pipeline is 'up' for universal pipeline, otherwise it's for the COVID pipeline
PIPELINE="up"

# ==========

# Universal pipeline branch to process FHIR files
if [[ ${PIPELINE} == "up" ]]; then
    BASE_DIR="$(pwd)/fhir"
    cd ${BASE_DIR}

    # Uncomment to use the first version of the for loop for every subdirectory
    # for i in "*/"; do
    # Use the second version if just targeting certain numbered directories
    for i in {1..3}; do
        REPORT_DIR="${BASE_DIR}/${i}"
        cd ${REPORT_DIR}

        FILE=$(readlink -f *.fhir)
        OUT_DIR="${REPORT_DIR}/output/${PIPELINE}/"
        mkdir -p ${OUT_DIR}
        TEMP_FILE="${BASE_DIR}/temp.fhir"
        
        cd ${PRIME_ROUTER_DIR}

        echo "./prime fhirdata --input-file ${FILE} -s ${TRANSFORM} --receiver-name=full-elr --org=mo-phd --output-file ${OUT_FILE}"
        j=0
        while read line || [ -n "$line" ]; do
            ((j++))
            echo $line > ${TEMP_FILE}
            echo "Bundle: ${j}"
            
            OUT_FILE="${OUT_DIR}$(jq -r '.identifier.value' ${TEMP_FILE})_${j}.$(echo "${OUT_FORMAT}" | awk '{print tolower($0)}')"
            ./prime fhirdata --input-file ${TEMP_FILE} -s ${TRANSFORM} --receiver-name=full-elr --org=mo-phd --output-file ${OUT_FILE}
        done < ${FILE}
    done
# COVID pipeline branch to process CSV files
else
    PIPELINE="cp"
    BASE_DIR="$(pwd)/csv"

    # for i in "*/"; do
    for i in {1..3}; do
        REPORT_DIR="${BASE_DIR}/${i}"
        cd ${REPORT_DIR}

        FILE=$(readlink -f *.csv)
        OUT_DIR="${REPORT_DIR}/output/${PIPELINE}/"
        mkdir -p "${OUT_DIR}"
        
        cd ${PRIME_ROUTER_DIR}

        echo "./prime data --input ${FILE} --input-schema upload-covid-19 --route-to mo-phd.elr --output ${OUT_DIR}$(basename ${FILE} .csv).hl7"
        ./prime data --input ${FILE} --input-schema upload-covid-19 --route-to mo-phd.elr --output "${OUT_DIR}$(basename ${FILE} .csv).hl7"
    done 
fi
