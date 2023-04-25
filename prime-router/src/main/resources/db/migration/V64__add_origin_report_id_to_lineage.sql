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
 * Add a space to store the origin report id on any values in the item lineage
 * to remove the need to go up the tree to find relevant metadata for deliveries
 * see https://github.com/CDCgov/prime-reportstream/issues/8985
 */
ALTER TABLE item_lineage
    ADD origin_report_id UUID NULL,
    ADD origin_report_index INT NULL;

ALTER TABLE IF EXISTS public.item_lineage
    ADD CONSTRAINT item_lineage_origin_report_id_fkey FOREIGN KEY (origin_report_id)
    REFERENCES public.report_file (report_id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE CASCADE;