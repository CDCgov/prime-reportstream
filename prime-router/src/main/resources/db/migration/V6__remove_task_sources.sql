/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 * use VARCHAR(63) for names in organization and schema
 *
 * Copy a version of this comment into the next migration
 *
 */

/* 
 * This SQL removes the TASK_SOURCE table
 */

DROP INDEX task_source_from_report_id_idx;
DROP INDEX task_source_report_id_idx;
DROP TABLE task_source;

/* TASK_SOURCE WAS:
    source_id SERIAL PRIMARY KEY,
    report_id UUID REFERENCES task(report_id) ON DELETE CASCADE,
    from_report_id UUID,
    from_report_action VARCHAR(63),
    from_sender VARCHAR(63),
    from_sender_organization VARCHAR(63)
*/

/** WHAT TO DO WITH THIS TABLE
CREATE TABLE task (
    -- one task for each report
    report_id UUID PRIMARY KEY,

    -- pipeline state
    next_action TASK_ACTION,
    next_action_at TIMESTAMP WITH TIME ZONE,

    -- ReportHeader information (these should not change)
REMOVE    schema_name VARCHAR(63) NOT NULL,
REMOVE    receiver_name VARCHAR(63),
REMOVE    item_count INT,
REMOVE    body_format VARCHAR(63),
REMOVE    body_url VARCHAR(2048),

    -- Audit information
    created_at TIMESTAMP WITH TIME ZONE,
    translated_at TIMESTAMP WITH TIME ZONE,
    batched_at TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE,
REMOVE?    wiped_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX task_next_action_idx ON task(next_action);

**/


