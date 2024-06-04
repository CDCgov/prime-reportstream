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
 *  - new task_action values
 */
ALTER TYPE public.task_action ADD VALUE 'destination-filter' after 'process';
ALTER TYPE public.task_action ADD VALUE 'receiver-filter' after 'destination-filter';