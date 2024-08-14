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
  Add new actions 'receive' and 'download', and fix the current task table to use it.
 */
ALTER TABLE task ALTER COLUMN next_action TYPE VARCHAR(255);
DROP TYPE IF EXISTS task_action;
CREATE TYPE task_action AS ENUM ('receive', 'translate', 'batch', 'send', 'download', 'wipe', 'batch_error', 'send_error', 'wipe_error', 'none');
ALTER TABLE task ALTER COLUMN next_action TYPE task_action USING next_action::task_action;

-- Each row is an action already taken.
CREATE TABLE action (
    action_id BIGSERIAL PRIMARY KEY,
    action_name TASK_ACTION,
    action_params VARCHAR(512),
    action_result VARCHAR(2048),
    -- Every table must have created_at timestamp
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Each row is a report, representing actual data, either stored internally 
-- or a file sent to an external receiver, per transport mechanism.
-- A report_file is always created by an action.
CREATE TABLE report_file (
    report_id UUID PRIMARY KEY,
    action_id BIGINT NOT NULL REFERENCES action(action_id) ON DELETE CASCADE,

    next_action TASK_ACTION,   -- what if there are multiple next actions?
    next_action_at TIMESTAMP WITH TIME ZONE,

    -- These are non-null only for 'receive' actions
    sending_org VARCHAR(63),        -- should be a ref to an org table someday
    sending_org_client VARCHAR(63), -- OrganizationClient

    -- These are non-null, once the report has been created for a specific receiver.
    receiving_org VARCHAR(63),      -- should be a ref to an org table someday
    receiving_org_svc VARCHAR(63),  -- OrganizationService
    -- These are the params used to send, and results of a send.
    -- Needed here because one action can send one file to multiple transports
    transport_params VARCHAR(512),
    transport_result VARCHAR(512),
    
    schema_name VARCHAR(63) NOT NULL,   -- should be a fk someday
    schema_topic VARCHAR(63) NOT NULL,
    body_url VARCHAR(2048) UNIQUE,
    external_name VARCHAR(2048) UNIQUE,  -- for report sent to external receiver. Filename, Redox ID etc
    body_format VARCHAR(63) NOT NULL,
    blob_digest bytea,
    item_count INT NOT NULL,
    wiped_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX report_file_next_action_idx ON report_file(next_action);


-- Each row represents a state transition in the data, caused by an action.
CREATE TABLE report_lineage (
    report_lineage_id BIGSERIAL PRIMARY KEY,
    action_id BIGINT NOT NULL REFERENCES action(action_id) ON DELETE CASCADE,
    parent_report_id UUID NOT NULL REFERENCES report_file(report_id) ON DELETE CASCADE,
    child_report_id UUID NOT NULL REFERENCES report_file(report_id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);


