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

### Addition of Quality Check

A basic Quality check on certain fields is now applied to Covid-19 data before sending it to downstream public health departments.
```
       // valid human and valid test
        "hasValidDataFor(" +
            "message_id," +
            "equipment_model_name," +
            "specimen_type," +
            "test_result," +
            "patient_last_name," +
            "patient_first_name," +
            "patient_dob" +
        ")",
        // has valid location (for contact tracing)
        "hasAtLeastOneOf(patient_street,patient_zip_code)",
        // has valid date (for relevance/urgency)
        "hasAtLeastOneOf(order_test_date,specimen_collection_date_time,test_result_date)",
        // able to conduct contact tracing
        "hasAtLeastOneOf(patient_phone_number,patient_email)"
```

