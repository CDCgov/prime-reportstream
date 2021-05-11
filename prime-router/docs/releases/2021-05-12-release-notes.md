#  Hub Release Notes

*May 12, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Changes to api/reports response

This release adds the configured topic for the organization sender to the Json response along with an ISO-8601 timestamp. The openapi.yml was updated to reflect the changes as well.

```
{
  "id" : "abcd1234-abcd-1234-abcd-abcd1234abcd",
  "timestamp" : "2021-05-11T20:05:02.571867Z",
  "topic" : "covid-19",
  "reportItemCount" : 25,
  "destinations" : [ {
```
