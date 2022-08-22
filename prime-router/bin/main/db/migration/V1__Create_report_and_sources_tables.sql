/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration
*/

/*
A task tracks a reports progress though the work pipeline. It also contains
metadata about the body of the report. Each report in the system has a task.
*/
CREATE TYPE TASK_ACTION AS ENUM ('translate', 'batch', 'send', 'wipe', 'none');
CREATE TABLE task (
    -- one task for each report
    report_id UUID PRIMARY KEY,

    -- pipeline state
    next_action TASK_ACTION,
    next_action_at TIMESTAMP WITH TIME ZONE,

    -- ReportHeader information (these should not change)
    schema_name VARCHAR(63) NOT NULL,
    receiver_name VARCHAR(63),
    item_count INT,
    body_format VARCHAR(63),
    body_url VARCHAR(2048),

    -- Audit information
    created_at TIMESTAMP WITH TIME ZONE,
    translated_at TIMESTAMP WITH TIME ZONE,
    batched_at TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE,
    wiped_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX task_next_action_idx ON task(next_action);

/*
Sources tell what information and operations where used to create a report
It has many to one relationship to a task.
*/
CREATE TABLE task_source (
    source_id SERIAL PRIMARY KEY,
    report_id UUID REFERENCES task(report_id) ON DELETE CASCADE,
    from_report_id UUID,
    from_report_action VARCHAR(63),
    from_sender VARCHAR(63),
    from_sender_organization VARCHAR(63)
);
CREATE INDEX task_source_report_id_idx ON task_source(report_id);
CREATE INDEX task_source_from_report_id_idx ON task_source(from_report_id);
