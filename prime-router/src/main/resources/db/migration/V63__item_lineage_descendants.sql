/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration
*/

/* 
 * This SQL creates an item_lineage_descendants stored function.
 *
 * I have purposely called this 'item_lineage_descendants' and not 'item_descendants'
 * because of this tech debt:
 *  this function takes an item_lineage_id as its parameter, not an item_id, because as of this writing, there
 * are no item_ids because there is no item table!
 *
 * (also, there is already an 'item_descendants' function, which is perhaps erroneously named, because
 *  it takes a report_id as its arg.)
 *
 */

-- On the item_lineage table, find myself plus all my children, grandchildren, etc
CREATE OR REPLACE FUNCTION item_lineage_descendants(start_item_lineage_id BIGINT)
RETURNS SETOF BIGINT
AS $$
DECLARE
BEGIN
    RETURN QUERY
      WITH RECURSIVE tmp AS (
        SELECT start_item_lineage_id AS tmp_item_lineage_id
       UNION ALL
        SELECT ILchild.item_lineage_id
        FROM item_lineage AS ILchild
	-- Confusing:  The parent/child relationship is on the report_id/index tuple,
	--             not on the item_lineage_id.  Hence the need for the triple join (ILparent, ILchild, tmp).
	--             This is yet another side effect of not having an ITEM table.
	--             Then, you could have parent_item_id and child_item_id columns - way less confusing.
        -- Also Confusing:  This join is on the parent's child == the child's parent. 
        JOIN item_lineage AS ILparent ON ILparent.child_report_id = ILchild.parent_report_id
                   and ILparent.child_index = ILchild.parent_index
	JOIN tmp ON ILparent.item_lineage_id = tmp.tmp_item_lineage_id
      )
SELECT tmp_item_lineage_id FROM tmp;
END;
$$  LANGUAGE PLPGSQL;

