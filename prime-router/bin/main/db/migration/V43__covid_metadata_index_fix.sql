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
 * This procedure is included in the migration scripts because it needs to be run, but should be run manually at
 * deployment time since it could have performance impact.
 */

--update covid_result_metadata set report_index = report_index + 1
--where report_id in (
--	select distinct report_id
--	from covid_result_metadata
--	where report_index = 0
--	and created_at > '2/22/2022 16:00:00'
--)