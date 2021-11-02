#  ReportStream Release Notes

*November 4, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Updates to the Warnings and Errors JSON response for `/api/waters`

Building upon the error and warning message updates from the 2021-10-19 release, a few changes have been made to
the server's error and warning responses.

1. `consolidatedWarnings` and `consolidatedErrors` have been renamed to `warnings` and `errors`.

3. The `scope` property has been added to every warning and error.

4. The previously-named `rows` property has been renamed to `itemNums` and is only
displayed for `ITEM` scope warnings and errors.

5. When running in `verbose` mode, the new `itemDetails` array displays an array of objects that
contain the `itemNum` and `groupId` properties per-item.