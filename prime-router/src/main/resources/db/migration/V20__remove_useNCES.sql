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
 * remove all instances of 'useNCESFacilityName' from any setting where it exists
 */
UPDATE setting
SET values = (
    SELECT values #- '{translation,useNCESFacilityName}'
    FROM setting s2
    WHERE s2.setting_id = setting.setting_id
    )
WHERE
    is_active = true AND
    values -> 'translation' ->> 'useNCESFacilityName' is not null;
