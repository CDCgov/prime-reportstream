#  Hub Release Notes

*May 12, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Changes to api/reports response

This release adds the configured topic for the organization sender to the Json response. The openapi.yml was updated to reflect the changes as well.

```
{
  "id" : "abcd1234-abcd-1234-abcd-abcd1234abcd",
  "topic" : "covid-19",
  "reportItemCount" : 25,
  "destinations" : [ {
```
