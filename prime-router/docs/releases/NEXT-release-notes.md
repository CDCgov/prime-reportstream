#  Hub Release Notes
*Next Release* - no date set yet.

All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)

# For clients sending data to the Hub

## Changes to the response json

Added these fields to the response to a POST to `reports/`
- `reportItemCount`
- `errorCount`
- `warningCount`
For details, see the [Hub OpenApi Spec](../openapi.yml)

## SimpleReport Schema changes

Hub is now ready to accept these fields, currently listed as optional:
- Processing_mode_code
- Test_result_status

Note: the data dictionary for SimpleReport csv data sent to the hub is here: [Simple Report Schema](../schema_documentation/primedatainput-pdi-covid-19.md)

