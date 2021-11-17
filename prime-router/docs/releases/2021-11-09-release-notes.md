#  ReportStream Release Notes

*November 9, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Results from the receiver quality filters in the response.

A new `filteredReportRows` entry has now been added to `destination` in the json response.

 Each jurisdiction that receives data from ReportStream can configure 'quality filters' that filter out data that does not meet that receiver's needs.   With this release we are now returning information back to submitters on why certain rows were filtered from going to a particular jurisdiction.  
 
Each entry lists the filter applied and the rows that were filtered by it.  `filteredReportRows` is present only if at least one row has been filtered.

Many receivers have a 'secondary' feed, often designed to catch data that was filtered from that receiver's primary feed.   In those cases, if the submitted data shows as filtered from the primary feed, it may still have been sent to that jurisdiction's secondary feed.

**Examples**

All Items filtered out:

ReportStream applies a number of quality filters on behalf of the California Dept of Public Health.  One of these filters requires a number of fields to have valid values.  (See `hasValidDataFor` and the list of values, in the example below).   In this example, it turns out that all three of the data items destined for CA failed this test, so all are being filtered.

However, as you can see in the destinations list, all those data items are being sent to CA's secondary feed.
```
{
    "id": "8049440b-f95a-43e9-bdb7-a7cdfbf261af",
    "timestamp": "2021-11-05T15:37:47.804670Z",
    "topic": "covid-19",
    "reportItemCount": 3,
    "destinations": [
        {
            "organization": "California Department of Public Health",
            "organization_id": "ca-dph",
            "service": "elr",
            "filteredReportRows": [
                "For ca-dph.elr, qualityFilter hasValidDataFor, [message_id, equipment_model_name, specimen_type, test_result, patient_last_name, patient_first_name, patient_dob] filtered out Rows 1,2,3 reducing the Item count from 3 to 0."
            ],
            "sending_at": "never - all items filtered out",
            "itemCount": 0
        },
        {
            "organization": "California Department of Public Health",
            "organization_id": "ca-dph",
            "service": "elr-secondary",
            "sending_at": "2021-11-05T11:38-04:00",
            "itemCount": 3
        }
    ],
    "destinationCount": 2,
    "warningCount": 0,
    "errorCount": 0,
    "errors": [],
    "warnings": [],
    "consolidatedErrors": [],
    "consolidatedWarnings": []
}
```

Some but not all items filtered out:

In this example one of the items passed CA DPH's filter criteria, but two items failed.

A note on the item numbering:  ReportStream can accept data in CSV and HL7 batch formats.

For a CSV submission, an 'item' is just a row of data in the CSV, and item 1 is the first row of actual data _after_ the header row, item 2 is the second row of actual data, and so on.

For an HL7 batch submission an 'item' is a single HL7 message in the batch, and item 1 is the first message in the batch, item 2 is the second message, and so on.


```
{
    "id": "5fd89dcc-4767-40be-b72b-9ebfbe0db8e5",
    "timestamp": "2021-11-05T15:57:38.582762Z",
    "topic": "covid-19",
    "reportItemCount": 3,
    "destinations": [
        {
            "organization": "California Department of Public Health",
            "organization_id": "ca-dph",
            "service": "elr",
            "filteredReportRows": [
                "For ca-dph.elr, qualityFilter hasValidDataFor, [message_id, equipment_model_name, specimen_type, test_result, patient_last_name, patient_first_name, patient_dob] filtered out Rows 1,3 reducing the Item count from 3 to 1."
            ],
            "sending_at": "2021-11-05T11:58-04:00",
            "itemCount": 1
        },
        {
            "organization": "California Department of Public Health",
            "organization_id": "ca-dph",
            "service": "elr-secondary",
            "sending_at": "2021-11-05T11:58-04:00",
            "itemCount": 2
        }
    ],
    "destinationCount": 2,
    "warningCount": 0,
    "errorCount": 0,
    "errors": [],
    "warnings": [],
    "consolidatedErrors": [],
    "consolidatedWarnings": []
}
```

No items filtered out:
```
{
    "id": "e6f4b7ea-7a74-4beb-ae82-bfecfedbe6c6",
    "timestamp": "2021-11-05T15:58:04.009910Z",
    "topic": "covid-19",
    "reportItemCount": 3,
    "destinations": [
        {
            "organization": "California Department of Public Health",
            "organization_id": "ca-dph",
            "service": "elr",
            "sending_at": "2021-11-05T11:59-04:00",
            "itemCount": 3
        }
    ],
    "destinationCount": 1,
    "warningCount": 0,
    "errorCount": 0,
    "errors": [],
    "warnings": [],
    "consolidatedErrors": [],
    "consolidatedWarnings": []
}
```