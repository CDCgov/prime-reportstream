/*
This SQL creates the tables of the DB. The Flyway tool applies this migration to create the database

Follow this style guide https://about.gitlab.com/handbook/business-ops/data-team/platform/sql-style-guide/
use VARCHAR(63) for names in organization and schema

Copy a version of this comment into the next migration
*/

/*
    An item can be a covid test result or any other message (HL7 v2), row (CSV), bundle (FHIR), et. al.
*/
CREATE TABLE item (
    -- Key
                            id SERIAL PRIMARY KEY,

    -- Value
                            report_id UUID,
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    -- Foreign Key
                            CONSTRAINT report_id
                                FOREIGN KEY(report_id)
                                REFERENCES report_file(report_id)
);

/*
    Add FK references to ITEM table in related tables
 */
ALTER TABLE item_lineage ADD COLUMN item_id INT
    CONSTRAINT item_lineage_item_id_fkey REFERENCES item (id);

ALTER TABLE covid_result_metadata ADD COLUMN item_id INT
    CONSTRAINT covid_result_metadata_item_id_fkey REFERENCES item (id);

ALTER TABLE elr_result_metadata ADD COLUMN item_id INT
    CONSTRAINT elr_result_metadata_item_id_fkey REFERENCES item (id);