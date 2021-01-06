# 2020-12-29 Hub Release Notes

# For clients sending data to the Hub

This release contains the new `option` URL query parameters, documented here:
[Hub OpenApi Spec](../openapi.yml)

Also see: [Link to detailed schema dictionaries](../schema_documentation)

Here's some helpful examples of the various options

## No options specified

`curl -X POST -H "client:simple_report" -H "Content-Type: text/csv" -H "x-functions-key:<secret>" --data-binary "@./src/test/csv_test_files/input/simplereport.csv" "https://prime-data-hub-prod.azurefd.net/api/reports"`

### Example Results
```
{
  "id" : "0336462c-7002-4e08-a4de-9606752f32fa",
  "destinations" : [ "Sending 50 items to Arizona PHD (elr-test) at 2020-12-29T16:23-07:00", "Sending 25 items to Pima County, Arizona PHD at 2020-12-29T16:23-07:00" ],
  "errors" : [ ],
  "warnings" : [ {
    "scope" : "REPORT",
    "id" : "",
    "details" : "Missing 'Test_result_status' header"
  } ]
}
```

## With `option=CheckConnections`

Always safe to call in Production

`curl -X POST -H "client:simple_report" -H "Content-Type: text/csv" -H "x-functions-key:<secret>" --data-binary "@./src/test/csv_test_files/input/simplereport.csv" "https://prime-data-hub-prod.azurefd.net/api/reports?option=CheckConnections"`

### Everything looks good, so you see this:

```
{
  "id" : null,
  "destinations" : [ ],
  "errors" : [ ],
  "warnings" : [ ]
}
```

## With	`option=ValidatePayload`:  Parse the data for correctness

Always safe to call in Production

`curl -X POST -H "client:simple_report" -H "Content-Type: text/csv" -H "x-functions-key:<secret>" --data-binary "@./src/test/csv_test_files/input/simplereport.csv" "https://prime-data-hub-prod.azurefd.net/api/reports?option=ValidatePayload"`

### Note the destinations is now empty.
```
{
  "id" : "42ec8a8f-f7bd-42e5-85ab-c0d69364f6f3",
  "destinations" : [ ],
  "errors" : [ ],
  "warnings" : [ {
    "scope" : "REPORT",
    "id" : "",
    "details" : "Missing 'Test_result_status' header"
  } ]
}
```

## With `option=SkipSend`: Run data thru the gauntlet, but safely skip the final step

Always safe to call in Production

`curl -X POST -H "client:simple_report" -H "Content-Type: text/csv" -H "x-functions-key:<secret>" --data-binary "@./src/test/csv_test_files/input/simplereport.csv" "https://prime-data-hub-prod.azurefd.net/api/reports?option=SkipSend"`

### You can't tell any difference here, but in theory the Hub team can watch for internal errors.  We plan to build a query api that lets you track data custody

```
{
  "id" : "69bb28ee-5dd0-4f80-91de-b094a45518b7",
  "destinations" : [ ],
  "errors" : [ ],
  "warnings" : [ {
    "scope" : "REPORT",
    "id" : "",
    "details" : "Missing 'Test_result_status' header"
  } ]
}
```
