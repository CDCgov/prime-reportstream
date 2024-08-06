/*
* The Flyway tool applies this migration to create the database.
*
* Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
* use VARCHAR(63) for names in organization and schema
*
* Copy a version of this comment into the next migration
*
*/

-- Alter the table to make schema_name and schema_topic nullable
ALTER TABLE public.report_file
    ALTER COLUMN schema_name DROP NOT NULL,
    ALTER COLUMN schema_topic DROP NOT NULL;