/*
The Flyway tool applies this migration to create the database.

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration

*/

/* 
 * This SQL creates tables to track fine-grained item-level lineage
 */


ALTER TABLE report_file DROP CONSTRAINT report_file_external_name_key;

-- Each row is an item that's part of a report.
-- item_index is where the item can be found in the report, eg, item 1, item 2, etc.
CREATE TABLE item (
    item_id BIGSERIAL PRIMARY KEY,
    item_index INTEGER NOT NULL,   -- First actual data item in report is 1.  Not zero-based.
    report_id UUID NOT NULL REFERENCES report_file(report_id) ON DELETE CASCADE,
    tracking_id VARCHAR(128) NOT NULL,
    transport_params VARCHAR(512),
    transport_result VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX item_index_report_idx ON item(item_index, report_id);
CREATE INDEX item_tracking_id_idx ON item(tracking_id);


-- Each row represents a state transition in the data, caused by an action.
CREATE TABLE item_lineage (
    item_lineage_id BIGSERIAL PRIMARY KEY,
    action_id BIGINT NOT NULL REFERENCES action(action_id) ON DELETE CASCADE,
    parent_item_id BIGINT NOT NULL REFERENCES item(item_id) ON DELETE CASCADE,
    child_item_id BIGINT NOT NULL REFERENCES item(item_id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);


