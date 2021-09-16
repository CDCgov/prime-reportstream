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
 * The vw_results_by_county_lat_long view was manually created in prod, so we need to drop it first.
 * Another PR will add the view back in the repo.
 */
DROP VIEW IF EXISTS public.vw_results_by_county_lat_long;

/*
 * Add several new columns to the covid_result_metadata table to track sender ID and testkit.
 */
ALTER TABLE covid_result_metadata
    ADD COLUMN sender_id VARCHAR(512) NULL,
    ADD COLUMN test_kit_name_id VARCHAR(512) NULL,
    ADD COLUMN test_performed_loinc_code VARCHAR(512) NULL
;

/*
 * Now back fill the sender_id using data from the report_file table.
 */
UPDATE covid_result_metadata
SET sender_id = subquery.sender_id
FROM (SELECT rf.report_id, rf.sending_org || '.' || rf.sending_org_client as sender_id
FROM report_file rf
JOIN covid_result_metadata crm ON rf.report_id = crm.report_id) AS subquery
WHERE covid_result_metadata.sender_id IS NULL AND covid_result_metadata.report_id = subquery.report_id;