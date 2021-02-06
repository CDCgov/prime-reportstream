/*
The Flyway tool applies this migration to create the database.

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration

*/

/* 
 * This SQL creates tables to track fine-grained item-level lineage
 */

/****
-- Each row is an item that's part of a report.
CREATE TABLE item (
    item_id BIGSERIAL PRIMARY KEY,
    item_index INTEGER NOT NULL,
    report_id UUID NOT NULL REFERENCES report_file(report_id) ON DELETE CASCADE,
    tracking_id VARCHAR(128) NOT NULL,
    transport_params VARCHAR(512),
    transport_result VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX item_index_report_idx ON item(item_index, report_id);
CREATE INDEX item_tracking_id_idx ON item(tracking_id);


-- Each row represents a state transition in the data, caused by an action.
CREATE TABLE item_lineage_old (
    item_lineage_id BIGSERIAL PRIMARY KEY,
    parent_item_id BIGINT NOT NULL REFERENCES item(item_id) ON DELETE CASCADE,
    child_item_id BIGINT NOT NULL REFERENCES item(item_id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
*/

-- This table allows many:many mappings between items.
-- Use case for 1:Many is one item sent to many receivers.  No known Many:1 item use cases as of this writing.
-- As long as we're not tracking any actual data about an item, we can get away without an Item table.
-- Note that (report_id, index) uniquely defines any one item/row in any one report.
CREATE TABLE item_lineage (
    item_lineage_id BIGSERIAL PRIMARY KEY,
    parent_report_id UUID NOT NULL REFERENCES report_file(report_id) ON DELETE CASCADE,
    parent_index INTEGER NOT NULL,
    child_report_id UUID NOT NULL REFERENCES report_file(report_id) ON DELETE CASCADE,
    child_index INTEGER NOT NULL,
    tracking_id VARCHAR(128), -- value of the trackingElement of the child
    transport_result VARCHAR(512),     -- results of transporting the child
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX item_lineage_parent_idx ON item_lineage(parent_report_id, parent_index);
CREATE INDEX item_lineage_child_idx ON item_lineage(child_report_id, child_index);
CREATE INDEX item_lineage_tracking_id_idx ON item_lineage(tracking_id);


