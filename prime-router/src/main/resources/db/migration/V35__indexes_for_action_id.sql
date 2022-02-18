/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 * use VARCHAR(63) for names in organization and schema
 *
 * Copy a version of this comment into the next migration
 *
 */

CREATE INDEX IF NOT EXISTS idx_action_log_action
  ON action_log
  USING btree(action_id)
;

CREATE INDEX IF NOT EXISTS idx_report_file_action
  ON report_file
  USING btree(action_id)
;

CREATE INDEX IF NOT EXISTS idx_report_lineage_action
  ON report_lineage
  USING btree(action_id)
;
