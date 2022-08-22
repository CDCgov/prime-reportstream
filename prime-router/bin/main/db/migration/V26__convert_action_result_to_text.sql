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
 * Changing action_result to TEXT so we can store more information
 * varchar(2048) just isn't enough
 */
ALTER TABLE action
    ALTER COLUMN action_result TYPE TEXT
;
