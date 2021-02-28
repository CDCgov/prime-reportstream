/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 * use VARCHAR(63) for names in organization and schema
 *
 * Copy a version of this comment into the next migration
 *
 */

--  This SQL removes the TASK_SOURCE table
DROP INDEX task_source_from_report_id_idx;
DROP INDEX task_source_report_id_idx;
DROP TABLE task_source;

--  Increase the size of this column.  Useful data was getting cut off.
ALTER TABLE action ALTER COLUMN action_params TYPE VARCHAR(2048);
