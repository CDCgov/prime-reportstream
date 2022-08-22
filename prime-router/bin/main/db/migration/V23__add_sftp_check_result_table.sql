/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 * use VARCHAR(63) for names in organization and schema
 *
 * Copy a version of this comment into the next migration
 *
 */

CREATE TABLE receiver_connection_check_results
(
  receiver_connection_check_result_id BIGSERIAL PRIMARY KEY,
  organization_id INTEGER NOT NULL REFERENCES setting(setting_id) ON DELETE CASCADE,
  receiver_id INTEGER NOT NULL REFERENCES  setting(setting_id) ON DELETE CASCADE,
  connection_check_result TEXT NOT NULL,
  connection_check_successful BOOLEAN NOT NULL DEFAULT FALSE,
  connection_check_started_at TIMESTAMP WITH TIME ZONE NOT NULL,
  connection_check_completed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

COMMENT ON TABLE receiver_connection_check_results IS
    'Contains information about checks of connections to receiver remote servers';
COMMENT ON COLUMN receiver_connection_check_results.organization_id IS
    'The ID for an organization. This points to SETTING';
COMMENT ON COLUMN receiver_connection_check_results.receiver_id IS
    'The ID for a receiver. This points to SETTING';
COMMENT ON COLUMN receiver_connection_check_results.connection_check_result IS
    'The text result of the connection check. May contain the error details';
COMMENT ON COLUMN receiver_connection_check_results.connection_check_successful IS
    'A boolean indicating the test was successful or not';
COMMENT ON COLUMN receiver_connection_check_results.connection_check_started_at IS
    'The date time the connection check was initiated';
COMMENT ON COLUMN receiver_connection_check_results.connection_check_completed_at IS
    'The date time the connection check returned';