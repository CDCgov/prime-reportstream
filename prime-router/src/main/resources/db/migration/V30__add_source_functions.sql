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
        WITH RECURSIVE R AS (
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
                       R.gen + 1,
                       item_lineage.parent_report_id,
                       item_lineage.parent_index
                FROM item_lineage, R
                WHERE
                      R.parent_report_id = item_lineage.child_report_id AND
                      R.parent_index = item_lineage.child_index
            )
        SELECT
               gen as generation,
               parent_report_id as report_id,
               parent_index as index
        FROM R ORDER BY gen;
END;
$$;

/*
    Find the source item for an item in a receiver's report by walking through an item's lineage.
    The receivers item is specified by a report_id and a index.
    The function returns a table containing the source report_id and index.
    Since an item can only have one source item, the returned table can have most one row.
*/
CREATE OR REPLACE FUNCTION source_report(receiver_report_id uuid, receiver_report_index INT)
    RETURNS
        TABLE(report_id UUID, index INT)
    LANGUAGE plpgsql
    STABLE
AS $$
DECLARE
BEGIN
    RETURN QUERY
        WITH ancestors AS
             (SELECT
                  item_ancestors.generation,
                  item_ancestors.report_id,
                  item_ancestors.index
              FROM item_ancestors(receiver_report_id, receiver_report_index))
        SELECT ancestors.report_id, ancestors.index FROM ancestors ORDER BY ancestors.generation DESC LIMIT 1;
END;
$$;


/*
    Find the report file information for a set of report items by walking through an item's lineage.
    The set of report items is specified by a receiver report's id and offset and limit.
    The limit may be larger than the report's item_count.
    Similar to the source_report function, but for report_file information and multiple items.
 */
CREATE OR REPLACE FUNCTION source_report_files(output_report_id uuid, output_offset INT, output_limit INT)
    RETURNS
        TABLE(
             report_id UUID,
             index INT,
             action_id BIGINT,
             next_action task_action,
             next_action_at TIMESTAMP WITH TIME ZONE,
             sending_org VARCHAR(63),
             sending_org_client VARCHAR(63),
             schema_name VARCHAR(63),
             schema_topic VARCHAR(63),
             body_url VARCHAR(2048),
             body_format VARCHAR(63),
             blob_digest bytea,
             created_at TIMESTAMP WITH TIME ZONE,
             receiver_report_id UUID,
             receiver_report_index INT)
    LANGUAGE plpgsql
AS $$
DECLARE
BEGIN
    -- Build up a temp table of source item for every item in the range of receiver items.
    CREATE TEMPORARY TABLE sources(
        source_report_id UUID,
        source_index INT,
        output_id UUID,
        output_index INT
    );
    FOR i IN output_offset .. output_offset + output_limit LOOP
        -- We can terminate the loop early if receiver index does not exist
        IF NOT EXISTS(SELECT item_lineage_id FROM item_lineage WHERE child_report_id = output_report_id AND child_index = i) THEN
            EXIT;
        END IF;
        WITH source AS ((SELECT * FROM source_report(output_report_id, i)))
        INSERT INTO sources SELECT source.report_id, source.index, output_report_id, i FROM source;
    END LOOP;
    -- Join the sources table with the report file table
    RETURN QUERY
        SELECT
            rf.report_id,
            sources.source_index,
            rf.action_id,
            rf.next_action,
            rf.next_action_at,
            rf.sending_org,
            rf.sending_org_client,
            rf.schema_name,
            rf.schema_topic,
            rf.body_url,
            rf.body_format,
            rf.blob_digest,
            rf.created_at,
            sources.output_id,
            sources.output_index
        FROM report_file as rf INNER JOIN sources ON rf.report_id = sources.source_report_id;
END;
$$;
