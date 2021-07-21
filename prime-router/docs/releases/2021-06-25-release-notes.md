#  ReportStream Release Notes

*June 25, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Submit Results in HL7 Format

Added the ability to ingest results as one HL7 message, or an HL7 batch message. The content 
type of `application/hl7-v2` must be provided in the headers when sending the results in HL7 format.  

### Documentation Updates

Added additional features to the documentation generation, including links for HL7 fields to the
documentation at Caristix.

### COVID-19 Test Result Metadata Capture

On ingestion of files from senders, ReportStream will now capture non-PII/non-PHI data allowing us to
build metrics and visualizations off of the data and provide trend analysis to our partners.

### HHSProtect

Fixed data formats to be a simple yyyyMMdd

### Dependency Updates
Updates done to:
- Postgresql libraries
- Flyway libraries
- Azure libraries

