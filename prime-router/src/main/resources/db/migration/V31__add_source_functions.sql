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
    #variable_conflict use_variable
DECLARE
    receiver_report_item_count integer;
BEGIN
    receiver_report_item_count := (SELECT item_count FROM report_file WHERE report_file.report_id = report_id);

    RETURN QUERY WITH RECURSIVE
    -- All item_lineage rows that apply
    r AS (
        SELECT
            1 as gen,
            parent_report_id,
            parent_index,
            child_report_id as source_report_id,
            child_index as source_index
        FROM item_lineage
        WHERE
            child_report_id = report_id AND
            child_index in (
                SELECT *
                FROM generate_series(
                    report_index,
                    -- Don't select beyond the last item of the receiver report
                    greatest(report_index + max_rows - 1, receiver_report_item_count - 1)
                )
            )

        UNION ALL

        SELECT
            r.gen + 1,
            item_lineage.parent_report_id,
            item_lineage.parent_index,
            r.source_report_id,
            r.source_index
        FROM item_lineage, r
        WHERE
            item_lineage.child_report_id = r.parent_report_id AND
            item_lineage.child_index = r.parent_index
    ),
    -- The sender items (in other words, the rows belonging to the leaf ancestors)
    sender_items AS (
        SELECT max(gen) as last_gen, source_index FROM r GROUP BY r.source_index
    )
    SELECT
        r.parent_report_id,
        r.parent_index,
        r.source_report_id,
        r.source_index
    FROM r INNER JOIN sender_items
        ON r.gen = sender_items.last_gen AND
           r.source_index = sender_items.source_index
    ORDER BY r.source_index;
END;
$$;
