#  ReportStream Release Notes

*June 25, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Submit Results in HL7 Format

Added the ability to send results as one HL7 message or an HL7 batch message. The content 
type of `application/hl7-v2` must be provided in the headers when sending the results in HL7 format.  

