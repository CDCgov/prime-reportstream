#  ReportStream Release Notes

*November 9, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Results from the receiver quality filters in the response.

`filteredReportRows` entry added to `destination` in the response when rows have been filtered out by a receivers configured quality filters. Eacy entry lists the filter used, the arguments for it, and the rows that were filtered by it. This key is present only if at least one row has been filtered out, and is never present for secondary destinations.

**Examples**

All Items filtered out:
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

Multiple Items filtered out
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