/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 * use VARCHAR(63) for names in organization and schema
 *
 * Copy a version of this comment into the next migration
 *
 */

/*
 * Add several new columns to the covid_result_metadata table to track sender ID and testkit
 */
ALTER TABLE covid_result_metadata
    ADD COLUMN sender_id VARCHAR(512) NULL,
    ADD COLUMN test_kit_name_id VARCHAR(512) NULL,
    ADD COLUMN test_performed_loinc_code VARCHAR(512) NULL
;
