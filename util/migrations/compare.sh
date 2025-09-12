#!/bin/bash

# Take the resulting files through the covid and universal pipelines and compare them
# Create a nifty <separator> delimited file to insert into the spreadsheet for easy comparison

# Example file structure:
# compare.sh
# bulk-entry/
#   |
#   ms-doh/
#     |
#     1_from_csv.hl7
#     1_from_fhir.hl7

# Change as needed
SEPARATOR="@"
ORG="ms-doh"
# Manually change to keep filenames organized as needed
i=1

# Change these
BASE_DIR="</path/to/files>/${ORG}"
PRIME_DIR="</path/to>/prime-router"

# Base example: We will compare '1_from_csv.hl7' with '1_from_fhir.hl7'
INPUT_CSV="${BASE_DIR}/${i}_from_csv.hl7"
INPUT_FHIR="${BASE_DIR}/${i}_from_fhir.hl7"

# Make the comparison base file
cd ${PRIME_DIR}
echo "Creating comparison file using 'prime hl7data'..."
echo "./prime hl7data --starter-file ${INPUT_CSV}  --comparison-file ${INPUT_FHIR} > "${BASE_DIR}/${i}_comparison_results.txt""
./prime hl7data --starter-file ${INPUT_CSV}  --comparison-file ${INPUT_FHIR} > "${BASE_DIR}/${i}_comparison_results.txt"
echo "...done"

# Format the output file a bit
echo "Reformatting comparison file..."
cd ${BASE_DIR}
f="${BASE_DIR}/${i}_comparison_results.txt"
f2="${BASE_DIR}/$(basename ${f} .txt)_formatted.txt"

# Cut out the prefix for the field names
sed -e 's/PATIENT_RESULT([[:digit:]]*)-PATIENT([[:digit:]]*)-//g' ${f} > ${f2}
sed -i -e 's/PATIENT_RESULT([[:digit:]]*)-ORDER_OBSERVATION([[:digit:]]*)-SPECIMEN([[:digit:]]*)-//g' ${f2}
sed -i -e 's/PATIENT_RESULT([[:digit:]]*)-ORDER_OBSERVATION([[:digit:]]*)-OBSERVATION([[:digit:]]*)-//g' ${f2}
sed -i -e 's/PATIENT_RESULT([[:digit:]]*)-ORDER_OBSERVATION([[:digit:]]*)-//g' ${f2}
# Cut out diff output text
sed -i -e '/-------diff output/d' ${f2}
sed -i -e "s/There were [[:digit:]]* differences between the input and output/\nHl7Segment${SEPARATOR}Covid(CSV) Data${SEPARATOR}UP (FHIR) Data\n/g" ${f2}

# Edit this line
# Use this to adjust and cut lines based on how many garbage log lines show up at the top
# Example: Cut the first ten lines since it's just generic log output
sed -i -e '1,10d' ${f2}

# Remove temp sed files
find . -iname "*-e" -delete

echo "...done"
echo "Result: ${f2}"