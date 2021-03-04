
### Schema:         pdi-covid-19
#### Description:   HL7 data elements resulting from pdi-covid-19

---

**Name**: employed_in_healthcare

**Type**: CODE

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

**Name**: first_test

**Type**: CODE

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

**Name**: illness_onset_date

**Type**: DATE

**HL7 Field**: AOE

**LOINC Code**: 65222-2

**Cardinality**: [0..1]

---

**Name**: resident_congregate_setting

**Type**: CODE

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

**Name**: symptomatic_for_disease

**Type**: CODE

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

**Name**: sending_application

**Type**: HD

**HL7 Field**: MSH-3

**Cardinality**: [0..1]

---

**Name**: reporting_facility_name

**Type**: TEXT

**HL7 Field**: MSH-4-1

**Cardinality**: [0..1]

**Documentation**:

The reporting facility's name

---

**Name**: reporting_facility_clia

**Type**: ID_CLIA

**HL7 Field**: MSH-4-2

**Cardinality**: [0..1]

**Documentation**:

The reporting facility's CLIA

---

**Name**: receiving_application

**Type**: HD

**HL7 Field**: MSH-5

**Cardinality**: [0..1]

**Documentation**:

The receiving application for the message (specified by the receiver)

---

**Name**: receiving_facility

**Type**: HD

**HL7 Field**: MSH-6

**Cardinality**: [0..1]

**Documentation**:

The receiving facility for the message (specified by the receiver)

---

**Name**: message_id

**Type**: ID

**HL7 Field**: MSH-10

**Cardinality**: [1..1]

**Documentation**:

unique id to track the usage of the message

---

**Name**: processing_mode_code

**Type**: CODE

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

**Name**: message_profile_id

**Type**: EI

**HL7 Field**: MSH-21

**Cardinality**: [0..1]

**Documentation**:

The message profile identifer

---

**Name**: ordered_test_code

**Type**: TABLE

**HL7 Field**: OBR-4-1

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Ordered LOINC Code

---

**Name**: ordered_test_name

**Type**: TABLE

**HL7 Field**: OBR-4-2

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Ordered LOINC Long Name

---

**Name**: ordered_test_system_abbr

**Type**: TEXT

**HL7 Field**: OBR-4-3

**Cardinality**: [0..1]

---

**Name**: date_result_released

**Type**: DATETIME

**HL7 Field**: OBR-22

**Cardinality**: [0..1]

---

**Name**: result_format

**Type**: TEXT

**HL7 Field**: OBX-2

**Cardinality**: [0..1]

---

**Name**: test_performed_code

**Type**: TABLE

**HL7 Field**: OBX-3-1

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Performed LOINC Code

**Documentation**:

The LOINC code of the test performed. This is a standardized coded value describing the test

---

**Name**: test_performed_name

**Type**: TABLE

**HL7 Field**: OBX-3-2

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Performed LOINC Long Name

**Documentation**:

The LOINC description of the test performed as related to the LOINC code.

---

**Name**: test_performed_system_abbr

**Type**: TEXT

**HL7 Field**: OBX-3-3

**Cardinality**: [0..1]

---

**Name**: test_result

**Type**: CODE

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

**Documentation**:

The result of the test performed. For IgG, IgM and CT results that give a numeric value put that here.

---

**Name**: abnormal_flag

**Type**: CODE

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

**Name**: device_id

**Type**: TABLE

**HL7 Field**: OBX-17-1

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Documentation**:

Device_id is a generated value for the OBX-17 field. It is based on the device model and the LIVD table.

---

**Name**: device_id_type

**Type**: TABLE

**HL7 Field**: OBX-17-3

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Documentation**:

Device_id_type is a generated value for the OBX-17 field. It is based on the device model and the LIVD table.

---

**Name**: equipment_model_id

**Type**: TABLE

**HL7 Field**: OBX-18-1

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Equipment UID

---

**Name**: equipment_model_id_type

**Type**: TABLE

**HL7 Field**: OBX-18-3

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Equipment UID Type

---

**Name**: test_result_date

**Type**: DATETIME

**HL7 Field**: OBX-19

**Cardinality**: [0..1]

---

**Name**: testing_lab_name

**Type**: TEXT

**HL7 Field**: OBX-23-1

**Cardinality**: [0..1]

**Documentation**:

The name of the laboratory which performed the test, can be the same as the sending facility name

---

**Name**: testing_lab_id_assigner

**Type**: HD

**HL7 Field**: OBX-23-6

**Cardinality**: [0..1]

---

**Name**: testing_lab_clia

**Type**: ID_CLIA

**HL7 Field**: OBX-23-10

**Cardinality**: [1..1]

**Documentation**:

CLIA Number from the laboratory that sends the message to DOH

An example of the ID is 03D2159846


---

**Name**: testing_lab_street

**Type**: STREET

**HL7 Field**: OBX-24-1

**Cardinality**: [0..1]

---

**Name**: testing_lab_street2

**Type**: STREET_OR_BLANK

**HL7 Field**: OBX-24-2

**Cardinality**: [0..1]

---

**Name**: testing_lab_city

**Type**: CITY

**HL7 Field**: OBX-24-3

**Cardinality**: [0..1]

---

**Name**: testing_lab_state

**Type**: TABLE

**HL7 Field**: OBX-24-4

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

---

**Name**: testing_lab_zip_code

**Type**: POSTAL_CODE

**HL7 Field**: OBX-24-5

**Cardinality**: [0..1]

---

**Name**: testing_lab_county_code

**Type**: TABLE

**HL7 Field**: OBX-24-9

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

---

**Name**: placer_order_id

**Type**: ID

**HL7 Fields**: ORC-2-1, OBR-2-1

**Cardinality**: [0..1]

---

**Name**: filler_order_id

**Type**: ID

**HL7 Field**: ORC-3-1

**Cardinality**: [0..1]

---

**Name**: ordering_provider_id

**Type**: ID_NPI

**HL7 Fields**: ORC-12-1, OBR-16-1

**Cardinality**: [0..1]

**Documentation**:

The ordering provider’s National Provider Identifier

---

**Name**: ordering_provider_first_name

**Type**: PERSON_NAME

**HL7 Fields**: ORC-12-3, OBR-16-3

**Cardinality**: [0..1]

**Documentation**:

The first name of the provider who ordered the test

---

**Name**: ordering_provider_id_authority

**Type**: HD

**HL7 Fields**: ORC-12-9, OBR-16-9

**Cardinality**: [0..1]

**Documentation**:

Usually the OID for CMS

---

**Name**: ordering_provider_id_authority_type

**Type**: TEXT

**HL7 Fields**: ORC-12-13, OBR-16-13

**Cardinality**: [0..1]

**Documentation**:

Usually NPI

---

**Name**: ordering_provider_phone_number

**Type**: TELEPHONE

**HL7 Fields**: ORC-14, OBR-17

**Cardinality**: [0..1]

**Documentation**:

The phone number of the provider

---

**Name**: order_test_date

**Type**: DATE

**HL7 Field**: ORC-15

**Cardinality**: [0..1]

---

**Name**: ordering_facility_name

**Type**: TEXT

**HL7 Field**: ORC-21-1

**Cardinality**: [0..1]

**Documentation**:

The name of the facility which the test was ordered from

---

**Name**: ordering_facility_street

**Type**: STREET

**HL7 Field**: ORC-22-1

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: ordering_facility_street2

**Type**: STREET_OR_BLANK

**HL7 Field**: ORC-22-2

**Cardinality**: [0..1]

**Documentation**:

The secondary address of the facility which the test was ordered from

---

**Name**: ordering_facility_city

**Type**: CITY

**HL7 Field**: ORC-22-3

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: ordering_facility_state

**Type**: TABLE

**HL7 Field**: ORC-22-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---

**Name**: ordering_facility_zip_code

**Type**: POSTAL_CODE

**HL7 Field**: ORC-22-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: ordering_facility_county_code

**Type**: TABLE

**HL7 Field**: ORC-22-9

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

---

**Name**: ordering_facility_email

**Type**: EMAIL

**HL7 Field**: ORC-23-4

**Cardinality**: [0..1]

---

**Name**: ordering_facility_phone_number

**Type**: TELEPHONE

**HL7 Field**: ORC-23

**Cardinality**: [1..1]

**Documentation**:

The phone number of the facility which the test was ordered from

---

**Name**: ordering_provider_street

**Type**: STREET

**HL7 Field**: ORC-24-1

**Cardinality**: [0..1]

**Documentation**:

The street address of the provider

---

**Name**: ordering_provider_street2

**Type**: STREET_OR_BLANK

**HL7 Field**: ORC-24-2

**Cardinality**: [0..1]

**Documentation**:

The street second address of the provider

---

**Name**: ordering_provider_city

**Type**: CITY

**HL7 Field**: ORC-24-3

**Cardinality**: [0..1]

**Documentation**:

The city of the provider

---

**Name**: ordering_provider_state

**Type**: TABLE

**HL7 Field**: ORC-24-4

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the provider

---

**Name**: ordering_provider_zip_code

**Type**: POSTAL_CODE

**HL7 Field**: ORC-24-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the provider

---

**Name**: ordering_provider_county_code

**Type**: TABLE

**HL7 Field**: ORC-24-9

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: patient_id

**Type**: TEXT

**HL7 Field**: PID-3-1

**Cardinality**: [0..1]

---

**Name**: patient_id_assigner

**Type**: HD

**HL7 Field**: PID-3-4

**Cardinality**: [0..1]

---

**Name**: patient_id_type

**Type**: TEXT

**HL7 Field**: PID-3-5

**Cardinality**: [0..1]

---

**Name**: patient_last_name

**Type**: PERSON_NAME

**HL7 Field**: PID-5-1

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: patient_first_name

**Type**: PERSON_NAME

**HL7 Field**: PID-5-2

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: patient_middle_initial

**Type**: PERSON_NAME

**HL7 Field**: PID-5-3

**Cardinality**: [0..1]

---

**Name**: patient_middle_name

**Type**: PERSON_NAME

**HL7 Field**: PID-5-3

**Cardinality**: [0..1]

---

**Name**: patient_suffix

**Type**: PERSON_NAME

**HL7 Field**: PID-5-4

**Cardinality**: [0..1]

---

**Name**: patient_name_type_code

**Type**: TEXT

**HL7 Field**: PID-5-7

**Cardinality**: [0..1]

---

**Name**: patient_dob

**Type**: DATE

**HL7 Field**: PID-7

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: patient_gender

**Type**: CODE

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

**Name**: patient_race

**Type**: CODE

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

**Name**: patient_street

**Type**: STREET

**HL7 Field**: PID-11-1

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: patient_street2

**Type**: STREET_OR_BLANK

**HL7 Field**: PID-11-2

**Cardinality**: [0..1]

**Documentation**:

The patient's second address line

---

**Name**: patient_city

**Type**: CITY

**HL7 Field**: PID-11-3

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: patient_state

**Type**: TABLE

**HL7 Field**: PID-11-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: patient_zip_code

**Type**: POSTAL_CODE

**HL7 Field**: PID-11-5

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: patient_county_code

**Type**: TABLE

**HL7 Field**: PID-11-9

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

---

**Name**: patient_email

**Type**: EMAIL

**HL7 Field**: PID-13-4

**Cardinality**: [0..1]

---

**Name**: patient_phone_number

**Type**: TELEPHONE

**HL7 Field**: PID-13

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: patient_ethnicity

**Type**: CODE

**HL7 Field**: PID-22

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
H|Hispanic or Latino
N|Non Hispanic or Latino
U|Unknown

**Documentation**:

The patient's ethnicity. There is a valueset defined based on the values in PID-22, but downstream consumers are free to define their own values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: patient_died

**Type**: CODE

**HL7 Field**: PID-30-1

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

---

**Name**: testing_lab_specimen_id

**Type**: ID

**HL7 Field**: SPM-2-1

**Cardinality**: [0..1]

**Documentation**:

The specimen-id from the testing lab

---

**Name**: specimen_type

**Type**: CODE

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

**HL7 Field**: SPM-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
119297000|Blood specimen (specimen)
71836000|Nasopharyngeal structure (body structure)
45206002|Nasal structure (body structure)

---

**Name**: specimen_collection_date_time

**Type**: DATETIME

**HL7 Fields**: SPM-17-1, OBR-7, OBR-8, OBX-14

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: testing_lab_specimen_received_datetime

**Type**: DATETIME

**HL7 Field**: SPM-18-1

**Cardinality**: [0..1]

---
