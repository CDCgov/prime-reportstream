/*
* The Flyway tool applies this migration to create the database.
*
* Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
* use VARCHAR(63) for names in organization and schema
*
* Copy a version of this comment into the next migration
*
*/

CREATE INDEX CONCURRENTLY IF NOT EXISTS report_file_receiving_org_svc_receiving_org_index
    on report_file (receiving_org_svc,receiving_org);