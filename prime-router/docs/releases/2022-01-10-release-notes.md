#  ReportStream Release Notes

*January 10, 2022*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Addition of warnings and errors when submitting invalid test devices

When uploading to the ReportStream REST endpoint, a warning is returned if the test device information
provided in a report cannot be validated against the list provided by the 
[Department of Health and Human Services’ (HHS) LOINC Mapping spreadsheet](https://www.cdc.gov/csels/dls/sars-cov-2-livd-codes.html).
If a given field with the test device information is required and cannot be validated then an error is returned instead and the 
report is rejected.



