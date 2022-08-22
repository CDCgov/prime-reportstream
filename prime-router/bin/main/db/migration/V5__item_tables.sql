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
 * This SQL creates tables to track fine-grained item-level lineage
 */

-- This table allows many:many mappings between items.
-- As long as we're not tracking any detailed data about an item,
-- we are attempting to get away without an Item table.
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

-- Retrieve item_lineage_id that descended from a single report.
CREATE OR REPLACE FUNCTION item_descendants(start_report_id UUID)
RETURNS SETOF BIGINT
AS $$
DECLARE
BEGIN
     RETURN QUERY   
     WITH RECURSIVE tmp AS (
          SELECT item_lineage_id as tmp_lineage_id, parent_report_id as tmp_report_id, parent_index as tmp_index
          FROM item_lineage WHERE parent_report_id = start_report_id   
       UNION ALL   
          SELECT IL.item_lineage_id, IL.child_report_id, IL.child_index
          FROM item_lineage AS IL   
          JOIN tmp ON (tmp.tmp_report_id = IL.parent_report_id
          AND tmp.tmp_index = IL.parent_index)
     )
     SELECT tmp_lineage_id FROM tmp ORDER BY tmp_lineage_id;
END; 
$$  LANGUAGE PLPGSQL;    

