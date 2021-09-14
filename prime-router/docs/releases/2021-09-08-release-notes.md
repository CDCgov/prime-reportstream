#  ReportStream Release Notes

*September 8, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### HL7 option to add NCES ID to ordering facility name

`useNCESFacilityName` flag added to HL7 receiver settings. If set and `site_of_care` is `k12`, ReportStream will lookup
the facility name in the NCES ID table and set the ORC-21 according to the APHL guidance. 

