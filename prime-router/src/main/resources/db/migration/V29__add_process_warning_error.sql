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
 * Add process_warning enum type to underlying custom data type
 */
ALTER TYPE public.task_action ADD VALUE 'process_warning' after 'process';

/*
 * Add process_warning enum type to underlying custom data type
 */
ALTER TYPE public.task_action ADD VALUE 'process_error' after 'process_warning';