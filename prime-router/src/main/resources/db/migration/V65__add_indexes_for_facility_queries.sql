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
 * Add indexes to columns that are sorted and filtered on the Facility queries
 * it primarily covers columns in the COVID Result Metadata table
 * refer to this when adding Facility queries for the Universal Pipeline
 */
CREATE INDEX IF NOT EXISTS item_lineage_created_at
    ON item_lineage USING btree
    (created_at ASC NULLS LAST);

CREATE INDEX covid_result_metadata_ordering_provider_name
    ON covid_result_metadata USING btree
    (ordering_provider_name ASC NULLS LAST);

CREATE INDEX covid_result_metadata_testing_lab_name
    ON covid_result_metadata USING btree
    (testing_lab_name ASC NULLS LAST);

CREATE INDEX covid_result_metadata_sender_id
    ON covid_result_metadata USING btree
    (sender_id ASC NULLS LAST);

CREATE INDEX covid_result_metadata_report_id
    ON public.covid_result_metadata USING btree
    (report_id ASC NULLS LAST);