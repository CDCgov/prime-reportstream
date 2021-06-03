#  Hub Release Notes

*June 1, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Addition of Quality Check

A basic Quality check on certain fields is now applied to Covid-19 data before sending it to downstream public health departments.
```
To ensure basic Covid test quality, these fields MUST be supplied:
            message_id
            equipment_model_name
            specimen_type
            test_result
            patient_last_name
            patient_first_name
            patient_dob

To ensure minimal valid location or other contact info (for contact tracing), at least ONE of these must be supplied:
            patient_street
            patient_zip_code
            patient_phone_number
            patient_email
To ensure valid date/time (for relevance/urgency), at least ONE of these date fields must be supplied:
            order_test_date
            specimen_collection_date_time
            test_result_date

To ensure valid CLIA numbers, at least ONE of the following fields must be supplied and must be a valid CLIA (10 alphanumeric characters)
           testing_lab_clia
           reporting_facility_clia

Important Notes
- The field names above are our ReportStream names - each jurisdiction often uses different names for these fields.
- It is easy to override the above default quality filter on a per-jurisdiction basis, as needed, to make it stricter or more lenient.
- It is also possible to turn off the default filter completely, by using the allowAll() filter.
- "supplied" means: The column name must be present (for CSV data), and a valid value must be present.

```

### Error and Warning Message Improvements

If there are problems with a data submission to ReportStream's reports endpoint, it returns detailed warning and error messages.   The submission continues even if there are warnings, but the entire submission will fail if there are any errors.

We have fixed a problem where ReportStream was failing to give warnings when a non-required coded field had an improper value.  Coded fields are those with an enumerated set of possible values, as found in a SNOMED, LOINC, HL7, or other valueset.    If a coded field is not required, and an illegal or unknown value is passed, ReportStream will continue to process that record to completion, but will replace the erroneous value with an empty string - this is all as designed.  However, we were not providing a warning back to the sender in these situations.  This is now fixed, so you may see more warnings in your submissions than before.

Example:
```
{
    "scope" : "ITEM",
    "id" : "2021042920-dsr^2021042920-f6ebbc3b133243e482cfdb4537c55ba4",
    "details" : "Invalid phone number 'n/a' for 'orderingProviderPhone' ('ordering_provider_phone_number') - setting value to ''"
}
```

#### Error and Warning Limits

Note that currently there is no limit on the number of warnings you may get.  Depending on the type of error, there is a limit of about 100 errors, after which ReportStream will stop processing the report.

