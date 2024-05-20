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
 * Adds necessary values for split routing steps (destination and receiver filters)
 *  - timestamps for completion of destination and receiver filter functions to the task table.
 */
ALTER TABLE TASK ADD destination_filtered_at timestamp with time zone;
ALTER TABLE TASK ADD receiver_filtered_at timestamp with time zone;