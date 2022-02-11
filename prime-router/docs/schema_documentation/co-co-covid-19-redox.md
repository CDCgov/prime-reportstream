
### Schema: co/co-covid-19-redox
### Topic: covid-19
### Tracking Element: none
### Base On: none
### Extends: [covid-19-redox](./covid-19-redox.md)
#### Description: Colorado Department of Public Health (CDPHE) REDOX messages

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
A|Abnormal (applies to non-numeric results)|HL7
N|Normal (applies to non-numeric results)|HL7

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
A|Abnormal|HL7
N|Normal|HL7

**Documentation**:

This field is generated based on the normalcy status of the result. A = abnormal; N = normal

---

**Name**: date_result_released

**ReportStream Internal Name**: date_result_released

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

---

**Name**: device_id

**ReportStream Internal Name**: device_id

**Type**: TABLE

**PII**: No

**HL7 Fields**

- [OBX-17-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.17.1)
- [OBX-17-9](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.17.9)

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2

**Documentation**:

Device_id is a generated value for the OBX-17 field. It is based on the device model and the LIVD table.

---

**Name**: employed_in_healthcare

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

**Name**: filler_order_id

**ReportStream Internal Name**: filler_order_id

**Type**: ID

**PII**: No

**HL7 Fields**

- [OBR-3-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.1)
- [ORC-3-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.1)
- [SPM-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.2)

**Cardinality**: [0..1]

**Documentation**:

Accension number

---

**Name**: first_test

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

**Name**: hospitalized

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

**Name**: icu

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

**Name**: illness_onset_date

**ReportStream Internal Name**: illness_onset_date

**Type**: TEXT

**PII**: No

**LOINC Code**: 65222-2

**Cardinality**: [0..1]

---

**Name**: order_test_date

**ReportStream Internal Name**: order_test_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordered_test_code

**ReportStream Internal Name**: ordered_test_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Ordered LOINC Code

---

**Name**: ordered_test_name

**ReportStream Internal Name**: ordered_test_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Ordered LOINC Long Name

---

**Name**: ordered_test_system

**ReportStream Internal Name**: ordered_test_system

**Type**: TEXT

**PII**: No

**Default Value**: LOINC

**Cardinality**: [0..1]

---

**Name**: ordering_facility_city

**ReportStream Internal Name**: ordering_facility_city

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: ordering_facility_country

**ReportStream Internal Name**: ordering_facility_country

**Type**: TEXT

**PII**: No

**Default Value**: USA

**Cardinality**: [0..1]

---

**Name**: ordering_facility_county

**ReportStream Internal Name**: ordering_facility_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: ordering_facility_email

**ReportStream Internal Name**: ordering_facility_email

**Type**: EMAIL

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordering_facility_name

**ReportStream Internal Name**: ordering_facility_name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The name of the facility which the test was ordered from

---

**Name**: ordering_facility_phone_number

**ReportStream Internal Name**: ordering_facility_phone_number

**Type**: TELEPHONE

**PII**: No

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

**Name**: ordering_provider_city

**ReportStream Internal Name**: ordering_provider_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The city of the provider

---

**Name**: ordering_provider_country

**ReportStream Internal Name**: ordering_provider_country

**Type**: TEXT

**PII**: No

**Default Value**: USA

**Cardinality**: [0..1]

---

**Name**: ordering_provider_county

**ReportStream Internal Name**: ordering_provider_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: ordering_provider_email

**ReportStream Internal Name**: ordering_provider_email

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

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

**Name**: ordering_provider_id

**ReportStream Internal Name**: ordering_provider_id

**Type**: ID_NPI

**PII**: No

**HL7 Fields**

- [OBR-16-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.1)
- [ORC-12-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.1)

**Cardinality**: [0..1]

**Documentation**:

The ordering provider’s National Provider Identifier

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

**Name**: ordering_provider_phone_number

**ReportStream Internal Name**: ordering_provider_phone_number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Fields**

- [OBR-17](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.17)
- [ORC-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.14)

**Cardinality**: [0..1]

**Documentation**:

The phone number of the provider

---

**Name**: ordering_provider_state

**ReportStream Internal Name**: ordering_provider_state

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the provider

---

**Name**: ordering_provider_street

**ReportStream Internal Name**: ordering_provider_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The street address of the provider

---

**Name**: ordering_provider_zip_code

**ReportStream Internal Name**: ordering_provider_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The zip code of the provider

---

**Name**: patient_city

**ReportStream Internal Name**: patient_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: patient_country

**ReportStream Internal Name**: patient_country

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: patient_county

**ReportStream Internal Name**: patient_county

**Type**: TABLE_OR_BLANK

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: patient_dob

**ReportStream Internal Name**: patient_dob

**Type**: DATE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: patient_drivers_license

**ReportStream Internal Name**: patient_drivers_license

**Type**: ID_DLN

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's drivers license number

---

**Name**: patient_email

**ReportStream Internal Name**: patient_email

**Type**: EMAIL

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_ethnicity

**ReportStream Internal Name**: patient_ethnicity

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
H|Hispanic or Latino|HL7
N|Non Hispanic or Latino|HL7
U|Unknown|HL7
H|Hispanic or Latino|HL7
N|Non Hispanic or Latino|HL7

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
H|true|HL7
N|false|HL7

**Documentation**:

The patient's ethnicity. There is a valueset defined based on the values in PID-22, but downstream
consumers are free to define their own values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: patient_first_name

**ReportStream Internal Name**: patient_first_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: patient_gender

**ReportStream Internal Name**: patient_gender

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
M|Male|HL7
F|Female|HL7
O|Other|HL7
A|Ambiguous|HL7
U|Unknown|HL7
N|Not applicable|HL7
M|Male|HL7
F|Female|HL7
O|Other|HL7
A|Ambiguous|HL7
U|Unknown|HL7
N|Not applicable|HL7

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
M|Male|HL7
F|Female|HL7
O|Other|HL7
A|Nonbinary|HL7
U|Unknown|HL7
N|Unknown|HL7

**Documentation**:

The patient's gender. There is a valueset defined based on the values in PID-8-1, but downstream consumers are free to define their own accepted values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: patient_id

**ReportStream Internal Name**: patient_id

**Type**: TEXT

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The ID for the patient within one of the reporting entities for this lab result. It could be the
the patient ID from the testing lab, the oder placer, the ordering provider, or even within the PRIME system itself.


---

**Name**: patient_id_type

**ReportStream Internal Name**: patient_id_type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: patient_last_name

**ReportStream Internal Name**: patient_last_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: patient_middle_name

**ReportStream Internal Name**: patient_middle_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_phone_number

**ReportStream Internal Name**: patient_phone_number

**Type**: TELEPHONE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: patient_race

**ReportStream Internal Name**: patient_race

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

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
2076-8|Native Hawaiian or Other Pacific Islander|HL7
2106-3|White|HL7
2131-1|Other|HL7
ASKU|Asked, but unknown|NULLFL

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
1002-5|American Indian or Alaska Native|HL7
2028-9|Asian|HL7
2054-5|Black or African American|HL7
2076-8|Native Hawaiian or Other Pacific Islander|HL7
2106-3|White|HL7
2131-1|Other Race|HL7
ASKU|Unknown|HL7

**Documentation**:

The patient's race. There is a common valueset defined for race values, but some states may choose to define different code/value combinations.


---

**Name**: patient_state

**ReportStream Internal Name**: patient_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: patient_street

**ReportStream Internal Name**: patient_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: patient_zip_code

**ReportStream Internal Name**: patient_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: placer_order_id

**ReportStream Internal Name**: placer_order_id

**Type**: ID

**PII**: No

**HL7 Fields**

- [OBR-2-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.1)
- [ORC-2-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.1)

**Cardinality**: [0..1]

**Documentation**:

The ID number of the lab order from the placer

---

**Name**: pregnant

**ReportStream Internal Name**: pregnant

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 82810-3

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
77386006|Pregnant|SNOMED_CT
60001007|Not Pregnant|SNOMED_CT
261665006|Unknown|SNOMED_CT

**Documentation**:

Is the patient pregnant?

---

**Name**: processing_mode_code

**ReportStream Internal Name**: processing_mode_code

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Default Value**: T

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
D|Debugging|HL7
P|Production|HL7
T|Training|HL7
D|Debugging|HL7
T|Training|HL7
P|Production|HL7

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
D|true|HL7
T|true|HL7
P|false|HL7

**Documentation**:

P, D, or T for Production, Debugging, or Training

---

**Name**: redox_destination_id

**ReportStream Internal Name**: redox_destination_id

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_destination_name

**ReportStream Internal Name**: redox_destination_name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_employed_in_healthcare_code

**ReportStream Internal Name**: redox_employed_in_healthcare_code

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_employed_in_healthcare_codeset

**ReportStream Internal Name**: redox_employed_in_healthcare_codeset

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_employed_in_healthcare_description

**ReportStream Internal Name**: redox_employed_in_healthcare_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_employed_in_healthcare_status

**ReportStream Internal Name**: redox_employed_in_healthcare_status

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_employed_in_healthcare_value_type

**ReportStream Internal Name**: redox_employed_in_healthcare_value_type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_first_test_code

**ReportStream Internal Name**: redox_first_test_code

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_first_test_codeset

**ReportStream Internal Name**: redox_first_test_codeset

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_first_test_description

**ReportStream Internal Name**: redox_first_test_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_first_test_status

**ReportStream Internal Name**: redox_first_test_status

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_first_test_value_type

**ReportStream Internal Name**: redox_first_test_value_type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_hospitalized_code

**ReportStream Internal Name**: redox_hospitalized_code

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_hospitalized_codeset

**ReportStream Internal Name**: redox_hospitalized_codeset

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_hospitalized_description

**ReportStream Internal Name**: redox_hospitalized_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_hospitalized_status

**ReportStream Internal Name**: redox_hospitalized_status

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_hospitalized_value_type

**ReportStream Internal Name**: redox_hospitalized_value_type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_icu_code

**ReportStream Internal Name**: redox_icu_code

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_icu_codeset

**ReportStream Internal Name**: redox_icu_codeset

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_icu_description

**ReportStream Internal Name**: redox_icu_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_icu_status

**ReportStream Internal Name**: redox_icu_status

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_icu_value_type

**ReportStream Internal Name**: redox_icu_value_type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_illness_onset_date_code

**ReportStream Internal Name**: redox_illness_onset_date_code

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_illness_onset_date_codeset

**ReportStream Internal Name**: redox_illness_onset_date_codeset

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_illness_onset_date_description

**ReportStream Internal Name**: redox_illness_onset_date_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_illness_onset_date_status

**ReportStream Internal Name**: redox_illness_onset_date_status

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_illness_onset_date_value_type

**ReportStream Internal Name**: redox_illness_onset_date_value_type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_patient_drivers_license_type

**ReportStream Internal Name**: redox_patient_drivers_license_type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_pregnant_code

**ReportStream Internal Name**: redox_pregnant_code

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_pregnant_codeset

**ReportStream Internal Name**: redox_pregnant_codeset

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_pregnant_description

**ReportStream Internal Name**: redox_pregnant_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_pregnant_status

**ReportStream Internal Name**: redox_pregnant_status

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_pregnant_value_type

**ReportStream Internal Name**: redox_pregnant_value_type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_resident_congregate_setting_code

**ReportStream Internal Name**: redox_resident_congregate_setting_code

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_resident_congregate_setting_codeset

**ReportStream Internal Name**: redox_resident_congregate_setting_codeset

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_resident_congregate_setting_description

**ReportStream Internal Name**: redox_resident_congregate_setting_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_resident_congregate_setting_status

**ReportStream Internal Name**: redox_resident_congregate_setting_status

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_resident_congregate_setting_value_type

**ReportStream Internal Name**: redox_resident_congregate_setting_value_type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_resulted

**ReportStream Internal Name**: redox_resulted

**Type**: TEXT

**PII**: No

**Default Value**: Resulted

**Cardinality**: [0..1]

---

**Name**: redox_source_id

**ReportStream Internal Name**: redox_source_id

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_source_name

**ReportStream Internal Name**: redox_source_name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_symptomatic_for_disease_code

**ReportStream Internal Name**: redox_symptomatic_for_disease_code

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_symptomatic_for_disease_codeset

**ReportStream Internal Name**: redox_symptomatic_for_disease_codeset

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_symptomatic_for_disease_description

**ReportStream Internal Name**: redox_symptomatic_for_disease_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_symptomatic_for_disease_status

**ReportStream Internal Name**: redox_symptomatic_for_disease_status

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_symptomatic_for_disease_value_type

**ReportStream Internal Name**: redox_symptomatic_for_disease_value_type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_test_lab_id_type

**ReportStream Internal Name**: redox_test_lab_id_type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: redox_test_result_type

**ReportStream Internal Name**: redox_test_result_type

**Type**: TEXT

**PII**: No

**Default Value**: Coded Entry

**Cardinality**: [0..1]

---

**Name**: reference_range

**ReportStream Internal Name**: reference_range

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The reference range of the lab result, such as “Negative” or “Normal”. For IgG, IgM and CT results that provide a value you MUST fill out this filed.

---

**Name**: resident_congregate_setting

**ReportStream Internal Name**: resident_congregate_setting

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 95421-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|Yes|HL7
N|No|HL7
UNK|Unknown|NULLFL

**Documentation**:

Does the patient reside in a congregate care setting?

---

**Name**: specimen_collection_date_time

**ReportStream Internal Name**: specimen_collection_date_time

**Type**: DATETIME

**PII**: No

**HL7 Fields**

- [OBR-7](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.7)
- [OBR-8](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.8)
- [OBX-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.14)
- [SPM-17-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.17.1)

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: specimen_received_date_time

**ReportStream Internal Name**: specimen_received_date_time

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: specimen_source_site_code

**ReportStream Internal Name**: specimen_source_site_code

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
119297000|Blood specimen (specimen)|SNOMED_CT
71836000|Nasopharyngeal structure (body structure)|SNOMED_CT
45206002|Nasal structure (body structure)|SNOMED_CT
53342003|Internal nose structure (body structure)|SNOMED_CT
29092000|Venous structure (body structure)|SNOMED_CT

**Documentation**:

Refers back to the specimen source site, which is then encoded into the SPM-8 segment

---

**Name**: specimen_type

**ReportStream Internal Name**: specimen_type

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
445297001|Swab of internal nose|SNOMED_CT
258500001|Nasopharyngeal swab|SNOMED_CT
871810001|Mid-turbinate nasal swab|SNOMED_CT
697989009|Anterior nares swab|SNOMED_CT
258411007|Nasopharyngeal aspirate|SNOMED_CT
429931000124105|Nasal aspirate|SNOMED_CT
258529004|Throat swab|SNOMED_CT
119334006|Sputum specimen|SNOMED_CT
119342007|Saliva specimen|SNOMED_CT
258607008|Bronchoalveolar lavage fluid sample|SNOMED_CT
119364003|Serum specimen|SNOMED_CT
119361006|Plasma specimen|SNOMED_CT
440500007|Dried blood spot specimen|SNOMED_CT
258580003|Whole blood sample|SNOMED_CT
122555007|Venous blood specimen|SNOMED_CT
119297000|Blood specimen|SNOMED_CT
122554006|Capillary blood specimen|SNOMED_CT

**Documentation**:

The specimen source, such as Blood or Serum

---

**Name**: symptomatic_for_disease

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

**Name**: test_performed_code

**ReportStream Internal Name**: test_performed_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Performed LOINC Code

**Documentation**:

The LOINC code of the test performed. This is a standardized coded value describing the test

---

**Name**: test_performed_name

**ReportStream Internal Name**: test_performed_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Performed LOINC Long Name

**Documentation**:

The LOINC description of the test performed as related to the LOINC code.

---

**Name**: test_performed_system

**ReportStream Internal Name**: test_performed_system

**Type**: TEXT

**PII**: No

**Default Value**: LOINC

**Cardinality**: [0..1]

---

**Name**: test_result

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

**Name**: test_result_date

**ReportStream Internal Name**: test_result_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

---

**Name**: test_result_status

**ReportStream Internal Name**: test_result_status

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**HL7 Fields**

- [OBR-25-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.25.1)
- [OBX-11-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.11.1)

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
A|Some, but not all, results available|HL7
C|Corrected, final|HL7
F|Final results|HL7
I|No results available; specimen received, procedure incomplete|HL7
M|Corrected, not final|HL7
N|Procedure completed, results pending|HL7
O|Order received; specimen not yet received|HL7
P|Preliminary|HL7
R|Results stored; not yet verified|HL7
S|No results available; procedure scheduled, but not done|HL7
X|No results available; Order canceled|HL7
Y|No order on record for this test|HL7
Z|No record of this patient|HL7
C|Corrected, final|HL7
F|Final results|HL7
X|No results available; Order canceled|HL7
A|Some, but not all, results available|HL7
I|No results available; specimen received, procedure incomplete|HL7
M|Corrected, not final|HL7
N|Procedure completed, results pending|HL7
O|Order received; specimen not yet received|HL7
P|Preliminary|HL7
R|Results stored; not yet verified|HL7
S|No results available; procedure scheduled, but not done|HL7
Y|No order on record for this test|HL7
Z|No record of this patient|HL7

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
C|Corrected|HL7
F|Final|HL7
X|Canceled|HL7
A|Preliminary|HL7
I|Unavailable|HL7
M|Corrected|HL7
N|Preliminary|HL7
O|Preliminary|HL7
P|Preliminary|HL7
R|Preliminary|HL7
S|Unavailable|HL7
Y|Unavailable|HL7
Z|Unavailable|HL7

**Documentation**:

The test result status, which is different from the test result itself. Per the valueset, this indicates if
the test result is in some intermediate status, is a correction, or is the final result.


---

**Name**: testing_lab_city

**ReportStream Internal Name**: testing_lab_city

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The city of the testing lab

---

**Name**: testing_lab_clia

**ReportStream Internal Name**: testing_lab_clia

**Type**: ID_CLIA

**PII**: No

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

**Name**: testing_lab_country

**ReportStream Internal Name**: testing_lab_country

**Type**: TEXT

**PII**: No

**Default Value**: USA

**Cardinality**: [0..1]

**Documentation**:

The country for the testing lab. Currently defaults to USA

---

**Name**: testing_lab_county

**ReportStream Internal Name**: testing_lab_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

**Documentation**:

The text value for the testing lab county. This is used to do the lookup in the FIPS dataset.

---

**Name**: testing_lab_email

**ReportStream Internal Name**: testing_lab_email

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: testing_lab_name

**ReportStream Internal Name**: testing_lab_name

**Type**: TEXT

**PII**: No

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

**Cardinality**: [0..1]

**Documentation**:

The phone number of the testing lab

---

**Name**: testing_lab_state

**ReportStream Internal Name**: testing_lab_state

**Type**: TABLE

**PII**: No

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

**Cardinality**: [0..1]

**Documentation**:

The street address for the testing lab

---

**Name**: testing_lab_zip_code

**ReportStream Internal Name**: testing_lab_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The postal code for the testing lab

---
