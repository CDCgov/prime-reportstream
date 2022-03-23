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
 * Add fhir_processing support for tracking and logging.
 */
ALTER TYPE public.task_action ADD VALUE 'fhir_processing';
ALTER TYPE public.action_log_scope ADD VALUE 'fhir_processing';