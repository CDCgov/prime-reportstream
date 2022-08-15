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
 * Adds 'processed_at' to the task table. This is not strictly needed at this time, but removing all the
 *  finished action columns is not part of the scope of the 'process async' task
 */
 ALTER TABLE TASK ADD routed_at timestamp with time zone
;