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
 * Add organization name to the table. This relates to the organization name that comes from SimpleReport
 * and is usually the school district, but it's good to track this as well
 */
ALTER TABLE covid_result_metadata
    ADD organization_name VARCHAR(512) NULL
;

/*
 Add an index on the organization name to speed searches on this field
 */
CREATE INDEX idx_crm_organization_name ON covid_result_metadata(organization_name);
