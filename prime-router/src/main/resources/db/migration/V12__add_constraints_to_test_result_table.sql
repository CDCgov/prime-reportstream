/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database
Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema
Copy a version of this comment into the next migration
*/

/* Update certain fields to check null is being passed in instead of empty string */
UPDATE
    covid_result_metadata
SET
    ordering_provider_id = null
WHERE
    TRIM(ordering_provider_id) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT ck_ordering_provider_id
        CHECK (ordering_provider_id IS NULL OR LENGTH(ordering_provider_id) > 0);


UPDATE
    covid_result_metadata
SET
    ordering_provider_state = null
WHERE
    TRIM(ordering_provider_state) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT ck_ordering_provider_state
        CHECK(ordering_provider_state IS NULL OR LENGTH(ordering_provider_state) > 0);


UPDATE
    covid_result_metadata
SET
    ordering_provider_postal_code = null
WHERE
    TRIM(ordering_provider_postal_code) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT  ck_ordering_provider_postal_code
        CHECK(ordering_provider_postal_code IS NULL OR LENGTH(ordering_provider_postal_code) > 0);


UPDATE
    covid_result_metadata
SET
    ordering_provider_county = null
WHERE
        TRIM(ordering_provider_county) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT  ck_ordering_provider_county
        CHECK(ordering_provider_county IS NULL OR LENGTH(ordering_provider_county) > 0);


UPDATE
    covid_result_metadata
SET
    ordering_facility_city = null
WHERE
        TRIM(ordering_facility_city) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT  ck_ordering_facility_city
        CHECK(ordering_facility_city IS NULL OR LENGTH(ordering_facility_city) > 0);


UPDATE
    covid_result_metadata
SET
    ordering_facility_county = null
WHERE
        TRIM(ordering_facility_county) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT  ck_ordering_facility_county
        CHECK(ordering_facility_county IS NULL OR LENGTH(ordering_facility_county) > 0);


UPDATE
    covid_result_metadata
SET
    ordering_facility_name = null
WHERE
        TRIM(ordering_facility_name) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT  ck_ordering_facility_name
        CHECK(ordering_facility_name IS NULL OR LENGTH(ordering_facility_name) > 0);


UPDATE
    covid_result_metadata
SET
    ordering_facility_postal_code = null
WHERE
        TRIM(ordering_facility_postal_code) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT  ck_ordering_facility_postal_code
        CHECK(ordering_facility_postal_code IS NULL OR LENGTH(ordering_facility_postal_code) > 0);


UPDATE
    covid_result_metadata
SET
    testing_lab_city = null
WHERE
        TRIM(testing_lab_city) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT  ck_testing_lab_city
        CHECK(testing_lab_city IS NULL OR LENGTH(testing_lab_city) > 0);


UPDATE
    covid_result_metadata
SET
    testing_lab_clia = null
WHERE
        TRIM(testing_lab_clia) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT  ck_testing_lab_clia
        CHECK(testing_lab_clia IS NULL OR LENGTH(testing_lab_clia) > 0);


UPDATE
    covid_result_metadata
SET
    testing_lab_county = null
WHERE
        TRIM(testing_lab_county) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT  ck_testing_lab_county
        CHECK(testing_lab_county IS NULL OR LENGTH(testing_lab_county) > 0);


UPDATE
    covid_result_metadata
SET
    testing_lab_name = null
WHERE
        TRIM(testing_lab_name) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT  ck_testing_lab_name
        CHECK(testing_lab_name IS NULL OR LENGTH(testing_lab_name) > 0);


UPDATE
    covid_result_metadata
SET
    testing_lab_postal_code = null
WHERE
        TRIM(testing_lab_postal_code) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT  ck_testing_lab_postal_code
        CHECK(testing_lab_postal_code IS NULL OR LENGTH(testing_lab_postal_code) > 0);


UPDATE
    covid_result_metadata
SET
    testing_lab_state = null
WHERE
        TRIM(testing_lab_state) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT  ck_testing_lab_state
        CHECK(testing_lab_state IS NULL OR LENGTH(testing_lab_state) > 0);


UPDATE
    covid_result_metadata
SET
    patient_county = null
WHERE
        TRIM(patient_county) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT  ck_patient_county
        CHECK(patient_county IS NULL OR LENGTH(patient_county) > 0);


UPDATE
    covid_result_metadata
SET
    patient_postal_code = null
WHERE
        TRIM(patient_postal_code) = '';

ALTER TABLE covid_result_metadata
    ADD CONSTRAINT  ck_patient_postal_code
        CHECK(patient_postal_code IS NULL OR LENGTH(patient_postal_code) > 0);


