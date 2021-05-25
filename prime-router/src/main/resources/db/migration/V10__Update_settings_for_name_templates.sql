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
 * Update the setting table, changing values to type jsonb instead of json
 */
ALTER TABLE setting
ALTER COLUMN values
SET DATA TYPE JSONB
USING values::JSONB;

/*
 *  Remove the useAphlNamingFormat key
 */
UPDATE
    setting s
SET
    values = CASE (s.values::JSONB#>'{translation,useAphlNamingFormat}')
                 WHEN 'true' THEN
                     JSONB_SET(
                             s.values::JSONB,
                             '{translation}',
                             JSONB_SET(
                                     ((s.values::JSONB -> 'translation') - 'useAphlNamingFormat'),
                                     '{nameFormat}',
                                     '"aphl"'
                                 )
                         )
                 WHEN 'false' THEN
                     JSONB_SET(
                             s.values::JSONB,
                             '{translation}',
                             JSONB_SET(
                                     ((s.values::JSONB -> 'translation') - 'useAphlNamingFormat'),
                                     '{nameFormat}',
                                     COALESCE((s.values::JSONB#>'{translation,nameFormat}'), '"standard"')
                                 )
                         )
                 ELSE s.values::JSONB
        END
WHERE
    s.type = 'RECEIVER'
    AND s.values::JSONB#>'{translation}' IS NOT NULL
    AND s.is_active = TRUE
;

UPDATE
    setting s
SET
    values = JSONB_SET(s.values::JSONB, '{translation,nameFormat}', '"standard"')
WHERE
    s.type = 'RECEIVER'
    AND s.values::JSONB#>'{translation,nameFormat}' IS NULL
    AND s.is_active = TRUE
;