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
 * PROBLEM QUERY:
 *
 * select
 * a.testing_lab_clia,
 * a.testing_lab_name,
 * count(a.covid_results_metadata_id) as COUNT_RECORDS,
 * count(a.test_result) filter (where a.test_result like 'Detected') as POSITIVE,
 * a.testing_lab_city, a.testing_lab_state
 * from covid_result_metadata a
 * join report_ancestors(cast('fc218c2a-e7ef-4226-adfc-940058005a94' as uuid))
 *     on report_ancestors.report_ancestors = a.report_id
 * group by a.testing_lab_name,
 * a.testing_lab_clia,
 * a.testing_lab_city,
 * a.testing_lab_state
*/

-- Handle the WHERE clause
CREATE INDEX CONCURRENTLY idx_report_lineage_parent_report_id
    ON report_lineage(parent_report_id);
CREATE INDEX CONCURRENTLY idx_report_lineage_child_report_id
    ON report_lineage(child_report_id);
CREATE INDEX CONCURRENTLY idx_covid_result_metadata_report_id
    ON covid_result_metadata(report_id);

-- Handle the GROUP BY
CREATE INDEX CONCURRENTLY idx_covid_result_metadata_get_reports_group_by
    ON covid_result_metadata(
        testing_lab_name,
        testing_lab_clia,
        testing_lab_city,
        testing_lab_state
    );