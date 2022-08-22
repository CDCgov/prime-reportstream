/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 *
 * Copy a version of this comment into the next migration
 */

/*
 * Adds two new columns to the action table.
 *
 * 1) 'external_name' stores the payloadName value submitted by the user.  Often a filename.
 * Tracked in ACTION table so we can show an external_name even for failed actions.
 * (Redundantly tracked in report_file, because it was there first)
 *
 * 2) The 'userid' should be populated for actions done by a specific
 * user.  Leave null for server-to-server automated actions.
 */
ALTER TABLE action
ADD COLUMN external_name text,  -- payloadName parameter, or any other externally visible filename created or downloaded by this action.
ADD COLUMN username text
;

-- We may eventually want to add an index on (sending_organization, external_name), 
-- however it would only be useful for exact string matches and startsWith() matches,
-- so we're leaving it out pending feedback from the frontend team.
