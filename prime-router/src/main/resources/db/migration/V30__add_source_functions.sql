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
    Find the ancestor items for an receiver's item by using an item's lineage.
    The item is specified by a receiver_report_id and a receiver_report_index.
    The function returns a table containing the a report_id and the index for every ancestor item.
*/
CREATE OR REPLACE FUNCTION item_ancestors(receiver_report_id uuid, receiver_report_index INT)
    RETURNS
        TABLE(
             generation INT,
             report_id UUID,
             index INT
        )
    STABLE
    LANGUAGE plpgsql
AS $$
DECLARE
BEGIN
    RETURN QUERY
        WITH RECURSIVE r AS (
            SELECT
                1 as gen,
                parent_report_id,
                parent_index
            FROM item_lineage
            WHERE
                child_report_id = receiver_report_id AND
                child_index = receiver_report_index

            UNION ALL

            SELECT
                r.gen + 1,
                item_lineage.parent_report_id,
                item_lineage.parent_index
            FROM item_lineage, r
            WHERE
                r.parent_report_id = item_lineage.child_report_id AND
                r.parent_index = item_lineage.child_index
        )
        SELECT
           gen as generation,
           parent_report_id as report_id,
           parent_index as index
        FROM r
        ORDER BY gen;
END;
$$;


/*
    Find the sender report items for a set of receiver report items by walking through each item's lineage to its source report.
    The set of report items is specified by a receiver report's id and offset and max_rows.
    The number of rows returned depends on the number of actual items in the output report after the offset and the max_rows parameter.
 */
CREATE OR REPLACE FUNCTION sender_items(report_id UUID, report_index INT, max_rows INT)
    RETURNS
        TABLE
        (
            sender_report_id UUID,
            sender_report_index INT,
            receiver_report_id UUID,
            receiver_report_index INT
        )
    LANGUAGE plpgsql
AS $$
DECLARE
BEGIN
    -- Build up a temp table of sender report and indices for every item in the range of receiver items.
    CREATE TEMPORARY TABLE sources_temp(
        sender_report_id UUID,
        sender_report_index INT,
        receiver_report_id UUID,
        receiver_report_index INT
    );
    FOR i IN report_index .. report_index + max_rows LOOP
        -- We can terminate the loop early if receiver index does not exist
        IF NOT EXISTS(SELECT item_lineage_id FROM item_lineage WHERE child_report_id = report_id AND child_index = i) THEN
            EXIT;
        END IF;

        WITH source AS (
            SELECT
                a.report_id as ancestor_report_id,
                a.index as ancesstor_index
            FROM item_ancestors(report_id, i) AS a
            ORDER BY a.generation DESC
            LIMIT 1
        )
        INSERT INTO sources_temp
            SELECT source.ancestor_report_id, source.ancesstor_index, report_id, i FROM source;
    END LOOP;
    RETURN QUERY SELECT * FROM sources_temp;
END;
$$;
