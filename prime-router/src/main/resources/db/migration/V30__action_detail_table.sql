/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 * use VARCHAR(63) for names in organization and schema
 *
 * Copy a version of this comment into the next migration
 *
 */

CREATE TYPE action_detail_type AS ENUM ('info', 'warning', 'error', 'filter');

CREATE TYPE action_detail_scope AS ENUM ('parameter', 'report', 'item', 'translation');

CREATE TABLE action_detail
(
  id BIGSERIAL PRIMARY KEY,
  action_id INTEGER NOT NULL REFERENCES action(action_id) ON DELETE CASCADE,
  response_message TEXT,
  row INTEGER,
  report_id UUID REFERENCES report_file(report_id) ON DELETE CASCADE,
  type action_detail_type,
  scope action_detail_scope,
  context JSONB,
  time timestamp NOT NULL
);