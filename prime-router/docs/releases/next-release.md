#  Hub Release Notes

*NO DATE SET*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## Changes to the Schemas, including for SimpleReport:

Added optional field `Corrected_result_ID`, a pointer/link to the unique Result_ID of a previously submitted result.  Usually blank. Or, if an item modifies/corrects a prior item, this field holds the ResultID of the prior item.

