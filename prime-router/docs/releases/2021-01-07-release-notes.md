#  Hub Release Notes

*Thursday January 7, 2021*

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

## Incoming Data Formats and Customers

Now accepting data from Strac.   See [Link to detailed schema dictionaries](../schema_documentation)

## Outgoing Data Formats and Customers

Hub now sends HL7 data to AZ TEST site (only).   This data will be identical to the .csv data sent to TEST, but in HL7 instead of CSV format, so they can be compared.   Note:  currently this data is not 'batched', so there is no delay - it will be sent as soon as its sent by simplereport.

New outgoing data formats are now available for PA, FL, CO.  See [Link to detailed schema dictionaries](../schema_documentation)

## Timing for AZ

- In anticipation of SimpleReport sending us one report at a time, we've switching the timing of sends to AZ DHS to 6 times a day, every 4 hours.
- In test, files are sent to our mock sftp site every 10 minutes.
- Production Pima is now set to send files once a day, at 8am AZ time.  However, currently there is no send to Pima, its still manual.

