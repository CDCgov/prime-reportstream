#  Hub Release Notes

*Feb 12, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Changes to api/reports response

This release changes the format of destinations in our return Json from informal sentences

```
"destinations" : [ "Sending 58 items to Jones County Public Health Dept (prod-elr) at 2021-02-08T12:00-07:00", etc ]
```

to more formal json:
```
  "destinations" : [ {
    "organization" : "Jones County Public Health Dept",
    "organization_id" : "prod-phd",
    "service" : "prod-elr",
    "sending_at" : "2021-02-08T12:00-07:00",
    "itemCount" : 58
  }, {
```

Also adds a 
`destinationCount`

If the data is valid, but is going nowhere, you'll see:
```
  "destinations" : [ ],
  "destinationCount" : 0,
```

