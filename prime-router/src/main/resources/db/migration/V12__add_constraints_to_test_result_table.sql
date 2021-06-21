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

