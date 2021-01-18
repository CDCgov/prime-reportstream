/*
The Flyway tool applies this migration to create the database.

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration

*/
/* 
   This SQL creates tables to track lineage. 
*/


/*
A task tracks a reports progress though the work pipeline. It also contains
metadata about the body of the report. Each report in the system has a task.
*/

/*
   Add a new action 'receive', and fix the current task table to use it.
 */
ALTER TABLE task ALTER COLUMN next_action TYPE VARCHAR(255);
DROP TYPE IF EXISTS task_action;
CREATE TYPE task_action AS ENUM ('receive', 'translate', 'batch', 'send', 'wipe', 'batch_error', 'send_error', 'wipe_error', 'none');
ALTER TABLE task ALTER COLUMN next_action TYPE task_action USING next_action::task_action;

-- Each row is an action already taken.
CREATE TABLE action (
    action_id SERIAL PRIMARY KEY,
    action_name TASK_ACTION,
    action_result JSONB,
    next_action TASK_ACTION,   -- what if there are multiple next actions?
    next_action_at TIMESTAMP WITH TIME ZONE,
    -- Every table must have created_at timestamp
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX action_next_action_idx ON action(next_action);

-- Each row is a report, created by some action
CREATE TABLE report_file (
    report_id UUID PRIMARY KEY,
    action_id INT NOT NULL REFERENCES action(action_id) ON DELETE CASCADE,

    -- These are non-null only for 'receive' actions
    sending_org VARCHAR(63),        -- should be a ref to an org table someday
    sending_params JSONB,           -- incoming API params (except secrets)
    sending_result_returned JSONB,  -- what we sent back to the sender
    sending_org_client VARCHAR(63),

    -- These are non-null only for 'send' actions:
    receiving_org VARCHAR(63),      -- should be a ref to an org table someday
    receiving_org_svc VARCHAR(63),  -- OrganizationService

    schema_name VARCHAR(63) NOT NULL,   -- should be a fk someday
    schema_topic VARCHAR(63) NOT NULL,
    body_url VARCHAR(2048),
    body_format VARCHAR(63) NOT NULL,
    item_count INT NOT NULL,
    wiped_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
