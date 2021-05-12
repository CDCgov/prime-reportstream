
### Schema:         co/co-covid-19-redox
#### Description:   Colorado Department of Public Health (CDPHE) REDOX messages

---

**Name**: redox_source_id

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_source_name

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_destination_id

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_destination_name

**PII**: No

**Cardinality**: [0..1]

---

**Name**: processing_mode_code

**Type**: CODE

**PII**: No

**HL7 Field**: MSH-11-1

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
D|Debugging
P|Production
T|Training
D|Debugging
T|Training
P|Production

**Alt Value Sets**

Code | Display
---- | -------
D|true
T|true
P|false

**Documentation**:

P, D, or T for Production, Debugging, or Training

---

**Name**: patient_id

**Type**: TEXT

**PII**: No

**HL7 Field**: PID-3-1

**Cardinality**: [0..1]

---

**Name**: patient_id_type

**Type**: TEXT

**PII**: No

**HL7 Field**: PID-3-5

**Cardinality**: [0..1]

---

**Name**: patient_last_name

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-1

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: patient_first_name

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-2

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: patient_middle_name

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-3

**Cardinality**: [0..1]

---

**Name**: patient_dob

**Type**: DATE

**PII**: Yes

**HL7 Field**: PID-7

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: patient_drivers_license

**Type**: ID_DLN

**PII**: Yes

**HL7 Field**: PID-20-1

**Cardinality**: [0..1]

---

**Name**: redox_patient_drivers_license_type

**PII**: No

**Cardinality**: [0..1]

---

**Name**: patient_ethnicity

**Type**: CODE

**PII**: No

**HL7 Field**: PID-22

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
H|Hispanic or Latino
N|Non Hispanic or Latino
U|Unknown
H|Hispanic or Latino
N|Non Hispanic or Latino

**Alt Value Sets**

Code | Display
---- | -------
H|true
N|false

**Documentation**:

The patient's ethnicity. There is a valueset defined based on the values in PID-22, but downstream
consumers are free to define their own values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: patient_gender

**Type**: CODE

**PII**: No

**HL7 Field**: PID-8-1

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
M|Male
F|Female
O|Other
A|Ambiguous
U|Unknown
N|Not applicable
M|Male
F|Female
O|Other
A|Ambiguous
U|Unknown
N|Not applicable

**Alt Value Sets**

Code | Display
---- | -------
M|Male
F|Female
O|Other
A|Nonbinary
U|Unknown
N|Unknown

**Documentation**:

The patient's gender. There is a valueset defined based on the values in PID-8-1, but downstream consumers are free to define their own accepted values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: patient_race

**Type**: CODE

**PII**: No

**HL7 Field**: PID-10

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
1002-5|American Indian or Alaska Native
2028-9|Asian
2054-5|Black or African American
2076-8|Native Hawaiian or Other Pacific Islander
2106-3|White
2131-1|Other
UNK|Unknown
ASKU|Asked, but unknown
1002-5|American Indian or Alaska Native
2028-9|Asian
2054-5|Black or African American
2076-8|Native Hawaiian or Other Pacific Islander
2106-3|White
2131-1|Other
ASKU|Asked, but unknown

**Alt Value Sets**

Code | Display
---- | -------
1002-5|American Indian or Alaska Native
2028-9|Asian
2054-5|Black or African American
2076-8|Native Hawaiian or Other Pacific Islander
2106-3|White
2131-1|Other Race
ASKU|Unknown

**Documentation**:

The patient's race. There is a common valueset defined for race values, but some states may choose to define different code/value combinations.


---

**Name**: patient_phone_number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Field**: PID-13

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: patient_street

**Type**: STREET

**PII**: Yes

**HL7 Field**: PID-11-1

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: patient_city

**Type**: CITY

**PII**: Yes

**HL7 Field**: PID-11-3

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: patient_state

**Type**: TABLE

**PII**: No

**HL7 Field**: PID-11-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: patient_zip_code

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: PID-11-5

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: patient_county

**Type**: TABLE_OR_BLANK

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: patient_country

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: patient_email

**Type**: EMAIL

**PII**: Yes

**HL7 Field**: PID-13-4

**Cardinality**: [0..1]

---

**Name**: placer_order_id

**Type**: ID

**PII**: No

**HL7 Fields**: ORC-2-1, OBR-2-1

**Cardinality**: [0..1]

---

**Name**: ordering_facility_name

**Type**: TEXT

**PII**: No

**HL7 Field**: ORC-21-1

**Cardinality**: [0..1]

**Documentation**:

The name of the facility which the test was ordered from

---

**Name**: ordering_facility_street

**Type**: STREET

**PII**: No

**HL7 Field**: ORC-22-1

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: ordering_facility_city

**Type**: CITY

**PII**: No

**HL7 Field**: ORC-22-3

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: ordering_facility_state

**Type**: TABLE

**PII**: No

**HL7 Field**: ORC-22-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---

**Name**: ordering_facility_zip_code

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: ORC-22-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: ordering_facility_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: ordering_facility_country

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordering_facility_phone_number

**Type**: TELEPHONE

**PII**: No

**HL7 Field**: ORC-23

**Cardinality**: [0..1]

**Documentation**:

The phone number of the facility which the test was ordered from

---

**Name**: ordering_facility_email

**Type**: EMAIL

**PII**: No

**HL7 Field**: ORC-23-4

**Cardinality**: [0..1]

---

**Name**: order_test_date

**Type**: DATE

**PII**: No

**HL7 Field**: ORC-15

**Cardinality**: [0..1]

---

**Name**: filler_order_id

**Type**: ID

**PII**: No

**HL7 Fields**: ORC-3-1, SPM-2-2, OBR-3-1

**Cardinality**: [0..1]

**Documentation**:

Accension number

---

**Name**: specimen_received_date_time

**PII**: No

**Cardinality**: [0..1]

---

**Name**: specimen_collection_date_time

**Type**: DATETIME

**PII**: No

**HL7 Fields**: SPM-17-1, OBR-7, OBR-8, OBX-14

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: test_result_date

**Type**: DATETIME

**PII**: No

**HL7 Field**: OBX-19

**Cardinality**: [0..1]

---

**Name**: test_result_status

**Type**: CODE

**PII**: No

**HL7 Fields**: OBX-11-1, OBR-25-1

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
A|Some, but not all, results available
C|Corrected, final
F|Final results
I|No results available; specimen received, procedure incomplete
M|Corrected, not final
N|Procedure completed, results pending
O|Order received; specimen not yet received
P|Preliminary
R|Results stored; not yet verified
S|No results available; procedure scheduled, but not done
X|No results available; Order canceled
Y|No order on record for this test
Z|No record of this patient
C|Corrected, final
F|Final results
X|No results available; Order canceled
A|Some, but not all, results available
I|No results available; specimen received, procedure incomplete
M|Corrected, not final
N|Procedure completed, results pending
O|Order received; specimen not yet received
P|Preliminary
R|Results stored; not yet verified
S|No results available; procedure scheduled, but not done
Y|No order on record for this test
Z|No record of this patient

**Alt Value Sets**

Code | Display
---- | -------
C|Corrected
F|Final
X|Canceled
A|Preliminary
I|Unavailable
M|Corrected
N|Preliminary
O|Preliminary
P|Preliminary
R|Preliminary
S|Unavailable
Y|Unavailable
Z|Unavailable

---

**Name**: ordered_test_code

**Type**: TABLE

**PII**: No

**HL7 Field**: OBR-4-1

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Ordered LOINC Code

---

**Name**: ordered_test_system

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordered_test_name

**Type**: TABLE

**PII**: No

**HL7 Field**: OBR-4-2

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Ordered LOINC Long Name

---

**Name**: ordering_provider_id

**Type**: ID_NPI

**PII**: No

**HL7 Fields**: ORC-12-1, OBR-16-1

**Cardinality**: [0..1]

**Documentation**:

The ordering provider’s National Provider Identifier

---

**Name**: ordering_provider_first_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**: ORC-12-3, OBR-16-3

**Cardinality**: [0..1]

**Documentation**:

The first name of the provider who ordered the test

---

**Name**: ordering_provider_last_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**: ORC-12-2, OBR-16-2

**Cardinality**: [0..1]

**Documentation**:

The last name of provider who ordered the test

---

**Name**: ordering_provider_street

**Type**: STREET

**PII**: Yes

**HL7 Field**: ORC-24-1

**Cardinality**: [0..1]

**Documentation**:

The street address of the provider

---

**Name**: ordering_provider_city

**Type**: CITY

**PII**: Yes

**HL7 Field**: ORC-24-3

**Cardinality**: [0..1]

**Documentation**:

The city of the provider

---

**Name**: ordering_provider_state

**Type**: TABLE

**PII**: No

**HL7 Field**: ORC-24-4

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the provider

---

**Name**: ordering_provider_zip_code

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: ORC-24-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the provider

---

**Name**: ordering_provider_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: ordering_provider_country

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordering_provider_phone_number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Fields**: ORC-14, OBR-17

**Cardinality**: [0..1]

**Documentation**:

The phone number of the provider

---

**Name**: ordering_provider_email

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_resulted

**PII**: No

**Cardinality**: [0..1]

---

**Name**: device_id

**Type**: TABLE

**PII**: No

**HL7 Fields**: OBX-17-1, OBX-17-9

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Documentation**:

Device_id is a generated value for the OBX-17 field. It is based on the device model and the LIVD table.

---

**Name**: test_performed_code

**Type**: TABLE

**PII**: No

**HL7 Field**: OBX-3-1

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Performed LOINC Code

**Documentation**:

The LOINC code of the test performed. This is a standardized coded value describing the test

---

**Name**: test_performed_system

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: test_performed_name

**Type**: TABLE

**PII**: No

**HL7 Field**: OBX-3-2

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Performed LOINC Long Name

**Documentation**:

The LOINC description of the test performed as related to the LOINC code.

---

**Name**: specimen_type

**Type**: CODE

**PII**: No

**HL7 Field**: SPM-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
445297001|Swab of internal nose
258500001|Nasopharyngeal swab
871810001|Mid-turbinate nasal swab
697989009|Anterior nares swab
258411007|Nasopharyngeal aspirate
429931000124105|Nasal aspirate
258529004|Throat swab
119334006|Sputum specimen
119342007|Saliva specimen
258607008|Bronchoalveolar lavage fluid sample
119364003|Serum specimen
119361006|Plasma specimen
440500007|Dried blood spot specimen
258580003|Whole blood sample
122555007|Venous blood specimen

**Documentation**:

The specimen source, such as Blood or Serum

---

**Name**: specimen_source_site_code

**Type**: CODE

**PII**: No

**HL7 Field**: SPM-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
119297000|Blood specimen (specimen)
71836000|Nasopharyngeal structure (body structure)
45206002|Nasal structure (body structure)

---

**Name**: reference_range

**Type**: TEXT

**PII**: No

**HL7 Field**: OBX-7

**Cardinality**: [0..1]

**Documentation**:

The reference range of the lab result, such as “Negative” or “Normal”. For IgG, IgM and CT results that provide a value you MUST fill out this filed.

---

**Name**: test_result

**Type**: CODE

**PII**: No

**HL7 Field**: OBX-5

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
260373001|Detected
260415000|Not detected
720735008|Presumptive positive
10828004|Positive
42425007|Equivocal
260385009|Negative
895231008|Not detected in pooled specimen
462371000124108|Detected in pooled specimen
419984006|Inconclusive
125154007|Specimen unsatisfactory for evaluation
455371000124106|Invalid result
840539006|Disease caused by sever acute respitory syndrome coronavirus 2 (disorder)
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)

**Documentation**:

The result of the test performed. For IgG, IgM and CT results that give a numeric value put that here.

---

**Name**: redox_test_result_type

**PII**: No

**Cardinality**: [0..1]

---

**Name**: abnormal_flag

**Type**: CODE

**PII**: No

**HL7 Field**: OBX-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
A|Abnormal (applies to non-numeric results)
>|Above absolute high-off instrument scale
H|Above high normal
HH|Above upper panic limits
AC|Anti-complementary substances present
<|Below absolute low-off instrument scale
L|Below low normal
LL|Below lower panic limits
B|Better--use when direction not relevant
TOX|Cytotoxic substance present
DET|Detected
IND|Indeterminate
I|Intermediate. Indicates for microbiology susceptibilities only.
MS|Moderately susceptible. Indicates for microbiology susceptibilities only.
NEG|Negative
null|No range defined, or normal ranges don't apply
NR|Non-reactive
N|Normal (applies to non-numeric results)
ND|Not Detected
POS|Positive
QCF|Quality Control Failure
RR|Reactive
R|Resistant. Indicates for microbiology susceptibilities only.
D|Significant change down
U|Significant change up
S|Susceptible. Indicates for microbiology susceptibilities only.
AA|Very abnormal (applies to non-numeric units, analogous to panic limits for numeric units)
VS|Very susceptible. Indicates for microbiology susceptibilities only.
WR|Weakly reactive
W|Worse--use when direction not relevant
A|Abnormal (applies to non-numeric results)
N|Normal (applies to non-numeric results)

**Alt Value Sets**

Code | Display
---- | -------
A|Abnormal
N|Normal

**Documentation**:

This field is generated based on the normalcy status of the result. A = abnormal; N = normal

---

**Name**: date_result_released

**Type**: DATETIME

**PII**: No

**HL7 Field**: OBR-22

**Cardinality**: [0..1]

---

**Name**: testing_lab_clia

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**: OBX-15-1, OBX-23-10, ORC-3-3, OBR-3-3, OBR-2-3, ORC-2-3

**Cardinality**: [1..1]

**Documentation**:

CLIA Number from the laboratory that sends the message to DOH

An example of the ID is 03D2159846


---

**Name**: redox_test_lab_id_type

**PII**: No

**Cardinality**: [0..1]

---

**Name**: testing_lab_name

**Type**: TEXT

**PII**: No

**HL7 Fields**: ORC-2-2, OBR-2-2, ORC-3-2, OBR-3-2, OBX-23-1

**Cardinality**: [0..1]

**Documentation**:

The name of the laboratory which performed the test, can be the same as the sending facility name

---

**Name**: testing_lab_street

**Type**: STREET

**PII**: No

**HL7 Field**: OBX-24-1

**Cardinality**: [0..1]

---

**Name**: testing_lab_city

**Type**: CITY

**PII**: No

**HL7 Field**: OBX-24-3

**Cardinality**: [0..1]

---

**Name**: testing_lab_state

**Type**: TABLE

**PII**: No

**HL7 Field**: OBX-24-4

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

---

**Name**: testing_lab_zip_code

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: OBX-24-5

**Cardinality**: [0..1]

---

**Name**: testing_lab_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: testing_lab_country

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: testing_lab_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

---

**Name**: testing_lab_email

**PII**: No

**Cardinality**: [0..1]

---

**Name**: equipment_model_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Model

---

**Name**: pregnant

**Type**: CODE

**PII**: No

**HL7 Field**: AOE

**LOINC Code**: 82810-3

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
77386006|Pregnant
60001007|Not Pregnant
261665006|Unknown

**Documentation**:

Is the patient pregnant?

---

**Name**: redox_pregnant_value_type

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_pregnant_code

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_pregnant_codeset

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_pregnant_description

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_pregnant_status

**PII**: No

**Cardinality**: [0..1]

---

**Name**: illness_onset_date

**Type**: DATE

**PII**: No

**HL7 Field**: AOE

**LOINC Code**: 65222-2

**Cardinality**: [0..1]

---

**Name**: redox_illness_onset_date_value_type

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_illness_onset_date_code

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_illness_onset_date_codeset

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_illness_onset_date_description

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_illness_onset_date_status

**PII**: No

**Cardinality**: [0..1]

---

**Name**: employed_in_healthcare

**Type**: CODE

**PII**: No

**HL7 Field**: AOE

**LOINC Code**: 95418-0

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Is the patient employed in health care?

---

**Name**: redox_employed_in_healthcare_value_type

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_employed_in_healthcare_code

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_employed_in_healthcare_codeset

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_employed_in_healthcare_description

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_employed_in_healthcare_status

**PII**: No

**Cardinality**: [0..1]

---

**Name**: first_test

**Type**: CODE

**PII**: No

**HL7 Field**: AOE

**LOINC Code**: 95417-2

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Is this the patient's first test for this condition?

---

**Name**: redox_first_test_value_type

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_first_test_code

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_first_test_codeset

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_first_test_description

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_first_test_status

**PII**: No

**Cardinality**: [0..1]

---

**Name**: hospitalized

**Type**: CODE

**PII**: No

**HL7 Field**: AOE

**LOINC Code**: 77974-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Is the patient hospitalized?

---

**Name**: redox_hospitalized_value_type

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_hospitalized_code

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_hospitalized_codeset

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_hospitalized_description

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_hospitalized_status

**PII**: No

**Cardinality**: [0..1]

---

**Name**: icu

**Type**: CODE

**PII**: No

**HL7 Field**: AOE

**LOINC Code**: 95420-6

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Is the patient in the ICU?

---

**Name**: redox_icu_value_type

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_icu_code

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_icu_codeset

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_icu_description

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_icu_status

**PII**: No

**Cardinality**: [0..1]

---

**Name**: resident_congregate_setting

**Type**: CODE

**PII**: No

**HL7 Field**: AOE

**LOINC Code**: 95421-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Does the patient reside in a congregate care setting?

---

**Name**: redox_resident_congregate_setting_value_type

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_resident_congregate_setting_code

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_resident_congregate_setting_codeset

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_resident_congregate_setting_description

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_resident_congregate_setting_status

**PII**: No

**Cardinality**: [0..1]

---

**Name**: symptomatic_for_disease

**Type**: CODE

**PII**: No

**HL7 Field**: AOE

**LOINC Code**: 95419-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Is the patient symptomatic?

---

**Name**: redox_symptomatic_for_disease_value_type

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_symptomatic_for_disease_code

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_symptomatic_for_disease_codeset

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_symptomatic_for_disease_description

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_symptomatic_for_disease_status

**PII**: No

**Cardinality**: [0..1]

---
