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
 * Adds index to the item_lineage table on created_at.
 */
CREATE INDEX IF NOT EXISTS idx_item_lineage_created_at
    ON item_lineage
    USING btree (created_at)
;