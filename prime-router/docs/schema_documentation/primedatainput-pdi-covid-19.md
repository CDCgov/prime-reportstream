
### Schema:         primedatainput/pdi-covid-19
#### Description:   SimpleReport COVID-19 flat file

---

**Name**: Patient_last_name

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-1

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: Patient_first_name

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-2

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: Patient_middle_name

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-3

**Cardinality**: [0..1]

---

**Name**: Patient_suffix

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-4

**Cardinality**: [0..1]

---

**Name**: Patient_ID

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

**Name**: Ordered_test_code

**Type**: TABLE

**PII**: No

**HL7 Field**: OBX-3-1

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Performed LOINC Code

**Documentation**:

The LOINC code of the test performed. This is a standardized coded value describing the test

---

**Name**: Specimen_source_site_code

**Type**: CODE

**PII**: No

**Format**: $code

**HL7 Field**: SPM-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
119297000|Blood specimen (specimen)
71836000|Nasopharyngeal structure (body structure)
45206002|Nasal structure (body structure)

---

**Name**: Specimen_type_code

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

**Name**: Device_ID

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Model

---

**Name**: Instrument_ID

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Result_ID

**Type**: ID

**PII**: No

**HL7 Field**: MSH-10

**Cardinality**: [1..1]

**Documentation**:

unique id to track the usage of the message

---

**Name**: Corrected_result_ID

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

pointer/link to the unique id of a previously submitted result.  Usually blank. Or, if an item modifies/corrects a prior item, this field holds the message_id of the prior item.

---

**Name**: placer_order_id

**Type**: ID

**PII**: No

**HL7 Fields**: ORC-2-1, OBR-2-1

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

**Name**: Test_result_status

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

---

**Name**: Test_result_code

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

**Name**: Illness_onset_date

**Type**: DATE

**PII**: No

**HL7 Field**: AOE

**LOINC Code**: 65222-2

**Cardinality**: [0..1]

---

**Name**: Specimen_collection_date_time

**Type**: DATETIME

**PII**: No

**HL7 Fields**: SPM-17-1, OBR-7, OBR-8, OBX-14

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: Order_test_date

**Type**: DATE

**PII**: No

**HL7 Field**: ORC-15

**Cardinality**: [0..1]

---

**Name**: Test_date

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

**HL7 Field**: OBX-19

**Cardinality**: [0..1]

---

**Name**: Date_result_released

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

**HL7 Field**: OBR-22

**Cardinality**: [0..1]

---

**Name**: Patient_race

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

**Documentation**:

The patient's race. There is a common valueset defined for race values, but some states may choose to define different code/value combinations.


---

**Name**: Patient_DOB

**Type**: DATE

**PII**: Yes

**HL7 Field**: PID-7

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: Patient_gender

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

**Documentation**:

The patient's gender. There is a valueset defined based on the values in PID-8-1, but downstream consumers are free to define their own accepted values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: Patient_ethnicity

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

**Documentation**:

The patient's ethnicity. There is a valueset defined based on the values in PID-22, but downstream
consumers are free to define their own values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: Patient_street

**Type**: STREET

**PII**: Yes

**HL7 Field**: PID-11-1

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: Patient_street_2

**Type**: STREET_OR_BLANK

**PII**: Yes

**HL7 Field**: PID-11-2

**Cardinality**: [0..1]

**Documentation**:

The patient's second address line

---

**Name**: Patient_city

**Type**: CITY

**PII**: Yes

**HL7 Field**: PID-11-3

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: Patient_state

**Type**: TABLE

**PII**: No

**HL7 Field**: PID-11-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: Patient_zip_code

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: PID-11-5

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: Patient_phone_number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Field**: PID-13

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: Patient_county

**Type**: TABLE_OR_BLANK

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: Patient_email

**Type**: EMAIL

**PII**: Yes

**HL7 Field**: PID-13-4

**Cardinality**: [0..1]

---

**Name**: Patient_role

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Processing_mode_code

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

**Documentation**:

P, D, or T for Production, Debugging, or Training

---

**Name**: Employed_in_healthcare

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

**Name**: Resident_congregate_setting

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

**Name**: First_test

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

**Name**: Symptomatic_for_disease

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

**Name**: Testing_lab_name

**Type**: TEXT

**PII**: No

**HL7 Fields**: ORC-2-2, OBR-2-2, ORC-3-2, OBR-3-2, OBX-23-1

**Cardinality**: [0..1]

**Documentation**:

The name of the laboratory which performed the test, can be the same as the sending facility name

---

**Name**: Testing_lab_CLIA

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**: OBX-15-1, OBX-23-10, ORC-3-3, OBR-3-3, OBR-2-3, ORC-2-3

**Cardinality**: [1..1]

**Documentation**:

CLIA Number from the laboratory that sends the message to DOH

An example of the ID is 03D2159846


---

**Name**: Testing_lab_street

**Type**: STREET

**PII**: No

**HL7 Field**: OBX-24-1

**Cardinality**: [0..1]

---

**Name**: Testing_lab_street_2

**Type**: STREET_OR_BLANK

**PII**: No

**HL7 Field**: OBX-24-2

**Cardinality**: [0..1]

---

**Name**: Testing_lab_city

**Type**: CITY

**PII**: No

**HL7 Field**: OBX-24-3

**Cardinality**: [0..1]

---

**Name**: Testing_lab_state

**Type**: TABLE

**PII**: No

**HL7 Field**: OBX-24-4

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

---

**Name**: Testing_lab_zip_code

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: OBX-24-5

**Cardinality**: [0..1]

---

**Name**: Testing_lab_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Testing_lab_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: Organization_name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

For cases where organization owns many facilities (eg, a large hospital system)

---

**Name**: Ordering_facility_name

**Type**: TEXT

**PII**: No

**HL7 Field**: ORC-21-1

**Cardinality**: [0..1]

**Documentation**:

The name of the facility which the test was ordered from

---

**Name**: Ordering_facility_street

**Type**: STREET

**PII**: No

**HL7 Field**: ORC-22-1

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: Ordering_facility_street_2

**Type**: STREET_OR_BLANK

**PII**: No

**HL7 Field**: ORC-22-2

**Cardinality**: [0..1]

**Documentation**:

The secondary address of the facility which the test was ordered from

---

**Name**: Ordering_facility_city

**Type**: CITY

**PII**: No

**HL7 Field**: ORC-22-3

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: Ordering_facility_state

**Type**: TABLE

**PII**: No

**HL7 Field**: ORC-22-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---

**Name**: Ordering_facility_zip_code

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: ORC-22-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: Ordering_facility_phone_number

**Type**: TELEPHONE

**PII**: No

**HL7 Field**: ORC-23

**Cardinality**: [0..1]

**Documentation**:

The phone number of the facility which the test was ordered from

---

**Name**: Ordering_facility_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: Ordering_facility_email

**Type**: EMAIL

**PII**: No

**HL7 Field**: ORC-23-4

**Cardinality**: [0..1]

---

**Name**: Ordering_provider_ID

**Type**: ID_NPI

**PII**: No

**HL7 Fields**: ORC-12-1, OBR-16-1

**Cardinality**: [0..1]

**Documentation**:

The ordering provider’s National Provider Identifier

---

**Name**: Ordering_provider_last_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**: ORC-12-2, OBR-16-2

**Cardinality**: [0..1]

**Documentation**:

The last name of provider who ordered the test

---

**Name**: Ordering_provider_first_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**: ORC-12-3, OBR-16-3

**Cardinality**: [0..1]

**Documentation**:

The first name of the provider who ordered the test

---

**Name**: Ordering_provider_street

**Type**: STREET

**PII**: Yes

**HL7 Field**: ORC-24-1

**Cardinality**: [0..1]

**Documentation**:

The street address of the provider

---

**Name**: Ordering_provider_street_2

**Type**: STREET_OR_BLANK

**PII**: Yes

**HL7 Field**: ORC-24-2

**Cardinality**: [0..1]

**Documentation**:

The street second address of the provider

---

**Name**: Ordering_provider_city

**Type**: CITY

**PII**: Yes

**HL7 Field**: ORC-24-3

**Cardinality**: [0..1]

**Documentation**:

The city of the provider

---

**Name**: Ordering_provider_state

**Type**: TABLE

**PII**: No

**HL7 Field**: ORC-24-4

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the provider

---

**Name**: Ordering_provider_zip_code

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: ORC-24-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the provider

---

**Name**: Ordering_provider_phone_number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Fields**: ORC-14, OBR-17

**Cardinality**: [0..1]

**Documentation**:

The phone number of the provider

---

**Name**: Ordering_provider_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: reporting_facility_name

**Type**: TEXT

**PII**: No

**HL7 Field**: MSH-4-1

**Cardinality**: [0..1]

**Documentation**:

The reporting facility's name

---

**Name**: reporting_facility_clia

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**: MSH-4-2, SPM-2-1-3, SPM-2-2-3, PID-3-4-2, PID-3-6-2

**Cardinality**: [0..1]

**Documentation**:

The reporting facility's CLIA

---
