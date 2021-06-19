#  ReportStream Release Notes

*June 22, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### patientAgeUnits / patAgeUnits

We have added an additional column to data generated for HHSProtect:  patAgeUnits.   For our waters senders now sending directly to ReportStream, that field has been added as well, as patientAgeUnits.  The expected values are yr for year or mo for months.   Children under 2 years should have their age given in months, per HHS guidance.

