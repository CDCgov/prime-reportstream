/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration
*/

/*
A setting is used configure how processing of report
metadata about the body of the report. Each report in the system has a task.
*/
CREATE TABLE setting (
    --
    setting_name VARCHAR(63) NOT NULL,
    version INT,
    PRIMARY KEY (setting_name, version),

    --
    is_active BOOLEAN NOT NULL,
    details JSON,

    -- Audit information
    created_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(63)
);
CREATE INDEX setting_is_active ON setting(is_active);