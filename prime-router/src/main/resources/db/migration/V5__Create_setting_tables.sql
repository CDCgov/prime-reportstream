/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration
*/

/*
Settings are kept in hybrid table where the actual settings values are in a JSON field.
Because settings are read many more times than they are written, we have a table of the
current settings. Because settings can affect what is produced by the system, we never
always have a version of the setting in the settings history table.
Because settings are small in number and in size, this should not be a size problem.
When an entity is deleted, a tombstone entry is added to the history table
*/
CREATE TYPE SETTING_TYPE AS ENUM ('ORGANIZATION', 'RECEIVER', 'SENDER');
CREATE TABLE setting (
    --
    setting_id SERIAL PRIMARY KEY,
    type SETTING_TYPE NOT NULL,
    organization_name VARCHAR(63) NOT NULL,
    setting_name VARCHAR(63) NOT NULL,

    -- Value
    values JSON,

    -- Metadata
    version INT NOT NULL,
    created_by VARCHAR(63) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX setting_name_index ON setting(type, organization_name, setting_name);

CREATE TABLE setting_history (
    LIKE setting INCLUDING ALL,
    is_deleted BOOLEAN
);
DROP INDEX IF EXISTS setting_history_type_organization_name_setting_name_idx;
CREATE UNIQUE INDEX setting_history_name_index ON setting_history(type, organization_name, setting_name, version);




