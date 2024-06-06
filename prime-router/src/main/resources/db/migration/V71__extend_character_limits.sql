/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 *
 * Copy a version of this comment into the next migration
 *
 */

/*
 * Extend length of character limited fields that are in danger of being exceeded in production.
 */

ALTER TABLE action
ALTER COLUMN action_params TYPE VARCHAR,
ALTER COLUMN sender_ip TYPE VARCHAR,
ALTER COLUMN sending_org TYPE VARCHAR,
ALTER COLUMN sending_org_client TYPE VARCHAR,
ALTER COLUMN receiving_org TYPE VARCHAR,
ALTER COLUMN receiving_org_svc TYPE VARCHAR;

ALTER TABLE action_log
ALTER COLUMN tracking_id TYPE VARCHAR;

ALTER TABLE email_schedule
ALTER COLUMN created_by TYPE VARCHAR;

ALTER TABLE item_lineage
ALTER COLUMN tracking_id TYPE VARCHAR,
ALTER COLUMN transport_result TYPE VARCHAR;

ALTER TABLE jti_cache
ALTER COLUMN jti TYPE VARCHAR;

ALTER TABLE lookup_table_version
ALTER COLUMN table_name TYPE VARCHAR,
ALTER COLUMN created_by TYPE VARCHAR;

ALTER TABLE report_file
ALTER COLUMN sending_org TYPE VARCHAR,
ALTER COLUMN sending_org_client TYPE VARCHAR,
ALTER COLUMN receiving_org TYPE VARCHAR,
ALTER COLUMN receiving_org_svc TYPE VARCHAR,
ALTER COLUMN transport_params TYPE VARCHAR,
ALTER COLUMN schema_topic TYPE VARCHAR,
ALTER COLUMN body_url TYPE VARCHAR,
ALTER COLUMN external_name TYPE VARCHAR,
ALTER COLUMN body_format TYPE VARCHAR,
ALTER COLUMN downloaded_by TYPE VARCHAR;

ALTER TABLE setting
ALTER COLUMN name TYPE VARCHAR,
ALTER COLUMN created_by TYPE VARCHAR;

ALTER TABLE task
ALTER COLUMN schema_name TYPE VARCHAR,
ALTER COLUMN receiver_name TYPE VARCHAR,
ALTER COLUMN body_format TYPE VARCHAR,
ALTER COLUMN body_url TYPE VARCHAR;