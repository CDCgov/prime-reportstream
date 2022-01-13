/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 * use VARCHAR(63) for names in organization and schema
 *
 * Copy a version of this comment into the next migration
 *
 */

CREATE TYPE action_event_type AS ENUM ('info', 'warning', 'error', 'filter');

CREATE TYPE action_event_scope AS ENUM ('parameter', 'report', 'item', 'translation');

CREATE TABLE action_event
(
  action_event_id BIGSERIAL PRIMARY KEY,
  action_id INTEGER NOT NULL REFERENCES action(action_id) ON DELETE CASCADE,
  report_id UUID REFERENCES report_file(report_id) ON DELETE CASCADE,
  index INTEGER,
  type action_event_type,
  scope action_event_scope,
  detail JSONB,
  created_at timestamp NOT NULL
);

CREATE INDEX action_event_created_at_idx on action_event(created_at)