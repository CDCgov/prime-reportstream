#  ReportStream Release Notes

*December 12, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Addition of ability to attach a filename to your URL submission

When uploading to the ReportStream REST endpoint, the actual data payload is sent as part of the HTTP body.

This means that ReportStream does not have or track a filename associated with the file; in fact for automated systems, the data might not come from a file at all.

However, many senders to ReportStream will be sending files, and it would be valuable to track the filename for later reference by the sender.

The new payloadName parameter is designed to meet this need.

Here is an example, using curl:

```
curl -X POST -H "client:healthytests"  -H "content-type:text/csv"  --data-binary "@mytestfile12345.csv" "https://prime.cdc.gov/api/reports?payloadName=mytestfile12345.csv"
```

ReportStream does not use the payloadName, however, ReportStream will associated the payloadName with the submission as part of its history/lineage tracking, so that users can map submissions back to original filenames.

The payloadName does not have to be a filename - it could be any string that is meaningful to the data sender.   The name will appear in the json response as the 'externalName'.  Max 1000 chars.

### Additions to the json response

In addition to the externalName field, the httpStatus and sender names have been added to the standard json response.

