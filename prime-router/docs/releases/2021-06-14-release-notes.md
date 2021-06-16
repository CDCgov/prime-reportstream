#  ReportStream Release Notes

*June 14, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Abnormal_flag

As part of the jurisdiction filters feature, we are now defining a positive and negative results flag that 
can be used to create separate positive and negative feeds. The abnormal_flag was previously
only available on output as an HL7 OBX-8 value. 

### AS2 Transport

Added the ability to send results over an HTTPS/AS2 transport. Applicability Statement 2 (AS2) is
common in EDI systems and is used by HIEs. 
