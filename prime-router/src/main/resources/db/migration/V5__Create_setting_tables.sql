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
CREATE TYPE SETTING_TYPE AS ENUM ('organization', 'receiver', 'sender');
CREATE TABLE setting (
    -- Key
    type SETTING_TYPE NOT NULL,
    organization_name VARCHAR(63) NOT NULL,
    setting_name VARCHAR(63),
    PRIMARY KEY (organization_name, type, setting_name),

    -- Value
    values JSON,

    -- Metadata
    version INT,
    created_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(63)
);

CREATE TABLE setting_history (
    -- Key
    type SETTING_TYPE NOT NULL,
    organization_name VARCHAR(63) NOT NULL,
    setting_name VARCHAR(63),
    version INT,
    PRIMARY KEY (organization_name, type, setting_name, version),

    -- Value
    is_deleted BOOLEAN,
    values JSON,

    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(63)
);