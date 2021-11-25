/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 *
 * Copy a version of this comment into the next migration
 */

/*
 * Adds two new columns to the action table. 
 * Primary use case for the optional 'external_name' is to track a sender-assigned string name attached to the payload sent.  Often a filename. 
 * Its called external_name to match the name used in the REPORT_FILE table, representing names for files outside ReportStream.
 * Tracked redundantly here, because actions might fail, resulting in no REPORT_FILE entry, but user still wants to track by their payload name.
 * The 'userid' should bre populated for actions done by a specific user.  Leave null for server-to-server automated actions.
 */
ALTER TABLE action
ADD COLUMN external_name varchar(2048),  -- payloadName parameter, or any other externally visible filename created or downloaded by this action.
ADD COLUMN username varchar(63)
;

CREATE INDEX action_external_name_idx ON action(external_name);

