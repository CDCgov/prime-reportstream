#  Hub Release Notes

*June 8, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### File name changes

We are preparing the code to provide better filename formats.   More to come.

### Quality Updates

#### Default Quality Filter now excludes Training/Test and Debug data

The current default quality filter is:
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

The processing_mode_code must not be T (Training/Test) or D (Debug).
```

#### Secondary feed

States can now receive a secondary feed of data that does NOT meet the above filter criteria.  Other criteria can be added on a per-jurisdiction basis, or the quality filter itself can be overridden on a per-jurisdiction basis.



