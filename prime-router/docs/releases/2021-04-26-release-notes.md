#  Report Stream April 26, 2021

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### The `/api/reports` end-point handles malformed CSVs differently

A POST to the end-point (i.e. a submission of a report) will error if the CSV contains a malformed row. 
Examples of malformed rows include:
 - Empty rows
 - Rows with different number of columns

Previously, the row would be skipped and warning would be given for the row. 
The new behavior is to error on these malformed CSV files. 