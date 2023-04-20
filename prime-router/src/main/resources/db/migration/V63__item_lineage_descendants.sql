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
 * are no item_ids because there is no ITEM table!
 *
 * Note also this effect of having no ITEM table: If a submitted Item
 * in a report immediately goes no-where (that is, it is `received` but
 * never makes it to any next step in the pipeline), it'll never appear
 * in the item_lineage table.  As if it didn't exist!
 *
 * (Further note: there is already an 'item_descendants' function, which is perhaps erroneously named, because
 *  it takes a report_id as its arg.)
 *
 */

/*  Example call:

 Find info about all descendant items from all items created during March 2023:
  
    select item_lineage_id as original_item_lineage_id,
           (item_lineage_descendants(item_lineage_id)).*
    from item_lineage IL
    where IL.created_at between symmetric '2023-03-01' AND '2023-04-01'
    group by original_item_lineage_id;

 The above (xxx(x)).* syntactic magic splits the function results out into normal columns.

 */

-- On the item_lineage table, find myself plus all my children, grandchildren, etc
DROP FUNCTION IF EXISTS item_lineage_descendants;
CREATE OR REPLACE FUNCTION item_lineage_descendants(start_item_lineage_id BIGINT)
RETURNS TABLE
     (
      item_lineage_id_retval bigint,
      parent_report_id_retval uuid,
      parent_index_retval integer,
      child_report_id_retval uuid,
      child_index_retval integer,
      tracking_id_retval varchar(128),
      created_at_retval timestamp with time zone
     )
AS $$
DECLARE
BEGIN
    RETURN QUERY
      WITH RECURSIVE tmp AS (
        SELECT IL1.* from item_lineage IL1 where IL1.item_lineage_id = start_item_lineage_id
       UNION ALL
        SELECT ILchild.*
        FROM item_lineage AS ILchild
	-- Confusing:  The parent/child relationship is on the report_id/index tuple,
	--             not on the item_lineage_id.  Hence the need for the triple join (ILparent, ILchild, tmp).
	--             This is yet another side effect of not having an ITEM table.
	--             Then, you could have parent_item_id and child_item_id columns - way less confusing.
        -- Note that this join is on the parent's child == the child's parent.  Makes sense, eh?
        JOIN item_lineage AS ILparent ON ILparent.child_report_id = ILchild.parent_report_id
                   and ILparent.child_index = ILchild.parent_index
	JOIN tmp ON ILparent.item_lineage_id = tmp.item_lineage_id
      )
SELECT item_lineage_id, parent_report_id, parent_index,
       child_report_id, child_index, tracking_id, created_at
       FROM tmp;
END;
$$  LANGUAGE PLPGSQL;

