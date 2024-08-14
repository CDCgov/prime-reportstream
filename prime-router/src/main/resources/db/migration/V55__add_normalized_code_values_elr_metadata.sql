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
 * V55__add_normalized_code_values_elr_metadata
 *
 * Adds a set of "normalized" columns to the ELR metadata table so we can preserve both the raw text that
 * labs send us for tests, and then map to the preferred SNOMED/LOINC values
 */
ALTER TABLE elr_result_metadata ADD test_result_normalized VARCHAR(512);

ALTER TABLE elr_result_metadata ADD test_ordered_normalized VARCHAR(512);

ALTER TABLE elr_result_metadata ADD test_ordered_long_name VARCHAR(512);

ALTER TABLE elr_result_metadata ADD test_performed_normalized VARCHAR(512);

ALTER TABLE elr_result_metadata ADD test_performed_long_name VARCHAR(512);

-- add an expanded set of specimen-related fields
ALTER TABLE elr_result_metadata ADD specimen_source_site_code VARCHAR(512);

ALTER TABLE elr_result_metadata ADD specimen_type_normalized VARCHAR(512);

ALTER TABLE elr_result_metadata ADD specimen_type_code VARCHAR(512);

ALTER TABLE elr_result_metadata ADD specimen_collection_method_code VARCHAR(512);

ALTER TABLE elr_result_metadata ADD specimen_collection_site_code VARCHAR(512);


-- we will now swap values for the test result fields
UPDATE
    elr_result_metadata
SET
    test_result_normalized = test_result_code
;

UPDATE
    elr_result_metadata
SET
    test_result_code = test_result
;

UPDATE
    elr_result_metadata
SET
    test_result = test_result_normalized
;

-- now we fill in the normalized values
-- we are intentionally leaving the normalized field null if it doesn't match
-- if we can't map it, it's not able to be normalized
UPDATE
    elr_result_metadata
SET
    test_result_normalized = CASE test_result_code
        WHEN '260373001'    THEN 'Detected'
        WHEN '419984006'    THEN 'Inconclusive'
        WHEN '42425007'     THEN 'Equivocal'
        WHEN '260415000'    THEN 'Not detected'
        WHEN '373121007'    THEN 'Test not done'
    END
;

UPDATE
    elr_result_metadata
SET
    specimen_type_normalized = CASE specimen_type_code
        WHEN '435541000124108'      THEN 'Scab specimen (crust)'
        WHEN '472862007'            THEN 'Swab from lesion of skin'
        WHEN '16210971000119108'    THEN 'Swab from lesion'
        WHEN '418932006'            THEN 'Oral swab'
        WHEN '258528007'            THEN 'Rectal swab'
    END
;

UPDATE
    elr_result_metadata
SET
    test_ordered_normalized = case test_ordered_code
        WHEN '41853-3' THEN 'Orthopoxvirus'
        WHEN '100434-0' THEN 'Non-variola Orthopoxvirus'
        WHEN '100383-9' THEN 'Monkeypox Virus'
        WHEN '100888-7' THEN 'West African Monkeypox Virus'
        WHEN '100889-5' THEN 'Congo Basin Monkeypox Virus'
        WHEN '100885-3' THEN 'Parapoxvirus'
        WHEN '100886-1' THEN 'Orf Virus'
        WHEN '100887-9' THEN 'Pseudocowpox Virus'
        WHEN '100891-1' THEN 'Orthopoxvirus IgG'
        WHEN '100892-9' THEN 'Orthopoxvirus IgM'
    END
;

UPDATE
    elr_result_metadata
SET
    test_ordered_long_name = case test_ordered_code
        WHEN '41853-3' THEN 'Orthopoxvirus DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100434-0' THEN 'Orthopoxvirus non-variola DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100383-9' THEN 'Monkeypox virus DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100888-7' THEN 'West African monkeypox virus DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100889-5' THEN 'Congo Basin monkeypox virus DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100885-3' THEN 'Parapoxvirus DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100886-1' THEN 'Orf virus DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100887-9' THEN 'Pseudocowpox virus DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100891-1' THEN 'Orthopoxvirus IgG Ab [Presence] in Serum or Plasma by Immunoassay'
        WHEN '100892-9' THEN 'Orthopoxvirus IgM Ab [Presence] in Serum or Plasma by Immunoassay'
END
;

UPDATE
    elr_result_metadata
SET
    test_performed_normalized = case test_performed_code
        WHEN '41853-3' THEN 'Orthopoxvirus'
        WHEN '100434-0' THEN 'Non-variola Orthopoxvirus'
        WHEN '100383-9' THEN 'Monkeypox Virus'
        WHEN '100888-7' THEN 'West African Monkeypox Virus'
        WHEN '100889-5' THEN 'Congo Basin Monkeypox Virus'
        WHEN '100885-3' THEN 'Parapoxvirus'
        WHEN '100886-1' THEN 'Orf Virus'
        WHEN '100887-9' THEN 'Pseudocowpox Virus'
        WHEN '100891-1' THEN 'Orthopoxvirus IgG'
        WHEN '100892-9' THEN 'Orthopoxvirus IgM'
    END
;

UPDATE
    elr_result_metadata
SET
    test_performed_long_name = case test_performed_code
        WHEN '41853-3' THEN 'Orthopoxvirus DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100434-0' THEN 'Orthopoxvirus non-variola DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100383-9' THEN 'Monkeypox virus DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100888-7' THEN 'West African monkeypox virus DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100889-5' THEN 'Congo Basin monkeypox virus DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100885-3' THEN 'Parapoxvirus DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100886-1' THEN 'Orf virus DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100887-9' THEN 'Pseudocowpox virus DNA [Presence] in Specimen by NAA with probe detection'
        WHEN '100891-1' THEN 'Orthopoxvirus IgG Ab [Presence] in Serum or Plasma by Immunoassay'
        WHEN '100892-9' THEN 'Orthopoxvirus IgM Ab [Presence] in Serum or Plasma by Immunoassay'
    END
;