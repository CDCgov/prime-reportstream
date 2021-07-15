#  ReportStream Release Notes

*July 7, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Corrected phone numbers to follow HL7 2.5.1 expectations 

The first component of a Telephone number has been changed to `(###)###-####`
which follows of the HL7 2.3 specification for the US. It was `(###)#######`. 
In addition, if an extension number was present, it is now added to the number. 