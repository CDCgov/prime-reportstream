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
 * Add several new columns to the action table to track received reports
 */
ALTER TABLE action
    ADD COLUMN action_response JSONB NULL,
    ADD COLUMN http_status INT NULL,
    ADD COLUMN content_length INT NULL,
    ADD COLUMN sender_ip VARCHAR(39) NULL,
    ADD COLUMN sending_org VARCHAR(63) NULL,
    ADD COLUMN sending_org_client VARCHAR(63) NULL
;
