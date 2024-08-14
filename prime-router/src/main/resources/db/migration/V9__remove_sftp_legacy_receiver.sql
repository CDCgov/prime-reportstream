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
 * Remove the SFTP_LEGACY organization for IGNORE
 */
WITH ignore_org AS (
    SELECT setting_id AS org_id FROM setting WHERE is_active = true AND type = 'ORGANIZATION' AND name = 'ignore'
)
UPDATE
    setting
SET
    is_active = false,
    is_deleted = true
FROM
    ignore_org
WHERE
        organization_id = ignore_org.org_id
  AND name = 'SFTP_LEGACY'
  AND is_active = true
;