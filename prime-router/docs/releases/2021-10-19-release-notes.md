#  ReportStream Release Notes

*October 18, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### More instructive error and warning messages in API response

Previously, when there were problems with health data the error or warning message would simply state where the problem occurs. 
These changes update the messages so also include instructions for correcting the problems. Example new messages:

Invalid phone number:
> Invalid phone number 555-5555 for phoneNumber. Reformat to a 10-digit phone number (e.g. (555) - 555-5555).

Invalid date:
> Invalid date: 2021/09-02 for element testOrderedDate. Reformat to YYYYMMDD.

Invalid code:
> Invalid code: 'R' is not a display value in altValues set for patientSex.

Invalid postal code:
> Invalid postal code 1234 for patZip. Reformat to 01234.

Invalid column count:
> Expecting 26 columns, but found 28 in row 8.

Too many rows:
> Your file's row size of 20000 exceeds the maximum of 10000 rows per file. Reduce the amount of rows in this file.

Too many columns:
> Number of columns in your report exceeds the maximum of 2000 allowed. Adjust the excess columnar data in your report.

General CSV parsing error:
> There's an issue parsing your file. Contact the ReportStream team at reportstream@cdc.gov.

Missing column header:
> Your file is missing patientAge header. Add a header.

Unexpected header:
> Unexpected column header found, 'starSign' will be ignored.

Too many errors:
> Report file failed: Number of errors exceeded threshold. Contact the ReportStream team at reportstream@cdc.gov for assistance.


