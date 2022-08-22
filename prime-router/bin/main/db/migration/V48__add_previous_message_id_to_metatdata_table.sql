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
 * Add previous messages id to the table. In instances where a corrected message is sent, the message id of
 * original message is sent in this field and represents the accession number.
 */

ALTER TABLE covid_result_metadata
    ADD previous_message_id VARCHAR(512) NULL