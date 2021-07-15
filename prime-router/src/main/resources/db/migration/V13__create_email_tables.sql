/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration
*/

/*
Email schedules are kept a table where the actual schedule values are in a JSON field.
Each schedule is versioned to keep around old values for lineage auditing. Deleted schedules
are tomb-stoned, a row is never deleted
*/
CREATE TABLE email_schedule (
    -- Key
    email_schedule_id SERIAL PRIMARY KEY,

    -- Value
    values JSON,
    is_deleted BOOLEAN,
    is_active BOOLEAN,

    -- Metadata
    version INT NOT NULL,
    created_by VARCHAR(63) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    constraint "CHK_email_schedule.version" check ("version" > 0),
    constraint "CHK_email_schedule.created_by" check (length("created_by") > 0)
);
