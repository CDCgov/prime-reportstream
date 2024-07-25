#!/bin/bash

# This bash script is used for applying the fhirdata primeCLI command to an entire directory.
# This is useful when hl7 mappings need to be updated and this would cause a large number of fhir test output files to
# need to be updated as well.
# See ProcessFhirCommands.kt for more information on usage of the fhirdata primeCLI command.
#
# Example usage:
#   (To be run from within the project root directory)
#   bash ./recreate-fhir-in-dir.sh prime-router/src/testIntegration/resources/datatests/HL7_to_FHIR/
#
# Limitations: This script will update all differences including id fields, datetime fields, etc.
# This can create a slightly noisy git diff. There may also be discrepancies with the json formatting between the
# existing file and the output of this script. In that case use your best judgement and separate out formatting
# changes into their own commits.

root=prime-router/src/testIntegration/resources/datatests/mappinginventory/
# Function to recursively print relative paths without extensions
print_paths() {
    local dir="$1"
    local ext
    shopt -s nullglob  # Handle empty directories
    local remove="${directory}/"
    for file in "$dir"/*; do
        if [ -d "$file" ]; then
            print_paths "$file"
        elif [ -f "$file" ]; then
            ext="${file##*.}"  # Get the file extension
            local no_ext="${file%.$ext}"
            local no_dir="${no_ext#$root}"
            printf "%s\n" $no_dir   # Print relative path without extension
            ./gradlew primeCLI --args="fhirdata --input-file  src/testIntegration/resources/datatests/mappinginventory/${no_dir}.hl7 --output-file src/testIntegration/resources/datatests/mappinginventory/${no_dir}.fhir  --output-format FHIR"
        fi
    done
}

# Check if a directory path is provided as an argument
if [ $# -eq 0 ]; then
    echo "Usage: $0 /path/to/your/directory"
    exit 1
fi

directory="$1"

# Check if the provided path is a directory
if [ ! -d "$directory" ]; then
    echo "Error: $directory is not a directory"
    exit 1
fi

# Call the function with the provided directory path
print_paths "$directory"