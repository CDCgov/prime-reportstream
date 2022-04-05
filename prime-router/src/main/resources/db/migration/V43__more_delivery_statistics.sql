/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 *
 * Copy a version of this comment into the next migration
 */

/*
 * Adds a new column to the report_file table
 * To support a specific delivery statistics reporting requirement.
 * See Report.kt::itemCountPreQualityFilter for full documentation.
 * 
 * This will be set for reports created by the routing/filtering step.
 * This will be null for all prior and subsequent steps, because it makes no sense elsewhere.
 */
ALTER TABLE report_file ADD COLUMN item_count_pre_qualfilter INT  -- null is ok.


