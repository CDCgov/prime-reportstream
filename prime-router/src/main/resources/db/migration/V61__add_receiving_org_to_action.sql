/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration
*/


/*
 * Add several new columns to the action table to track received reports
 */
ALTER TABLE action
    ADD COLUMN receiving_org VARCHAR(63) NULL,
    ADD COLUMN receiving_org_svc VARCHAR(63) NULL
;
