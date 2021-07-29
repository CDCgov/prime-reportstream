#  ReportStream Release Notes

*July 22, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Changes to api/reports response

This release adds a new `routing` element to the Json response if the query string contains a `verbose=true` parameter. This routing shows the destination(s) for each item in the report along with the report index and trackingId. The openapi.yml has been updated to reflect these changes with the structure of the routing element. Example curl command with query string parameters:
```
curl -X POST -H 'client: sample_client' -H 'Content-Type: text/csv' \
    --data-binary '@./examples/upload-fake-data/sample_report.csv' \
    'http://localhost:7071/api/reports?verbose=true'
```

An example of this routing element:

```
"routing" : [ {
    "reportIndex" : 0,
    "trackingId" : "123456",
    "destinations" : [ "st-phd.elr", "county-st-phd.elr" ]
  }, {
    "reportIndex" : 1,
    "trackingId" : "987654",
    "destinations" : [ "st-phd.elr" ]
  } ]  
```

### Data Model / Schema changes

- Added a new field, patAgeUnits, to payloads delivered to HHSProtect, per the HHS standard.
- Waters data now all flowing to HHSProtect
- Added site_of_care field as optional on all Covid data
- Added support for HL7 date/times with no offset
