#  ReportStream Release Notes

*October 18, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### More instructive error and warning messages in API response

Previously, when there were problems with health data the error or warning message would simply state where the problem occurs. 
These changes update the messages so also include instructions for correcting the problems. For example:

Previous message:
> Invalid phone number format in row 3.

Updated message:
> Invalid phone number format in row 3. Reformat to a 10-digit phone number (e.g. (555) - 555-5555).
