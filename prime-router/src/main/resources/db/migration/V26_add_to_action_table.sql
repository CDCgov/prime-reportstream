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
 * The 'userid' should bre populated for actions done by a specific user.  Leave null for server-to-server automated actions.
 */
ALTER TABLE action
ADD COLUMN external_name varchar(2048),
ADD COLUMN username varchar(63)
;

