/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database
Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema
Copy a version of this comment into the next migration
*/


/*
 * This SQL adds 'site_of_care' to the covid_result_metadata table
 */

ALTER TABLE covid_result_metadata ADD COLUMN site_of_care VARCHAR(128);
