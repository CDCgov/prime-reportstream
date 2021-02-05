#  Hub Release Notes

*Feb 4, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Changes to the Schemas, including for SimpleReport:

Added optional field `Corrected_result_ID`, a pointer/link to the unique Result_ID of a previously submitted result.  Usually blank. Or, if an item modifies/corrects a prior item, this field holds the ResultID of the prior item.

### LVID table updated

Added support for the January 20, 2021 LVID table. New tests supported includes the Binax NOW Home test. 

### Download site

- Download site now operational.
- Download files now have improved filenames, with the timestamp embedded in the filename.
- Filenames used by sftp send are now identical to the download site filesnames.
- If a disallowed or garbled report_id is access, delivers a basic 'not found' message (rather than no message at all).  Should never occur under normal site link usage.
