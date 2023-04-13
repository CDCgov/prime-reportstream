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
 * Add a space to store the submitted report id on any values in the item lineage
 * to remove the need to go up the tree to find relevant metadata for deliveries
 * see https://github.com/CDCgov/prime-reportstream/issues/8985
 */
ALTER TABLE item_lineage ADD input_report_id UUID;
