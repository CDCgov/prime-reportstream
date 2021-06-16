/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database
Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema
Copy a version of this comment into the next migration
*/
CREATE TABLE covid_result_metadata
(
    covid_results_metadata_id BIGSERIAL PRIMARY KEY,
    -- the report file id
    report_id UUID NOT NULL  REFERENCES report_file(report_id) ON DELETE CASCADE,
    -- zero-based order of this data in the report.  (eg the first row of data has report_index == 0, etc)
    report_index INTEGER NOT NULL,
    -- the message id, ties back to the tracking_id item_lineage table
    -- better way to link, however, is (report_id, report_index) which is guaranteed unique and non-null in item_lineage.
    message_id VARCHAR(128) NULL,
    -- equipment information
    equipment_model VARCHAR(512) NULL,
    -- test result info
    test_result VARCHAR(512) NULL,
    test_result_code VARCHAR(32) NULL,
    -- ordering facility information
    ordering_facility_name VARCHAR(512) NULL,
    ordering_facility_city VARCHAR(128) NULL,
    ordering_facility_county VARCHAR(128) NULL,
    ordering_facility_state VARCHAR(128) NULL,
    ordering_facility_postal_code VARCHAR(32) NULL,
    -- ordering provider information
    ordering_provider_name VARCHAR(512) NULL,
    ordering_provider_id VARCHAR(128) NULL,
    ordering_provider_county VARCHAR(128) NULL,
    ordering_provider_state VARCHAR(128) NULL,
    ordering_provider_postal_code VARCHAR(32) NULL,
    -- deidentified patient data
    patient_state VARCHAR(128) NULL,
    patient_county VARCHAR(128) NULL,
    patient_postal_code VARCHAR(32) NULL,
    patient_age VARCHAR(64) NULL,
    patient_gender VARCHAR(32) NULL,
    patient_gender_code VARCHAR(32) NULL,
    patient_ethnicity VARCHAR(64) NULL,
    patient_ethnicity_code VARCHAR(32) NULL,
    patient_race VARCHAR(64) NULL,
    patient_race_code VARCHAR(32) NULL,
    patient_tribal_citizenship VARCHAR(128) NULL,
    patient_tribal_citizenship_code VARCHAR(32) NULL,
    specimen_collection_date_time DATE NULL,
    -- testing lab information
    testing_lab_city VARCHAR(128) NULL,
    testing_lab_county VARCHAR(128) NULL,
    testing_lab_postal_code VARCHAR(32) NULL,
    testing_lab_state VARCHAR(128) NULL,
    testing_lab_name VARCHAR(512) NULL,
    testing_lab_clia VARCHAR(64) NULL,
    -- the record creation date
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE covid_result_metadata IS 'A collection of deidentified data from tests to allow us to pull metrics';
CREATE INDEX covid_result_message_idx ON covid_result_metadata(message_id);
CREATE INDEX covid_result_item_idx ON covid_result_metadata(report_id, report_index);