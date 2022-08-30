
### Schema: tpca/tpca-covid-19
### Topic: covid-19
### Tracking Element: Sample ID (specimen_id)
### Base On: [covid-19](./covid-19.md)
### Extends: none
#### Description: A COVID-19 schema for TPCA working through A6

---

**Name**: Appr Date

**ReportStream Internal Name**: appr_date

**Type**: DATE

**PII**: No

**Format**: M/d/yyyy

**Cardinality**: [0..1]

---

**Name**: HCW?

**ReportStream Internal Name**: employed_in_healthcare

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 95418-0

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|Yes|HL7
N|No|HL7
UNK|Unknown|NULLFL

**Documentation**:

Is the patient employed in health care?

---

**Name**: 1st Test?

**ReportStream Internal Name**: first_test

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 95417-2

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|Yes|HL7
N|No|HL7
UNK|Unknown|NULLFL

**Documentation**:

Is this the patient's first test for this condition?

---

**Name**: Hosp?

**ReportStream Internal Name**: hospitalized

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 77974-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|Yes|HL7
N|No|HL7
UNK|Unknown|NULLFL

**Documentation**:

Is the patient hospitalized?

---

**Name**: ICU?

**ReportStream Internal Name**: icu

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 95420-6

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|Yes|HL7
N|No|HL7
UNK|Unknown|NULLFL

**Documentation**:

Is the patient in the ICU?

---

**Name**: Sympt Date

**ReportStream Internal Name**: illness_onset_date

**Type**: DATE

**PII**: No

**Format**: M/d/yyyy

**LOINC Code**: 65222-2

**Cardinality**: [0..1]

---

**Name**: Provider

**ReportStream Internal Name**: ordering_provider

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Age

**ReportStream Internal Name**: patient_age_and_units

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: City

**ReportStream Internal Name**: patient_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: DOB

**ReportStream Internal Name**: patient_dob

**Type**: DATE

**PII**: Yes

**Format**: M/d/yyyy

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: Patient ID

**ReportStream Internal Name**: patient_id

**Type**: TEXT

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The ID for the patient within one of the reporting entities for this lab result. It could be the
the patient ID from the testing lab, the oder placer, the ordering provider, or even within the PRIME system itself.


---

**Name**: Name

**ReportStream Internal Name**: patient_name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Phone

**ReportStream Internal Name**: patient_phone_number_raw

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Race

**ReportStream Internal Name**: patient_race

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
1002-5|American Indian or Alaska Native|HL7
2028-9|Asian|HL7
2054-5|Black or African American|HL7
2076-8|Native Hawaiian or Other Pacific Islander|HL7
2106-3|White|HL7
2131-1|Other|HL7
UNK|Unknown|NULLFL
ASKU|Asked, but unknown|NULLFL
1002-5|American Indian or Alaska Native|HL7
2028-9|Asian|HL7
2054-5|Black or African American|HL7
2054-5|Black or African American|HL7
2076-8|Native Hawaiian or Other Pacific Islander|HL7
2106-3|White|HL7
2016-3|Caucasian|HL7
2131-1|Other|HL7
UNK|Unknown|NULLFL
N/A|N/A|HL7
ASKU|Asked, but unknown|NULLFL

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
1002-5|American Indian or Alaska Native|HL7
2028-9|Asian|HL7
2054-5|Black or African American|HL7
2054-5|Black|HL7
2076-8|Native Hawaiian or Other Pacific Islander|HL7
2106-3|White|HL7
2016-3|Caucasian|HL7
2131-1|Other|HL7
UNK|Undefined|NULLFL
N/A|N/A|HL7
ASKU|Asked, but unknown|NULLFL

**Documentation**:

The patient's race. There is a common valueset defined for race values, but some states may choose to define different code/value combinations.


---

**Name**: State

**ReportStream Internal Name**: patient_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: Address

**ReportStream Internal Name**: patient_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: Zip

**ReportStream Internal Name**: patient_zip_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: zip-code-data

**Table Column**: zipcode

**Documentation**:

The patient's zip code

---

**Name**: Preg?

**ReportStream Internal Name**: pregnant

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**LOINC Code**: 82810-3

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
77386006|Pregnant|SNOMED_CT
60001007|Not Pregnant|SNOMED_CT
261665006|Unknown|SNOMED_CT
77386006|Pregnant|SNOMED_CT
60001007|Not Pregnant|SNOMED_CT
261665006|Unknown|SNOMED_CT

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
77386006|Y|SNOMED_CT
60001007|N|SNOMED_CT
261665006|Unknown|SNOMED_CT

**Documentation**:

Is the patient pregnant?

---

**Name**: Nsg Home?

**ReportStream Internal Name**: resident_congregate_care_setting

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Sample Type

**ReportStream Internal Name**: sample_type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Draw Date

**ReportStream Internal Name**: specimen_collection_date_time

**Type**: DATETIME

**PII**: No

**Format**: M/d/yyyy

**HL7 Fields**

- [OBR-7](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.7)
- [OBR-8](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.8)
- [OBX-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.14)
- [SPM-17-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.17.1)

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: Sample ID

**ReportStream Internal Name**: specimen_id

**Type**: EI

**PII**: No

**HL7 Fields**

- [SPM-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2)

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2) 

**Documentation**:

A unique code for this specimen

---

**Name**: Symptomatic

**ReportStream Internal Name**: symptomatic_for_disease

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 95419-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|Yes|HL7
N|No|HL7
UNK|Unknown|NULLFL

**Documentation**:

Is the patient symptomatic?

---

**Name**: LOINC

**ReportStream Internal Name**: test_performed_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Performed LOINC Code

**Documentation**:

The LOINC code of the test performed. This is a standardized coded value describing the test

---

**Name**: LOINC Test Descr

**ReportStream Internal Name**: test_performed_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: SNOMED Rslt Code

**ReportStream Internal Name**: test_result

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
260373001|Detected|SNOMED_CT
260415000|Not detected|SNOMED_CT
720735008|Presumptive positive|SNOMED_CT
10828004|Positive|SNOMED_CT
42425007|Equivocal|SNOMED_CT
260385009|Negative|SNOMED_CT
895231008|Not detected in pooled specimen|SNOMED_CT
462371000124108|Detected in pooled specimen|SNOMED_CT
419984006|Inconclusive|SNOMED_CT
125154007|Specimen unsatisfactory for evaluation|SNOMED_CT
455371000124106|Invalid result|SNOMED_CT
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)|SNOMED_CT
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)|SNOMED_CT
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)|SNOMED_CT
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)|SNOMED_CT
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)|SNOMED_CT
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)|SNOMED_CT
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)|SNOMED_CT
373121007|Test not done|SNOMED_CT
82334004|Indeterminate|SNOMED_CT

**Documentation**:

The result of the test performed. For IgG, IgM and CT results that give a numeric value put that here.

---

**Name**: Result Descr

**ReportStream Internal Name**: test_result_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: abnormal_flag

**ReportStream Internal Name**: abnormal_flag

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
A|Abnormal (applies to non-numeric results)|HL7
&#62;|Above absolute high-off instrument scale|HL7
H|Above high normal|HL7
HH|Above upper panic limits|HL7
AC|Anti-complementary substances present|HL7
<|Below absolute low-off instrument scale|HL7
L|Below low normal|HL7
LL|Below lower panic limits|HL7
B|Better--use when direction not relevant|HL7
TOX|Cytotoxic substance present|HL7
DET|Detected|HL7
IND|Indeterminate|HL7
I|Intermediate. Indicates for microbiology susceptibilities only.|HL7
MS|Moderately susceptible. Indicates for microbiology susceptibilities only.|HL7
NEG|Negative|HL7
null|No range defined, or normal ranges don't apply|HL7
NR|Non-reactive|HL7
N|Normal (applies to non-numeric results)|HL7
ND|Not Detected|HL7
POS|Positive|HL7
QCF|Quality Control Failure|HL7
RR|Reactive|HL7
R|Resistant. Indicates for microbiology susceptibilities only.|HL7
D|Significant change down|HL7
U|Significant change up|HL7
S|Susceptible. Indicates for microbiology susceptibilities only.|HL7
AA|Very abnormal (applies to non-numeric units, analogous to panic limits for numeric units)|HL7
VS|Very susceptible. Indicates for microbiology susceptibilities only.|HL7
WR|Weakly reactive|HL7
W|Worse--use when direction not relevant|HL7

**Documentation**:

This field is generated based on the normalcy status of the result. A = abnormal; N = normal

---

**Name**: equipment_model_name

**ReportStream Internal Name**: equipment_model_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2

**Table Column**: Model

---

**Name**: message_id

**ReportStream Internal Name**: message_id

**Type**: ID

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

unique id to track the usage of the message

---

**Name**: message_profile_id

**ReportStream Internal Name**: message_profile_id

**Type**: EI

**PII**: No

**Default Value**: PHLabReport-NoAck

**Cardinality**: [0..1]

**Documentation**:

The message profile identifer

---

**Name**: ordering_facility_city

**ReportStream Internal Name**: ordering_facility_city

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: ordering_facility_county

**ReportStream Internal Name**: ordering_facility_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: ordering_facility_phone_number

**ReportStream Internal Name**: ordering_facility_phone_number

**Type**: TELEPHONE

**PII**: No

**Default Value**: 8509426624:1:

**Cardinality**: [0..1]

**Documentation**:

The phone number of the facility which the test was ordered from

---

**Name**: ordering_facility_state

**ReportStream Internal Name**: ordering_facility_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---

**Name**: ordering_facility_street

**ReportStream Internal Name**: ordering_facility_street

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: ordering_facility_zip_code

**ReportStream Internal Name**: ordering_facility_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: ordering_provider_first_name

**ReportStream Internal Name**: ordering_provider_first_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.3)
- [ORC-12-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.3)

**Cardinality**: [0..1]

**Documentation**:

The first name of the provider who ordered the test

---

**Name**: ordering_provider_last_name

**ReportStream Internal Name**: ordering_provider_last_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.2)
- [ORC-12-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.2)

**Cardinality**: [0..1]

**Documentation**:

The last name of provider who ordered the test

---

**Name**: patient_age

**ReportStream Internal Name**: patient_age

**Type**: NUMBER

**PII**: Yes

**LOINC Code**: 30525-0

**Cardinality**: [0..1]

---

**Name**: patient_age_units

**ReportStream Internal Name**: patient_age_units

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
min|minutes|UCUM
h|hours|UCUM
d|days|UCUM
wk|weeks|UCUM
mo|months|UCUM
a|years|UCUM

**Documentation**:

Always filled when `patient_age` is filled

---

**Name**: patient_county

**ReportStream Internal Name**: patient_county

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: zip-code-data

**Table Column**: county

---

**Name**: patient_first_name

**ReportStream Internal Name**: patient_first_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: patient_last_name

**ReportStream Internal Name**: patient_last_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: patient_phone_number

**ReportStream Internal Name**: patient_phone_number

**Type**: TELEPHONE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: sending_application

**ReportStream Internal Name**: sending_application

**Type**: HD

**PII**: No

**Default Value**: CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO

**Cardinality**: [0..1]

**Documentation**:

The name and OID for the application sending information to the receivers


---

**Name**: test_authorized_for_home

**ReportStream Internal Name**: test_authorized_for_home

**Type**: TABLE

**PII**: No

**Default Value**: N

**Cardinality**: [0..1]


**Reference URL**:
[https://www.fda.gov/news-events/fda-newsroom/press-announcements](https://www.fda.gov/news-events/fda-newsroom/press-announcements) 

**Table**: LIVD-SARS-CoV-2

**Table Column**: is_home

**Documentation**:

Is the test authorized for home use by the FDA (Y, N, UNK)

---

**Name**: test_authorized_for_otc

**ReportStream Internal Name**: test_authorized_for_otc

**Type**: TABLE

**PII**: No

**Default Value**: N

**Cardinality**: [0..1]


**Reference URL**:
[https://www.fda.gov/news-events/fda-newsroom/press-announcements](https://www.fda.gov/news-events/fda-newsroom/press-announcements) 

**Table**: LIVD-SARS-CoV-2

**Table Column**: is_otc

**Documentation**:

Is the test authorized for over-the-counter purchase by the FDA (Y, N, UNK)

---

**Name**: test_authorized_for_unproctored

**ReportStream Internal Name**: test_authorized_for_unproctored

**Type**: TABLE

**PII**: No

**Default Value**: N

**Cardinality**: [0..1]


**Reference URL**:
[https://www.fda.gov/news-events/fda-newsroom/press-announcements](https://www.fda.gov/news-events/fda-newsroom/press-announcements) 

**Table**: LIVD-SARS-CoV-2

**Table Column**: is_unproctored

**Documentation**:

Is the test authorized for unproctored administration by the FDA (Y, N, UNK)

---

**Name**: testing_lab_city

**ReportStream Internal Name**: testing_lab_city

**Type**: CITY

**PII**: No

**Default Value**: Talahassee

**Cardinality**: [0..1]

**Documentation**:

The city of the testing lab

---

**Name**: testing_lab_clia

**ReportStream Internal Name**: testing_lab_clia

**Type**: ID_CLIA

**PII**: No

**Default Value**: 10D0270039

**HL7 Fields**

- [OBR-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.3)
- [OBR-3-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.3)
- [OBX-15-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.15.1)
- [OBX-23-10](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.23.10)
- [ORC-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.3)
- [ORC-3-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.3)

**Cardinality**: [1..1]

**Documentation**:

CLIA Number from the laboratory that sends the message to DOH

An example of the ID is 03D2159846


---

**Name**: testing_lab_county

**ReportStream Internal Name**: testing_lab_county

**Type**: TABLE

**PII**: No

**Default Value**: Leon

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

**Documentation**:

The text value for the testing lab county. This is used to do the lookup in the FIPS dataset.

---

**Name**: testing_lab_name

**ReportStream Internal Name**: testing_lab_name

**Type**: TEXT

**PII**: No

**Default Value**: TPCA Lab and Diagnostic Imaging Center

**HL7 Fields**

- [OBR-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.2)
- [OBR-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.2)
- [OBX-15-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.15.2)
- [OBX-23-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.23.1)
- [ORC-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.2)
- [ORC-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.2)
- [PID-3-4-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4.1)

**Cardinality**: [0..1]

**Documentation**:

The name of the laboratory which performed the test, can be the same as the sending facility name

---

**Name**: testing_lab_phone_number

**ReportStream Internal Name**: testing_lab_phone_number

**Type**: TELEPHONE

**PII**: No

**Default Value**: 8509426624:1:

**Cardinality**: [0..1]

**Documentation**:

The phone number of the testing lab

---

**Name**: testing_lab_state

**ReportStream Internal Name**: testing_lab_state

**Type**: TABLE

**PII**: No

**Default Value**: FL

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state for the testing lab

---

**Name**: testing_lab_street

**ReportStream Internal Name**: testing_lab_street

**Type**: STREET

**PII**: No

**Default Value**: 1803 Miccosukee Commons Drive

**Cardinality**: [0..1]

**Documentation**:

The street address for the testing lab

---

**Name**: testing_lab_zip_code

**ReportStream Internal Name**: testing_lab_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Default Value**: 32308

**Cardinality**: [0..1]

**Documentation**:

The postal code for the testing lab

---
