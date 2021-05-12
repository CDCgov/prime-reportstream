
### Schema:         strac/strac-covid-19
#### Description:   STRAC POC COVID-19 flat file

---

**Name**: reporting_facility_name

**Type**: TEXT

**PII**: No

**HL7 Field**: MSH-4-1

**Cardinality**: [0..1]

**Documentation**:

The reporting facility's name

---

**Name**: reporting_facility_CLIA

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**: MSH-4-2, SPM-2-1-3, SPM-2-2-3, PID-3-4-2, PID-3-6-2

**Cardinality**: [0..1]

**Documentation**:

The reporting facility's CLIA

---

**Name**: Ordering_Facility_Name

**Type**: TEXT

**PII**: No

**HL7 Field**: ORC-21-1

**Cardinality**: [0..1]

**Documentation**:

The name of the facility which the test was ordered from

---

**Name**: Ordering_Facility_Phone_Number

**Type**: TELEPHONE

**PII**: No

**HL7 Field**: ORC-23

**Cardinality**: [0..1]

**Documentation**:

The phone number of the facility which the test was ordered from

---

**Name**: Ordering_Facility_Address

**Type**: STREET

**PII**: No

**HL7 Field**: ORC-22-1

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: Ordering_Facility_City

**Type**: CITY

**PII**: No

**HL7 Field**: ORC-22-3

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: Ordering_Facility_State

**Type**: TABLE

**PII**: No

**HL7 Field**: ORC-22-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---

**Name**: Ordering_Facility_County

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: Ordering_Facility_ZIP

**Type**: POSTAL_CODE

**PII**: No

**Format**: $zipFive

**HL7 Field**: ORC-22-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: Ordering_Provider_Address

**Type**: STREET

**PII**: Yes

**HL7 Field**: ORC-24-1

**Cardinality**: [0..1]

**Documentation**:

The street address of the provider

---

**Name**: Ordering_Provider_State

**Type**: TABLE

**PII**: No

**HL7 Field**: ORC-24-4

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the provider

---

**Name**: Ordering_Provider_ZIP

**Type**: POSTAL_CODE

**PII**: No

**Format**: $zipFive

**HL7 Field**: ORC-24-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the provider

---

**Name**: id

**Type**: ID

**PII**: No

**HL7 Fields**: ORC-2-1, OBR-2-1

**Cardinality**: [0..1]

---

**Name**: kit_id

**Type**: ID

**PII**: No

**HL7 Fields**: ORC-3-1, SPM-2-2, OBR-3-1

**Cardinality**: [0..1]

**Documentation**:

Accension number

---

**Name**: patient_appt_datetime

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

---

**Name**: patient_first_name

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-2

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: patient_last_name

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-1

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: patient_address

**Type**: STREET

**PII**: Yes

**HL7 Field**: PID-11-1

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: patient_address2

**Type**: STREET_OR_BLANK

**PII**: Yes

**HL7 Field**: PID-11-2

**Cardinality**: [0..1]

**Documentation**:

The patient's second address line

---

**Name**: patient_callback_number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Field**: PID-13

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: patient_city

**Type**: CITY

**PII**: Yes

**HL7 Field**: PID-11-3

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: patient_county

**Type**: TABLE_OR_BLANK

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: patient_dl

**Type**: ID_DLN

**PII**: Yes

**HL7 Field**: PID-20-1

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

**Name**: patient_email

**Type**: EMAIL

**PII**: Yes

**HL7 Field**: PID-13-4

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

**Documentation**:

The patient's ethnicity. There is a valueset defined based on the values in PID-22, but downstream
consumers are free to define their own values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: patient_pregnant

**Type**: CODE

**PII**: No

**Format**: $display

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

**Name**: patient_race

**Type**: CODE

**PII**: No

**Format**: $code

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

**Name**: patient_results

**Type**: DATETIME

**PII**: No

**HL7 Field**: OBX-19

**Cardinality**: [0..1]

---

**Name**: patient_sex

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

**Name**: patient_symptom_onset

**Type**: DATE

**PII**: No

**HL7 Field**: AOE

**LOINC Code**: 65222-2

**Cardinality**: [0..1]

---

**Name**: patient_zip

**Type**: POSTAL_CODE

**PII**: No

**Format**: $zipFive

**HL7 Field**: PID-11-5

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: patient_positive

**Type**: CODE

**PII**: No

**Format**: $alt

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
260373001|Detected
260415000|Not detected
455371000124106|Invalid result
720735008|Presumptive positive
10828004|Positive
42425007|Equivocal
260385009|Negative
895231008|Not detected in pooled specimen
462371000124108|Detected in pooled specimen
419984006|Inconclusive
125154007|Specimen unsatisfactory for evaluation
840539006|Disease caused by sever acute respitory syndrome coronavirus 2 (disorder)
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)

**Alt Value Sets**

Code | Display
---- | -------
260373001|positive
260415000|negative
455371000124106|invalid
720735008|Presumptive positive
10828004|Positive
42425007|Equivocal
260385009|Negative
895231008|Not detected in pooled specimen
462371000124108|Detected in pooled specimen
419984006|Inconclusive
125154007|Specimen unsatisfactory for evaluation
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

**Name**: equipment_model_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Model

---

**Name**: ordered_test_code

**Type**: TABLE

**PII**: No

**HL7 Field**: OBR-4-1

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Ordered LOINC Code

---

**Name**: ordered_test_name

**Type**: TABLE

**PII**: No

**HL7 Field**: OBR-4-2

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Ordered LOINC Long Name

---

**Name**: ordered_test_system

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: order_test_date

**Type**: DATE

**PII**: No

**HL7 Field**: ORC-15

**Cardinality**: [0..1]

---

**Name**: reference_range

**Type**: TEXT

**PII**: No

**HL7 Field**: OBX-7

**Cardinality**: [0..1]

**Documentation**:

The reference range of the lab result, such as “Negative” or “Normal”. For IgG, IgM and CT results that provide a value you MUST fill out this filed.

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

**Name**: specimen_collection_date_time

**Type**: DATETIME

**PII**: No

**HL7 Fields**: SPM-17-1, OBR-7, OBR-8, OBX-14

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: specimen_received_date_time

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

**Documentation**:

This field is generated based on the normalcy status of the result. A = abnormal; N = normal

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

---

**Name**: testing_lab_name

**Type**: TEXT

**PII**: No

**HL7 Fields**: ORC-2-2, OBR-2-2, ORC-3-2, OBR-3-2, OBX-23-1

**Cardinality**: [0..1]

**Documentation**:

The name of the laboratory which performed the test, can be the same as the sending facility name

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

**Name**: message_id

**Type**: ID

**PII**: No

**HL7 Field**: MSH-10

**Cardinality**: [1..1]

**Documentation**:

unique id to track the usage of the message

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
