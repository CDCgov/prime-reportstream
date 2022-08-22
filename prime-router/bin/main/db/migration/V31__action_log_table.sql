/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 * use VARCHAR(63) for names in organization and schema
 *
 * Copy a version of this comment into the next migration
 *
 */

CREATE TYPE action_log_type AS ENUM ('info', 'warning', 'error', 'filter');

CREATE TYPE action_log_scope AS ENUM ('parameter', 'report', 'item', 'translation');

CREATE TABLE action_log
(
  action_log_id BIGSERIAL PRIMARY KEY,
  action_id BIGINT NOT NULL REFERENCES action(action_id) ON DELETE CASCADE,
  report_id UUID REFERENCES report_file(report_id) ON DELETE CASCADE,
  index INTEGER,
  tracking_id VARCHAR(128), -- value of the trackingElement of the child
  type action_log_type,
  scope action_log_scope,
  detail JSONB,
  created_at timestamp with time zone NOT NULL
);

CREATE INDEX action_log_created_at_idx on action_log(created_at);
CREATE INDEX action_log_tracking_id_idx on action_log(tracking_id);