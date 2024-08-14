/* Clean up any existing structures for the view that exist now */
DROP INDEX IF EXISTS idx_vw_livid_manufacturer;
DROP INDEX IF EXISTS idx_vw_livid_model;
DROP INDEX IF EXISTS idx_vw_livid_test_performed_loinc_code;
DROP INDEX IF EXISTS idx_vw_livid_test_orderd_loinc_code;
DROP INDEX IF EXISTS idx_vw_livid_otc;

/* Remove the view */
DROP MATERIALIZED VIEW IF EXISTS vw_livd_table;

/*
 * Recreate the view. It is better to do a drop and recreate. CREATE OR REPLACE could fail
 * if the shape of the data is different. Wanting this to be idempotent, and given the fact
 * that it's a view, it's okay to just drop it and rebuild it.
 */
CREATE MATERIALIZED VIEW vw_livd_table
AS
SELECT
    r.lookup_table_row_id
     , r.lookup_table_version_id
     , r.row_num
     , r.data->>'Manufacturer'                        as "manufacturer"
     , r.data->>'Model'                               as "model"
     , r.data->>'Vendor Analyte Name'                 as "vendor_analyte_name"
     , r.data->>'Vendor Specimen Description'         as "vendor_specimen_description"
     , r.data->>'Vendor Result Description'           as "vendor_result_description"
     , r.data->>'Test Performed LOINC Code'           as "test_performed_loinc_code"
     , r.data->>'Test Performed LOINC Long Name'      as "test_performed_loinc_long_name"
     , r.data->>'Test Ordered LOINC Code'             as "test_ordered_loinc_code"
     , r.data->>'Test Ordered LOINC Long Name'        as "test_orderd_loinc_long_name"
     , r.data->>'Vendor Comment'                      as "vendor_comment"
     , r.data->>'Vendor Analyte Code'                 as "vendor_analyte_code"
     , r.data->>'Vendor Reference ID'                 as "vendor_reference_id"
     , r.data->>'Testkit Name ID'                     as "testkit_name_id"
     , r.data->>'Testkit Name ID Type'                as "testkit_name_id_type"
     , r.data->>'Equipment UID'                       as "equipment_uid"
     , r.data->>'Equipment UID Type'                  as "equipment_uid_type"
     , r.data->>'Component'                           as "component"
     , r.data->>'Property'                            as "property"
     , r.data->>'Time'                                as "time"
     , r.data->>'System'                              as "system"
     , r.data->>'Scale'                               as "scale"
     , r.data->>'Method'                              as "method"
     , r.data->>'Publication Version ID'              as "publication_version_id"
     , r.data->>'LOINC Version ID'                    as "loinc_version_id"
     , r.data->>'Saliva'                              as "saliva"
     , r.data->>'Pooling'                             as "pooling"
     , r.data->>'Screening'                           as "screening"
     , r.data->>'Test Type'                           as "test_type"
     , r.data->>'CLIAComplexity'                      as "clia_complexity"
     , r.data->>'Fingerstick'                         as "fingerstick"
     , r.data->>'Test Method'                         as "test_method"
     , r.data->>'Multi-analyte'                       as "multi-analyte"
     , r.data->>'Collection Kit'                      as "collection_kit"
     , r.data->>'Serial Testing'                      as "serial_testing"
     , r.data->>'Home Collection'                     as "home_collection"
     , r.data->>'Serial Screening'                    as "serial_screening"
     , r.data->>'Authorized Settings'                 as "authorized_settings"
     , r.data->>'Quantitative Result'                 as "quantitative_result"
     , r.data->>'Direct to Consumer (DTC)'            as "direct_to_consumer"
     , r.data->>'Non-prescription Testing'            as "non-prescription_testing"
     , r.data->>'Prescription Home Testing'           as "prescription_home_testing"
     , r.data->>'Visual or Instrument Read'           as "visual_or_instrument_read"
     , r.data->>'Show All Home Testing Rows'          as "show_all_home_testing_rows"
     , r.data->>'Single or Multiple Targets'          as "single_or_multiple_targets"
     , r.data->>'Telehealth Proctor Supervised'       as "telehealth_proctor_supervised"
     , r.data->>'Pooled Serial Screening - Swab'      as "Pooled_serial_screening_swab"
     , r.data->>'Pooled Serial Screening - Media'     as "pooled_serial_screening_media"
     , r.data->>'Over the Counter (OTC) Home Testing' as "over_the_counter_home_testing"
     -- additional fields we add for our own needs
     , r.data->>'is_otc'                              as "is_otc"
     , r.data->>'is_home'                             as "is_home"
     , r.data->>'is_serial'                           as "is_serial"
     , r.data->>'is_unproctored'                      as "is_unproctored"
     , r.data->>'fda_ref'                             as "fda_ref"
     , r.data->>'fda_authorization'                   as "fda_authorization"
     , r.data->>'processing_mode_code'                as "processing_mode_code"
FROM
    lookup_table_row r
        JOIN lookup_table_version v ON r.lookup_table_version_id = v.lookup_table_version_id
        and upper(v.table_name) = 'LIVD-SARS-COV-2'
ORDER BY
        r.data->>'Manufacturer'
       , r.data->>'Model'
       , r.data->>'Equipment UID'
       , r.row_num
;

/* Create all our views */
CREATE INDEX idx_vw_livid_manufacturer on vw_livd_table(manufacturer);
CREATE INDEX idx_vw_livid_model on vw_livd_table(model);
CREATE INDEX idx_vw_livid_test_performed_loinc_code on vw_livd_table(test_performed_loinc_code);
CREATE INDEX idx_vw_livid_test_ordered_loinc_code on vw_livd_table(test_ordered_loinc_code);
CREATE INDEX idx_vw_livid_otc on vw_livd_table(is_otc, is_home, is_serial, is_unproctored);

/*
 Create a void function to safely refresh materialized views
 */
CREATE OR REPLACE FUNCTION refresh_materialized_views(table_name VARCHAR(256)) RETURNS VOID
LANGUAGE plpgsql
AS $$
    DECLARE
        view_exists integer;
    BEGIN
        /* Check to see what table we're updating */
        IF upper(table_name) = 'LIVD-SARS-COV-2' THEN
            /* Look to see if the livd materialized view exists */
            SELECT COUNT(*) INTO view_exists FROM pg_matviews WHERE matviewname ILIKE 'vw_livd_table';
            /* Refresh the view if it exists */
            IF view_exists > 0 THEN
                REFRESH MATERIALIZED VIEW vw_livd_table;
            END IF;
        END IF;
    END;
$$;