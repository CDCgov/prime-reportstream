/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration
*/

/*
Settings are kept a table where the actual settings values are in a JSON field. Receivers and Senders
have a parent organizationId. Organizations do not have a organizationId.
Each setting is versioned to keep around old values for lineage auditing. Deleted settings
are tomb-stoned, a row is never deleted
*/
CREATE TYPE SETTING_TYPE AS ENUM ('ORGANIZATION', 'RECEIVER', 'SENDER');
CREATE TABLE setting (
    -- Key
    setting_id SERIAL PRIMARY KEY,
    type SETTING_TYPE NOT NULL,
    name VARCHAR(63) NOT NULL,
    organization_id INT NULL REFERENCES setting(setting_id),

    -- Value
    values JSON,
    is_deleted BOOLEAN,
    is_active BOOLEAN,

    -- Metadata
    version INT NOT NULL,
    created_by VARCHAR(63) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (type, organization_id, name, version)
);

/* Partial indexes to optimize for active settings */
CREATE INDEX ON setting(organization_id) WHERE is_active = true;
CREATE UNIQUE INDEX ON setting(type, organization_id, name) WHERE is_active = true;





