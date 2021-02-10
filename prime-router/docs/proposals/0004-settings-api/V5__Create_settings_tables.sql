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
CREATE TYPE SETTINGS_ENTITY AS ENUM ('organization', 'receiver', 'sender');
CREATE TABLE settings (
    -- Key
    entity SETTINGS_ENTITY NOT NULL,
    organization_name VARCHAR(63) NOT NULL,
    setting_name VARCHAR(63),
    PRIMARY KEY (organization_name, entity, setting_name),

    -- Value
    values JSON,

    -- Metadata
    version INT,
    created_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(63)
);

CREATE TABLE settings_history (
    -- Key
    entity SETTINGS_ENTITY NOT NULL,
    organization_name VARCHAR(63) NOT NULL,
    setting_name VARCHAR(63),
    version INT,
    PRIMARY KEY (organization_name, entity, setting_name, version),

    -- Value
    values JSON,

    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(63)
);