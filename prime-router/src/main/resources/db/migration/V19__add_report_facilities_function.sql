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
 *
 */
DROP FUNCTION IF EXISTS report_facilities;

CREATE FUNCTION report_facilities(report_id UUID)
RETURNS
    TABLE(
             testing_lab_clia VARCHAR(63),
             testing_lab_name VARCHAR(63),
             count_records BIGINT,
             positive BIGINT,
             testing_lab_city VARCHAR(128),
             testing_lab_state VARCHAR(63)
         )
AS $$
DECLARE
BEGIN
    RETURN QUERY
        WITH RECURSIVE tmp AS (
            SELECT parent_report_id, parent_index, tracking_id
            FROM item_lineage
            WHERE child_report_id = report_id
            UNION ALL
            SELECT il.parent_report_id, il.parent_index, tmp.tracking_id
            FROM item_lineage il JOIN tmp ON (
                        tmp.parent_report_id = il.child_report_id
                    AND tmp.parent_index = il.child_index
                )
        )
        SELECT
            a.testing_lab_clia,
            a.testing_lab_name,
            count(a.covid_results_metadata_id) AS COUNT_RECORDS,
            sum(CASE WHEN a.test_result = 'DETECTED' THEN 1 ELSE 0 END) AS POSITIVE,
            a.testing_lab_city,
            a.testing_lab_state
        FROM
            tmp t
                JOIN covid_result_metadata a
                     ON t.parent_report_id = a.report_id
                            AND t.parent_index = a.report_index
        GROUP BY
            a.testing_lab_name,
            a.testing_lab_clia,
            a.testing_lab_city,
            a.testing_lab_state
    ;
END;
$$ LANGUAGE plpgsql;