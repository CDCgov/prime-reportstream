#  Hub Release Notes

*May 27, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Addition of Quality Check

A basic Quality check on certain fields is now applied to Covid-19 data before sending it to downstream public health departments.
```
For basic Covid test quality, these fields MUST be supplied:
            message_id
            equipment_model_name
            specimen_type
            test_result
            patient_last_name
            patient_first_name
            patient_dob

For minimal valid location or other contact info (for contact tracing), at least ONE of these must be supplied:
            patient_street
            patient_zip_code
            patient_phone_number
            patient_email
For valid date/time (for relevance/urgency), at least ONE of these date fields must be supplied:
            order_test_date
            specimen_collection_date_time
            test_result_date

For valid CLIA numbers, at least ONE of the following fields must be supplied:
           testing_lab_clia
           reporting_facility_clia

Important Notes
- The field names above are our ReportStream names - each jurisdiction often uses different names for these fields.
- It is easy to override the above default quality filter on a per-jurisdiction basis, as needed, to make it stricter or more lenient.
- It is also possible to turn off the default filter completely, by using the allowAll() filter.
- This is new, so we may tweak it.   
- "supplied" means: The column name must be present (for CSV data), and a valid value must be present.

```
