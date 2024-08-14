/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 * use VARCHAR(63) for names in organization and schema
 *
 * Copy a version of this comment into the next migration
 *
 */

CREATE INDEX IF NOT EXISTS idx_action_name_sending_org
  ON action
  (action_name, sending_org)
;

CREATE INDEX IF NOT EXISTS idx_action_created_and_sender
  ON action
  USING brin (created_at, sending_org)
;
