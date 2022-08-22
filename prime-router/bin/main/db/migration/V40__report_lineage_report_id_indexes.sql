/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 * use VARCHAR(63) for names in organization and schema
 *
 * Copy a version of this comment into the next migration
 *
 */

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_report_lineage_parent_report_id
  ON report_lineage
  USING btree(parent_report_id)
;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_report_lineage_child_report_id
  ON report_lineage
  USING btree(child_report_id)
;
