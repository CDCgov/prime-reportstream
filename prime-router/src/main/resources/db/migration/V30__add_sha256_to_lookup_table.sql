/*
 * The Flyway tool applies this migration to create the database.
 *
 * Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
 *
 * Copy a version of this comment into the next migration
 */

/*
 * Adds new columns to the lookup_table_version table.
 *
 * 1) 'table_Sha256' stores the SHA-256 checksum value of the lookup table.  It is the data integrity and uses to
 * check for the up-to-date.
 *
 */
ALTER TABLE lookup_table_version
ADD COLUMN table_sha256_checksum character varying(512) NOT NULL DEFAULT '0'
;
