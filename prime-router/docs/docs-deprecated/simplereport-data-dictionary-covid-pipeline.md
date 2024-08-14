# Data Dictionary for data incoming from SimpleReport

Official names of fields are in [pdi-covid-19.schema](../metadata/schemas/PrimeDataInput/pdi-covid-19.schema).
Official types of fields are mostly in [covid-19.schema](../metadata/schemas/covid-19.schema).
For types that are enumerations, the official values are mostly in [common.valuesets](../metadata/valuesets/common.valuesets) for general enums (eg Yes, No, Unknown), and in [covid-19.valuesets](../metadata/valuesets/covid-19.valuesets) for Covid specific items.

This doc is a summary of what's in those files.  At the moment, in the list below, I've only listed the more complex fields.

### csvField: Patient_race

Expecting one of the `code` values:

```
  values:
    - display: N      # Native
      code: 1002-5
    - display: A      # Asian
      code: 2028-9
    - display: B      # Black
      code: 2054-5
    - display: P      # Pacific Islander
      code: 2076-8
    - display: W      # White
      code: 2106-3
    - display: O      # Other
      code: 2131-1
    - display: O
      code: UNK
    - display: O      #  Asked, but unknown
      code: ASKU
```

### csvField: Patient_DOB

Expected form:   YYYYmmDD

### csvField: Patient_gender

Expecting one of the `code` values:

```
  values:
    - code: M
      display: Male
    - code: F
      display: Female
    - code: O
      display: Other
    - code: A
      display: Ambiguous
    - code: U
      alt_codes: [UNK]
      display: Unknown
    - code: N
      alt_codes: [NA]
      display: Not applicable
```

### csvField: Patient_ethnicity

Expecting one of the `code` values:

```
  values:
    - code: H
      display: Hispanic or Latino
    - code: N
      display: Non Hispanic or Latino
    - code: U
      display: Unknown
```

### csvField: Patient_street2

The correct column name ends in "street2"

### csvField: Patient_state

For all state fields, expected the two letter code.

### csvField: Patient_phone_number

For all phone numbers, Hub will parse out extranous punctuation, and forward just the digits.

### csvField: Patient_ID

Patient ID is generated internally by SimpleReport.  An internal database identifier the users don't see.

Plain text string.

###    csvField: Patient_lookup_ID

Lookup_id is the ID the user entered into SimpleReport.  Plain text string.

### csvField: Employed_in_healthcare

Expecting Y, N, UNK

### csvField: Resident_congregate_setting

Expecting Y, N, UNK

### csvField: Test_result_coded

Expecting one of the `code` values:

```
    - code: 260373001
      display: Detected
    - code: 260415000
      display: Not detected
    - code: 895231008
      display: Not detected in pooled specimen
    - code: 462371000124108
      display: Detected in pooled specimen
    - code: 419984006
      display: Inconclusive
```

### csvField: Specimen_collection_date_time

Expecting YYYYmmDD at the moment.  

### csvField: First_test

Expecting Y, N, UNK

### csvField: Symptomatic_for_disease

Expecting Y, N, UNK

### csvField: Testing_lab_CLIA

Typically of the form "##D#######"

### csvField: Testing_lab_street2

Note the street2.

### csvField: Ordered_test_code

The test that is being ordered.  Found in column H of the [LIVD-SARS spreadsheet](https://www.cdc.gov/csels/dls/documents/livd_test_code_mapping/LIVD-SARS-CoV-2-2020-10-21.xlsx), as the LOINC Order Code

We can add values as needed, but at the moment of this writing this is what we have tracked:

Expecting one of the `code` values:

```
    - code: 94563-4
      display: SARS coronavirus 2 IgG Ab [Presence] in Serum or Plasma by Immunoassay
    - code: 94500-6
      display: SARS coronavirus 2 RNA [Presence] in Respiratory specimen by NAA with probe detection
    - code: 94558-4
      display: SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay
    - code: 94534-5
      display: SARS coronavirus 2 RdRp gene [Presence] in Respiratory specimen by NAA with probe detection
    - code: 94564-2
      display: SARS-CoV-2 (COVID-19) IgM Ab [Presence] in Serum or Plasma by Immunoassay
    - code: 94531-1
      display: SARS coronavirus 2 RNA panel - Respiratory specimen by NAA with probe detection
    - code: 94559-2
      display: SARS coronavirus 2 ORF1ab region [Presence] in Respiratory specimen by NAA with probe detection
    - code: 95209-3
      display: SARS coronavirus+SARS coronavirus 2 Ag [Presence] in Respiratory specimen by Rapid immunoassay
```

### csvField: Specimen_source_site_code

Plain text string right now.  Example: 'Forearm'.  May turn into controlled vocab.

### csvField: Specimen_type_code

Type of specimen taken from the patient.   

Expecting one of the `code` values:

```
    - code: 258500001
      display: Nasopharyngeal swab
    - code: 871810001
      display: Mid-turbinate nasal swab
    - code: 697989009
      display: Anterior nares swab
    - code: 258411007
      display: Nasopharyngeal aspirate
    - code: 429931000124105
      display: Nasal aspirate
    - code: 258529004
      display: Throat swab
    - code: 119334006
      display: Sputum specimen
    - code: 119342007
      display: Saliva specimen
    - code: 258607008
      display: Bronchoalveolar lavage fluid sample
    - code: 119364003
      display: Serum specimen
    - code: 119361006
      display: Plasma specimen
    - code: 440500007
      display: Dried blood spot specimen
    - code: 258580003
      display: Whole blood sample
    - code: 122555007
      display: Venous blood specimen
```

### csvField: Device_ID

The type of device used for the test.  eg, BinaxNOW COVID-19 Ag Card
Plain text string right now.   May turn into controlled vocab.

### csvField: Instrument_ID

Identifier/Serial num for the specific piece of equipment used to run the test.
Plain text string.

### csvField: Test_date

YYYYmmDD

### csvField: Date_result_released

YYYYmmDD
