### Schema: hl7/hl7-ingest-covid-19-prod
### Topic: covid-19
### Tracking Element: (specimen_id)
### Base On: none
### Extends: [hl7/hl7-ingest-covid-19](./hl7-hl7-ingest-covid-19.md)
#### Description: HL7 schema - default production schema using sender settings for sender_id

---

**Name**: Screening

**ReportStream Internal Name**: Screening

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Screening

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

**Name**: collection_kit

**ReportStream Internal Name**: collection_kit

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Collection Kit

---

**Name**: comment

**ReportStream Internal Name**: comment

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: comment_source

**ReportStream Internal Name**: comment_source

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
L|Ancillary (filler) department is source of comment|HL7
O|Other system is source of comment|HL7
P|Orderer (placer) is source of comment|HL7

---

**Name**: comment_type

**ReportStream Internal Name**: comment_type

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
1R|Primary Reason|HL7
2R|Secondary Reason|HL7
AI|Ancillary Instructions|HL7
DR|Duplicate/Interaction Reason|HL7
GI|General Instructions|HL7
GR|General Reason|HL7
PI|Patient Instructions|HL7
RE|Remark|HL7

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

**Name**: device_id_type

**ReportStream Internal Name**: device_id_type

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 
                
**Table**: LIVD-SARS-CoV-2

**Documentation**:
Device_id_type is a generated value for the OBX-17 field. It is based on the device model and the LIVD table.
---

**Name**: direct_to_consumer

**ReportStream Internal Name**: direct_to_consumer

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Direct to Consumer (DTC)

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

**Name**: employed_in_high_risk_setting

**ReportStream Internal Name**: employed_in_high_risk_setting

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|Yes|HL7
N|No|HL7
UNK|Unknown|NULLFL

**Documentation**:
Is the patient employed in a high risk setting? This AOE question doesn't have an HL7 conversion per the HHS, so it is not included in HL7 messages.

---

**Name**: equipment_instance_id

**ReportStream Internal Name**: equipment_instance_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

---

**Name**: equipment_manufacture

**ReportStream Internal Name**: equipment_manufacture

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: Manufacturer

---

**Name**: equipment_model_id

**ReportStream Internal Name**: equipment_model_id

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: Equipment UID

---

**Name**: equipment_model_id_type

**ReportStream Internal Name**: equipment_model_id_type

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: Equipment UID Type

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

**Name**: file_created_date

**ReportStream Internal Name**: file_created_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
When was this file created. This is only used for HL7 generation.
---

**Name**: filler_clia

**ReportStream Internal Name**: filler_clia

**Type**: ID_CLIA

**PII**: No

**Cardinality**: [0..1]

---

**Name**: filler_name

**ReportStream Internal Name**: filler_name

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [OBR-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.2)
- [ORC-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.2)

**Cardinality**: [0..1]

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

**Name**: flatfile_version_no

**ReportStream Internal Name**: flatfile_version_no

**Type**: NUMBER

**PII**: No

**Cardinality**: [0..1]

---

**Name**: home_collection

**ReportStream Internal Name**: home_collection

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Home Collection

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

**Type**: DATE

**PII**: No

**LOINC Code**: 65222-2

**Cardinality**: [0..1]

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

**Default Value**: 

**Cardinality**: [0..1]

**Documentation**:
The message profile identifer
---

**Name**: message_profile_id_namespace_id

**ReportStream Internal Name**: message_profile_id_namespace_id

**Type**: TEXT

**PII**: No

**Default Value**: ELR_Receiver

**Cardinality**: [0..1]

**Documentation**:
The message profile identifer namespace id
---

**Name**: message_profile_id_universal_id

**ReportStream Internal Name**: message_profile_id_universal_id

**Type**: TEXT

**PII**: No

**Default Value**: 2.16.840.1.113883.9.11

**Cardinality**: [0..1]

**Documentation**:
The message profile identifer universal id
---

**Name**: message_profile_id_universal_id_type

**ReportStream Internal Name**: message_profile_id_universal_id_type

**Type**: TEXT

**PII**: No

**Default Value**: ISO

**Cardinality**: [0..1]

**Documentation**:
The message profile identifer universal id type
---

**Name**: nonprescription_testing

**ReportStream Internal Name**: nonprescription_testing

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Non-prescription Testing

---

**Name**: observation_result_status

**ReportStream Internal Name**: observation_result_status

**Type**: ID

**PII**: No

**Default Value**: F

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
C|Record coming over is a correction and thus replaces a final result|HL7
D|Deletes the OBX record|HL7
F|Final results; Can only be changed with a corrected result|HL7
I|Specimen in lab; results pending|HL7
N|Not asked; used to affirmatively document that the observation identified in the OBX was not sought when the universal service ID in OBR-4 implies that it would be sought.|HL7
O|Order detail description only (no result)|HL7
P|Preliminary results|HL7
R|Results entered -- not verified|HL7
S|Partial results|HL7
U|Results status change to final without retransmitting results already sent as ‘preliminary.’  E.g., radiology changes status from preliminary to final|HL7
W|Post original as wrong, e.g., transmitted for wrong patient|HL7
X|Results cannot be obtained for this observation|HL7

---

**Name**: order_result_status

**ReportStream Internal Name**: order_result_status

**Type**: ID

**PII**: No

**Default Value**: F

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

**Name**: ordered_test_encoding_version

**ReportStream Internal Name**: ordered_test_encoding_version

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: LOINC Version ID

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

**Name**: ordered_test_system_abbr

**ReportStream Internal Name**: ordered_test_system_abbr

**Type**: TEXT

**PII**: No

**Default Value**: LN

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

**Documentation**:
The country of the facility
---

**Name**: ordering_facility_county

**ReportStream Internal Name**: ordering_facility_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: ordering_facility_county_code

**ReportStream Internal Name**: ordering_facility_county_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

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

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:
The state of the facility which the test was ordered from
---

**Name**: ordering_facility_state_zip_lookup

**ReportStream Internal Name**: ordering_facility_state_zip_lookup

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: zip-code-data

**Table Column**: state_abbr

---

**Name**: ordering_facility_street

**ReportStream Internal Name**: ordering_facility_street

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The address of the facility which the test was ordered from
---

**Name**: ordering_facility_street2

**ReportStream Internal Name**: ordering_facility_street2

**Type**: STREET_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The secondary address of the facility which the test was ordered from
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

**Documentation**:
The country of the provider
---

**Name**: ordering_provider_county

**ReportStream Internal Name**: ordering_provider_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: ordering_provider_county_code

**ReportStream Internal Name**: ordering_provider_county_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

**Documentation**:
The FIPS code for the ordering provider
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

**Name**: ordering_provider_id_authority

**ReportStream Internal Name**: ordering_provider_id_authority

**Type**: HD

**PII**: No

**HL7 Fields**

- [OBR-16-9](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.9)
- [ORC-12-9](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.9)

**Cardinality**: [0..1]

**Documentation**:
Usually the OID for CMS
---

**Name**: ordering_provider_id_authority_type

**ReportStream Internal Name**: ordering_provider_id_authority_type

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [OBR-16-13](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.13)
- [ORC-12-13](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.13)

**Cardinality**: [0..1]

**Documentation**:
Usually NPI
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

**Name**: ordering_provider_middle_initial

**ReportStream Internal Name**: ordering_provider_middle_initial

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.4)
- [ORC-12-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.4)

**Cardinality**: [0..1]

---

**Name**: ordering_provider_middle_name

**ReportStream Internal Name**: ordering_provider_middle_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.4)
- [ORC-12-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.4)

**Cardinality**: [0..1]

---

**Name**: ordering_provider_name_type_code

**ReportStream Internal Name**: ordering_provider_name_type_code

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [OBR-16-10](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.10)
- [ORC-12-10](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.10)

**Cardinality**: [0..1]

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

**Name**: ordering_provider_street2

**ReportStream Internal Name**: ordering_provider_street2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The street second address of the provider
---

**Name**: ordering_provider_zip_code

**ReportStream Internal Name**: ordering_provider_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The zip code of the provider
---

**Name**: organization_name

**ReportStream Internal Name**: organization_name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The SimpleReport concept of organization. It refers to organization for the ordering & performing facility
---

**Name**: over_the_counter_home_testing

**ReportStream Internal Name**: over_the_counter_home_testing

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Over the Counter (OTC) Home Testing

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

**Name**: patient_citizenship

**ReportStream Internal Name**: patient_citizenship

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
ABW|Aruba|ISO
AFG|Afghanistan|ISO
AGO|Angola|ISO
AIA|Anguilla|ISO
ALA|Aland Islands|ISO
ALB|Albania|ISO
AND|Andorra|ISO
ARE|United Arab Emirates|ISO
ARG|Argentina|ISO
ARM|Armenia|ISO
ASM|American Samoa|ISO
ATA|Antarctica|ISO
ATF|French Southern Territories|ISO
ATG|Antigua and Barbuda|ISO
AUS|Australia|ISO
AUT|Austria|ISO
AZE|Azerbaijan|ISO
BDI|Burundi|ISO
BEL|Belgium|ISO
BEN|Benin|ISO
BES|Bonaire, Sint Eustatius and Saba|ISO
BFA|Burkina Faso|ISO
BGD|Bangladesh|ISO
BGR|Bulgaria|ISO
BHR|Bahrain|ISO
BHS|Bahamas|ISO
BIH|Bosnia and Herzegovina|ISO
BLM|Saint BarthÃ©lemy|ISO
BLR|Belarus|ISO
BLZ|Belize|ISO
BMU|Bermuda|ISO
BOL|Bolivia (Plurinational State of)|ISO
BRA|Brazil|ISO
BRB|Barbados|ISO
BRN|Brunei Darussalam|ISO
BTN|Bhutan|ISO
BVT|Bouvet Island|ISO
BWA|Botswana|ISO
CAF|Central African Republic|ISO
CAN|Canada|ISO
CCK|Cocos (Keeling) Islands|ISO
CHE|Switzerland|ISO
CHL|Chile|ISO
CHN|China|ISO
CIV|Côte d'Ivoire|ISO
CMR|Cameroon|ISO
COD|Congo, Democratic Republic of the|ISO
COG|Congo|ISO
COK|Cook Islands|ISO
COL|Colombia|ISO
COM|Comoros|ISO
CPV|Cabo Verde|ISO
CRI|Costa Rica|ISO
CUB|Cuba|ISO
CUW|CuraÃ§ao|ISO
CXR|Christmas Island|ISO
CYM|Cayman Islands|ISO
CYP|Cyprus|ISO
CZE|Czechia|ISO
DEU|Germany|ISO
DJI|Djibouti|ISO
DMA|Dominica|ISO
DNK|Denmark|ISO
DOM|Dominican Republic|ISO
DZA|Algeria|ISO
ECU|Ecuador|ISO
EGY|Egypt|ISO
ERI|Eritrea|ISO
ESH|Western Sahara|ISO
ESP|Spain|ISO
EST|Estonia|ISO
ETH|Ethiopia|ISO
FIN|Finland|ISO
FJI|Fiji|ISO
FLK|Falkland Islands (Malvinas)|ISO
FRA|France|ISO
FRO|Faroe Islands|ISO
FSM|Micronesia (Federated States of)|ISO
GAB|Gabon|ISO
GBR|United Kingdom of Great Britain and Northern Ireland|ISO
GEO|Georgia|ISO
GGY|Guernsey|ISO
GHA|Ghana|ISO
GIB|Gibraltar|ISO
GIN|Guinea|ISO
GLP|Guadeloupe|ISO
GMB|Gambia|ISO
GNB|Guinea-Bissau|ISO
GNQ|Equatorial Guinea|ISO
GRC|Greece|ISO
GRD|Grenada|ISO
GRL|Greenland|ISO
GTM|Guatemala|ISO
GUF|French Guiana|ISO
GUM|Guam|ISO
GUY|Guyana|ISO
HKG|Hong Kong|ISO
HMD|Heard Island and McDonald Islands|ISO
HND|Honduras|ISO
HRV|Croatia|ISO
HTI|Haiti|ISO
HUN|Hungary|ISO
IDN|Indonesia|ISO
IMN|Isle of Man|ISO
IND|India|ISO
IOT|British Indian Ocean Territory|ISO
IRL|Ireland|ISO
IRN|Iran (Islamic Republic of)|ISO
IRQ|Iraq|ISO
ISL|Iceland|ISO
ISR|Israel|ISO
ITA|Italy|ISO
JAM|Jamaica|ISO
JEY|Jersey|ISO
JOR|Jordan|ISO
JPN|Japan|ISO
KAZ|Kazakhstan|ISO
KEN|Kenya|ISO
KGZ|Kyrgyzstan|ISO
KHM|Cambodia|ISO
KIR|Kiribati|ISO
KNA|Saint Kitts and Nevis|ISO
KOR|Korea, Republic of|ISO
KWT|Kuwait|ISO
LAO|Lao People's Democratic Republic|ISO
LBN|Lebanon|ISO
LBR|Liberia|ISO
LBY|Libya|ISO
LCA|Saint Lucia|ISO
LIE|Liechtenstein|ISO
LKA|Sri Lanka|ISO
LSO|Lesotho|ISO
LTU|Lithuania|ISO
LUX|Luxembourg|ISO
LVA|Latvia|ISO
MAC|Macao|ISO
MAF|Saint Martin (French part)|ISO
MAR|Morocco|ISO
MCO|Monaco|ISO
MDA|Moldova, Republic of|ISO
MDG|Madagascar|ISO
MDV|Maldives|ISO
MEX|Mexico|ISO
MHL|Marshall Islands|ISO
MKD|North Macedonia|ISO
MLI|Mali|ISO
MLT|Malta|ISO
MMR|Myanmar|ISO
MNE|Montenegro|ISO
MNG|Mongolia|ISO
MNP|Northern Mariana Islands|ISO
MOZ|Mozambique|ISO
MRT|Mauritania|ISO
MSR|Montserrat|ISO
MTQ|Martinique|ISO
MUS|Mauritius|ISO
MWI|Malawi|ISO
MYS|Malaysia|ISO
MYT|Mayotte|ISO
NAM|Namibia|ISO
NCL|New Caledonia|ISO
NER|Niger|ISO
NFK|Norfolk Island|ISO
NGA|Nigeria|ISO
NIC|Nicaragua|ISO
NIU|Niue|ISO
NLD|Netherlands|ISO
NOR|Norway|ISO
NPL|Nepal|ISO
NRU|Nauru|ISO
NZL|New Zealand|ISO
OMN|Oman|ISO
PAK|Pakistan|ISO
PAN|Panama|ISO
PCN|Pitcairn|ISO
PER|Peru|ISO
PHL|Philippines|ISO
PLW|Palau|ISO
PNG|Papua New Guinea|ISO
POL|Poland|ISO
PRI|Puerto Rico|ISO
PRK|Korea (Democratic People's Republic of)|ISO
PRT|Portugal|ISO
PRY|Paraguay|ISO
PSE|Palestine, State of|ISO
PYF|French Polynesia|ISO
QAT|Qatar|ISO
REU|RÃ©union|ISO
ROU|Romania|ISO
RUS|Russian Federation|ISO
RWA|Rwanda|ISO
SAU|Saudi Arabia|ISO
SDN|Sudan|ISO
SEN|Senegal|ISO
SGP|Singapore|ISO
SGS|South Georgia and the South Sandwich Islands|ISO
SHN|Saint Helena, Ascension and Tristan da Cunha|ISO
SJM|Svalbard and Jan Mayen|ISO
SLB|Solomon Islands|ISO
SLE|Sierra Leone|ISO
SLV|El Salvador|ISO
SMR|San Marino|ISO
SOM|Somalia|ISO
SPM|Saint Pierre and Miquelon|ISO
SRB|Serbia|ISO
SSD|South Sudan|ISO
STP|Sao Tome and Principe|ISO
SUR|Suriname|ISO
SVK|Slovakia|ISO
SVN|Slovenia|ISO
SWE|Sweden|ISO
SWZ|Eswatini|ISO
SXM|Sint Maarten (Dutch part)|ISO
SYC|Seychelles|ISO
SYR|Syrian Arab Republic|ISO
TCA|Turks and Caicos Islands|ISO
TCD|Chad|ISO
TGO|Togo|ISO
THA|Thailand|ISO
TJK|Tajikistan|ISO
TKL|Tokelau|ISO
TKM|Turkmenistan|ISO
TLS|Timor-Leste|ISO
TON|Tonga|ISO
TTO|Trinidad and Tobago|ISO
TUN|Tunisia|ISO
TUR|Turkey|ISO
TUV|Tuvalu|ISO
TWN|Taiwan, Province of China|ISO
TZA|Tanzania, United Republic of|ISO
UGA|Uganda|ISO
UKR|Ukraine|ISO
UMI|United States Minor Outlying Islands|ISO
URY|Uruguay|ISO
USA|United States of America|ISO
UZB|Uzbekistan|ISO
VAT|Holy See|ISO
VCT|Saint Vincent and the Grenadines|ISO
VEN|Venezuela (Bolivarian Republic of)|ISO
VGB|Virgin Islands (British)|ISO
VIR|Virgin Islands (U.S.)|ISO
VNM|Viet Nam|ISO
VUT|Vanuatu|ISO
WLF|Wallis and Futuna|ISO
WSM|Samoa|ISO
YEM|Yemen|ISO
ZAF|South Africa|ISO
ZMB|Zambia|ISO
ZWE|Zimbabwe|ISO

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

**Name**: patient_county_code

**ReportStream Internal Name**: patient_county_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

**Documentation**:
The FIPS code for the patient's county
---

**Name**: patient_death_date

**ReportStream Internal Name**: patient_death_date

**Type**: DATE

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_died

**ReportStream Internal Name**: patient_died

**Type**: CODE

**PII**: Yes

**Format**: use value found in the Code column

**Default Value**: N

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|Yes|HL7
N|No|HL7
UNK|Unknown|NULLFL

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
H|Hispanic or Latino|LOCAL
H|Hispanic|LOCAL
H|Latino|LOCAL
H|Mex. Amer./Hispanic|LOCAL
H|2135-2|LOCAL
H|H|LOCAL
N|Non Hispanic or Latino|LOCAL
N|Non-Hispanic or Latino|LOCAL
N|Non Hispanic|LOCAL
N|Non-Hispanic|LOCAL
N|Not Hispanic or Latino|LOCAL
N|Not Hispanic|LOCAL
N|2186-5|LOCAL
N|N|LOCAL
U|Unknown|LOCAL
U|U|LOCAL
U|UNK|LOCAL
U|Black|LOCAL
U|White|LOCAL
U|African American|LOCAL
U|NULL|LOCAL
U|Patient Declines|LOCAL

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

**Name**: patient_id_assigner

**ReportStream Internal Name**: patient_id_assigner

**Type**: HD

**PII**: No

**HL7 Fields**

- [PID-3-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4)
- [PID-3-6-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.6.2)

**Cardinality**: [0..1]

**Documentation**:
The name of the assigner of the patient_id field. Typically we use the name of the ordering facility
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

**Name**: patient_middle_initial

**ReportStream Internal Name**: patient_middle_initial

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_middle_name

**ReportStream Internal Name**: patient_middle_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_name_type_code

**ReportStream Internal Name**: patient_name_type_code

**Type**: TEXT

**PII**: No

**Default Value**: L

**Cardinality**: [0..1]

---

**Name**: patient_nationality

**ReportStream Internal Name**: patient_nationality

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
ABW|Aruba|ISO
AFG|Afghanistan|ISO
AGO|Angola|ISO
AIA|Anguilla|ISO
ALA|Aland Islands|ISO
ALB|Albania|ISO
AND|Andorra|ISO
ARE|United Arab Emirates|ISO
ARG|Argentina|ISO
ARM|Armenia|ISO
ASM|American Samoa|ISO
ATA|Antarctica|ISO
ATF|French Southern Territories|ISO
ATG|Antigua and Barbuda|ISO
AUS|Australia|ISO
AUT|Austria|ISO
AZE|Azerbaijan|ISO
BDI|Burundi|ISO
BEL|Belgium|ISO
BEN|Benin|ISO
BES|Bonaire, Sint Eustatius and Saba|ISO
BFA|Burkina Faso|ISO
BGD|Bangladesh|ISO
BGR|Bulgaria|ISO
BHR|Bahrain|ISO
BHS|Bahamas|ISO
BIH|Bosnia and Herzegovina|ISO
BLM|Saint BarthÃ©lemy|ISO
BLR|Belarus|ISO
BLZ|Belize|ISO
BMU|Bermuda|ISO
BOL|Bolivia (Plurinational State of)|ISO
BRA|Brazil|ISO
BRB|Barbados|ISO
BRN|Brunei Darussalam|ISO
BTN|Bhutan|ISO
BVT|Bouvet Island|ISO
BWA|Botswana|ISO
CAF|Central African Republic|ISO
CAN|Canada|ISO
CCK|Cocos (Keeling) Islands|ISO
CHE|Switzerland|ISO
CHL|Chile|ISO
CHN|China|ISO
CIV|Côte d'Ivoire|ISO
CMR|Cameroon|ISO
COD|Congo, Democratic Republic of the|ISO
COG|Congo|ISO
COK|Cook Islands|ISO
COL|Colombia|ISO
COM|Comoros|ISO
CPV|Cabo Verde|ISO
CRI|Costa Rica|ISO
CUB|Cuba|ISO
CUW|CuraÃ§ao|ISO
CXR|Christmas Island|ISO
CYM|Cayman Islands|ISO
CYP|Cyprus|ISO
CZE|Czechia|ISO
DEU|Germany|ISO
DJI|Djibouti|ISO
DMA|Dominica|ISO
DNK|Denmark|ISO
DOM|Dominican Republic|ISO
DZA|Algeria|ISO
ECU|Ecuador|ISO
EGY|Egypt|ISO
ERI|Eritrea|ISO
ESH|Western Sahara|ISO
ESP|Spain|ISO
EST|Estonia|ISO
ETH|Ethiopia|ISO
FIN|Finland|ISO
FJI|Fiji|ISO
FLK|Falkland Islands (Malvinas)|ISO
FRA|France|ISO
FRO|Faroe Islands|ISO
FSM|Micronesia (Federated States of)|ISO
GAB|Gabon|ISO
GBR|United Kingdom of Great Britain and Northern Ireland|ISO
GEO|Georgia|ISO
GGY|Guernsey|ISO
GHA|Ghana|ISO
GIB|Gibraltar|ISO
GIN|Guinea|ISO
GLP|Guadeloupe|ISO
GMB|Gambia|ISO
GNB|Guinea-Bissau|ISO
GNQ|Equatorial Guinea|ISO
GRC|Greece|ISO
GRD|Grenada|ISO
GRL|Greenland|ISO
GTM|Guatemala|ISO
GUF|French Guiana|ISO
GUM|Guam|ISO
GUY|Guyana|ISO
HKG|Hong Kong|ISO
HMD|Heard Island and McDonald Islands|ISO
HND|Honduras|ISO
HRV|Croatia|ISO
HTI|Haiti|ISO
HUN|Hungary|ISO
IDN|Indonesia|ISO
IMN|Isle of Man|ISO
IND|India|ISO
IOT|British Indian Ocean Territory|ISO
IRL|Ireland|ISO
IRN|Iran (Islamic Republic of)|ISO
IRQ|Iraq|ISO
ISL|Iceland|ISO
ISR|Israel|ISO
ITA|Italy|ISO
JAM|Jamaica|ISO
JEY|Jersey|ISO
JOR|Jordan|ISO
JPN|Japan|ISO
KAZ|Kazakhstan|ISO
KEN|Kenya|ISO
KGZ|Kyrgyzstan|ISO
KHM|Cambodia|ISO
KIR|Kiribati|ISO
KNA|Saint Kitts and Nevis|ISO
KOR|Korea, Republic of|ISO
KWT|Kuwait|ISO
LAO|Lao People's Democratic Republic|ISO
LBN|Lebanon|ISO
LBR|Liberia|ISO
LBY|Libya|ISO
LCA|Saint Lucia|ISO
LIE|Liechtenstein|ISO
LKA|Sri Lanka|ISO
LSO|Lesotho|ISO
LTU|Lithuania|ISO
LUX|Luxembourg|ISO
LVA|Latvia|ISO
MAC|Macao|ISO
MAF|Saint Martin (French part)|ISO
MAR|Morocco|ISO
MCO|Monaco|ISO
MDA|Moldova, Republic of|ISO
MDG|Madagascar|ISO
MDV|Maldives|ISO
MEX|Mexico|ISO
MHL|Marshall Islands|ISO
MKD|North Macedonia|ISO
MLI|Mali|ISO
MLT|Malta|ISO
MMR|Myanmar|ISO
MNE|Montenegro|ISO
MNG|Mongolia|ISO
MNP|Northern Mariana Islands|ISO
MOZ|Mozambique|ISO
MRT|Mauritania|ISO
MSR|Montserrat|ISO
MTQ|Martinique|ISO
MUS|Mauritius|ISO
MWI|Malawi|ISO
MYS|Malaysia|ISO
MYT|Mayotte|ISO
NAM|Namibia|ISO
NCL|New Caledonia|ISO
NER|Niger|ISO
NFK|Norfolk Island|ISO
NGA|Nigeria|ISO
NIC|Nicaragua|ISO
NIU|Niue|ISO
NLD|Netherlands|ISO
NOR|Norway|ISO
NPL|Nepal|ISO
NRU|Nauru|ISO
NZL|New Zealand|ISO
OMN|Oman|ISO
PAK|Pakistan|ISO
PAN|Panama|ISO
PCN|Pitcairn|ISO
PER|Peru|ISO
PHL|Philippines|ISO
PLW|Palau|ISO
PNG|Papua New Guinea|ISO
POL|Poland|ISO
PRI|Puerto Rico|ISO
PRK|Korea (Democratic People's Republic of)|ISO
PRT|Portugal|ISO
PRY|Paraguay|ISO
PSE|Palestine, State of|ISO
PYF|French Polynesia|ISO
QAT|Qatar|ISO
REU|RÃ©union|ISO
ROU|Romania|ISO
RUS|Russian Federation|ISO
RWA|Rwanda|ISO
SAU|Saudi Arabia|ISO
SDN|Sudan|ISO
SEN|Senegal|ISO
SGP|Singapore|ISO
SGS|South Georgia and the South Sandwich Islands|ISO
SHN|Saint Helena, Ascension and Tristan da Cunha|ISO
SJM|Svalbard and Jan Mayen|ISO
SLB|Solomon Islands|ISO
SLE|Sierra Leone|ISO
SLV|El Salvador|ISO
SMR|San Marino|ISO
SOM|Somalia|ISO
SPM|Saint Pierre and Miquelon|ISO
SRB|Serbia|ISO
SSD|South Sudan|ISO
STP|Sao Tome and Principe|ISO
SUR|Suriname|ISO
SVK|Slovakia|ISO
SVN|Slovenia|ISO
SWE|Sweden|ISO
SWZ|Eswatini|ISO
SXM|Sint Maarten (Dutch part)|ISO
SYC|Seychelles|ISO
SYR|Syrian Arab Republic|ISO
TCA|Turks and Caicos Islands|ISO
TCD|Chad|ISO
TGO|Togo|ISO
THA|Thailand|ISO
TJK|Tajikistan|ISO
TKL|Tokelau|ISO
TKM|Turkmenistan|ISO
TLS|Timor-Leste|ISO
TON|Tonga|ISO
TTO|Trinidad and Tobago|ISO
TUN|Tunisia|ISO
TUR|Turkey|ISO
TUV|Tuvalu|ISO
TWN|Taiwan, Province of China|ISO
TZA|Tanzania, United Republic of|ISO
UGA|Uganda|ISO
UKR|Ukraine|ISO
UMI|United States Minor Outlying Islands|ISO
URY|Uruguay|ISO
USA|United States of America|ISO
UZB|Uzbekistan|ISO
VAT|Holy See|ISO
VCT|Saint Vincent and the Grenadines|ISO
VEN|Venezuela (Bolivarian Republic of)|ISO
VGB|Virgin Islands (British)|ISO
VIR|Virgin Islands (U.S.)|ISO
VNM|Viet Nam|ISO
VUT|Vanuatu|ISO
WLF|Wallis and Futuna|ISO
WSM|Samoa|ISO
YEM|Yemen|ISO
ZAF|South Africa|ISO
ZMB|Zambia|ISO
ZWE|Zimbabwe|ISO

---

**Name**: patient_phone_number

**ReportStream Internal Name**: patient_phone_number

**Type**: TELEPHONE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's phone number with area code
---

**Name**: patient_preferred_language

**ReportStream Internal Name**: patient_preferred_language

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
abk|Abkhazian|ISO
abk|Abkhaz|ISO
ace|Achinese|ISO
ach|Acoli|ISO
ada|Adangme|ISO
ady|Adyghe; Adygei|ISO
aar|Afar|ISO
afh|Afrihili|ISO
afr|Afrikaans|ISO
afa|Afro-Asiatic (Other)|ISO
ain|Ainu|ISO
aka|Akan|ISO
akk|Akkadian|ISO
alb|Albanian|ISO
ale|Aleut|ISO
alg|Algonquian languages|ISO
tut|Altaic (Other)|ISO
amh|Amharic|ISO
anp|Angika|ISO
apa|Apache languages|ISO
ara|Arabic|ISO
arg|Aragonese|ISO
arp|Arapaho|ISO
arw|Arawak|ISO
arm|Armenian|ISO
rup|Aromanian; Arumanian; Macedo-Romanian|ISO
art|Artificial (Other)|ISO
asm|Assamese|ISO
aii|Assyrian|ISO
ast|Asturian; Bable; Leonese; Asturleonese|ISO
ath|Athapascan languages|ISO
aus|Australian languages|ISO
map|Austronesian (Other)|ISO
ava|Avaric|ISO
ave|Avestan|ISO
awa|Awadhi|ISO
aym|Aymara|ISO
aze|Azerbaijani|ISO
ban|Balinese|ISO
bat|Baltic (Other)|ISO
bal|Baluchi|ISO
bam|Bambara|ISO
bai|Bamileke languages|ISO
bad|Banda languages|ISO
bnt|Bantu (Other)|ISO
bas|Basa|ISO
bak|Bashkir|ISO
baq|Basque|ISO
btk|Batak languages|ISO
bej|Beja; Bedawiyet|ISO
bel|Belarusian|ISO
bem|Bemba|ISO
ben|Bengali|ISO
ber|Berber (Other)|ISO
bho|Bhojpuri|ISO
bih|Bihari|ISO
bik|Bikol|ISO
bin|Bini; Edo|ISO
bis|Bislama|ISO
byn|Blin; Bilin|ISO
zbl|Blissymbols; Blissymbolics; Bliss|ISO
nob|Norwegian Bokmål|ISO
bos|Bosnian|ISO
bra|Braj|ISO
bre|Breton|ISO
bug|Buginese|ISO
bul|Bulgarian|ISO
bua|Buriat|ISO
bur|Burmese|ISO
cad|Caddo|ISO
yue|Cantonese|ISO
yue|Cantonese; Chinese|ISO
cat|Catalan; Valencian|ISO
cat|Catalan|ISO
cau|Caucasian (Other)|ISO
ceb|Cebuano|ISO
cel|Celtic (Other)|ISO
cai|Central American Indian (Other)|ISO
khm|Central Khmer|ISO
chg|Chagatai|ISO
cmc|Chamic languages|ISO
cha|Chamorro|ISO
che|Chechen|ISO
chr|Cherokee|ISO
chy|Cheyenne|ISO
chb|Chibcha|ISO
nya|Chichewa; Chewa; Nyanja|ISO
chi|Chinese|ISO
chn|Chinook jargon|ISO
chp|Chipewyan; Dene Suline|ISO
cho|Choctaw|ISO
chk|Chuukese|ISO
chv|Chuvash|ISO
nwc|Classical Newari; Old Newari; Classical Nepal Bhasa|ISO
syc|Classical Syriac|ISO
cop|Coptic|ISO
cor|Cornish|ISO
cos|Corsican|ISO
cre|Cree|ISO
mus|Creek|ISO
crp|Creoles and pidgins (Other)|ISO
cpe|Creoles and pidgins English based (Other)|ISO
cpf|Creoles and pidgins French-based (Other)|ISO
cpp|Creoles and pidgins Portuguese-based (Other)|ISO
crh|Crimean Tatar; Crimean Turkish|ISO
hrv|Croatian|ISO
cus|Cushitic (Other)|ISO
cze|Czech|ISO
dak|Dakota|ISO
dan|Danish|ISO
dar|Dargwa|ISO
prs|Dari|ISO
del|Delaware|ISO
din|Dinka|ISO
div|Divehi; Dhivehi; Maldivian|ISO
div|Dhivehi|ISO
doi|Dogri|ISO
dgr|Dogrib|ISO
dra|Dravidian (Other)|ISO
dua|Duala|ISO
dut|Dutch; Flemish|ISO
dut|Dutch|ISO
dyu|Dyula|ISO
dzo|Dzongkha|ISO
frs|Eastern Frisian|ISO
efi|Efik|ISO
eka|Ekajuk|ISO
elx|Elamite|ISO
eng|English|ISO
myv|Erzya|ISO
epo|Esperanto|ISO
est|Estonian|ISO
ewe|Ewe|ISO
ewo|Ewondo|ISO
fan|Fang|ISO
fat|Fanti|ISO
fao|Faroese|ISO
fij|Fijian|ISO
fil|Filipino; Pilipino|ISO
fil|Filipino|ISO
fin|Finnish|ISO
fiu|Finno-Ugrian (Other)|ISO
fon|Fon|ISO
fre|French|ISO
fry|Frisian|ISO
fur|Friulian|ISO
ful|Fulah|ISO
gaa|Ga|ISO
gla|Gaelic; Scottish Gaelic|ISO
gla|Gaelic|ISO
car|Galibi Carib|ISO
glg|Galician|ISO
lug|Ganda|ISO
gay|Gayo|ISO
gba|Gbaya|ISO
gez|Geez|ISO
geo|Georgian|ISO
ger|German|ISO
gem|Germanic (Other)|ISO
gil|Gilbertese|ISO
gon|Gondi|ISO
gor|Gorontalo|ISO
got|Gothic|ISO
grb|Grebo|ISO
gre|Greek|ISO
gre|Greek Modern (1453-)|ISO
grn|Guarani|ISO
guj|Gujarati|ISO
gwi|Gwich'in|ISO
hai|Haida|ISO
hat|Haitian; Haitian Creole|ISO
hat|Haitian|ISO
hau|Hausa|ISO
haw|Hawaiian|ISO
heb|Hebrew|ISO
her|Herero|ISO
hil|Hiligaynon|ISO
him|Himachali|ISO
hin|Hindi|ISO
hmo|Hiri Motu|ISO
hit|Hittite|ISO
hmn|Hmong|ISO
hun|Hungarian|ISO
hup|Hupa|ISO
iba|Iban|ISO
ice|Icelandic|ISO
ido|Ido|ISO
ibo|Igbo|ISO
ijo|Ijo languages|ISO
ilo|Iloko|ISO
smn|Inari Sami|ISO
inc|Indic (Other)|ISO
ine|Indo-European (Other)|ISO
ind|Indonesian|ISO
inh|Ingush|ISO
ikt|Inuinnaqtun|ISO
ina|Interlingua (International Auxiliary Language Association)|ISO
ile|Interlingue; Occidental|ISO
iku|Inuktitut|ISO
ipk|Inupiaq|ISO
ira|Iranian (Other)|ISO
gle|Irish|ISO
iro|Iroquoian languages|ISO
ita|Italian|ISO
jpn|Japanese|ISO
jav|Javanese|ISO
cjy|Jinyu Chinese|ISO
jrb|Judeo-Arabic|ISO
jpr|Judeo-Persian|ISO
kbd|Kabardian|ISO
kab|Kabyle|ISO
kac|Kachin; Jingpho|ISO
kal|Kalaallisut; Greenlandic|ISO
xal|Kalmyk; Oirat|ISO
kam|Kamba|ISO
kan|Kannada|ISO
kau|Kanuri|ISO
krc|Karachay-Balkar|ISO
kaa|Kara-Kalpak|ISO
krl|Karelian|ISO
kar|Karen languages|ISO
kas|Kashmiri|ISO
csb|Kashubian|ISO
kaw|Kawi|ISO
kaz|Kazakh|ISO
kha|Khasi|ISO
khi|Khoisan (Other)|ISO
khm|Khmer|ISO
kho|Khotanese|ISO
kik|Kikuyu; Gikuyu|ISO
kmb|Kimbundu|ISO
kin|Kinyarwanda|ISO
kir|Kirghiz; Kyrgyz|ISO
kir|Kyrgyz|ISO
kir|Kirghiz|ISO
tlh|Klingon; tlhIngan-Hol|ISO
kom|Komi|ISO
kon|Kongo|ISO
kok|Konkani|ISO
kor|Korean|ISO
kos|Kosraean|ISO
kpe|Kpelle|ISO
kro|Kru languages|ISO
kua|Kuanyama; Kwanyama|ISO
kum|Kumyk|ISO
kur|Kurdish|ISO
kru|Kurukh|ISO
kut|Kutenai|ISO
lad|Ladino|ISO
lah|Lahnda|ISO
lam|Lamba|ISO
day|Land Dayak languages|ISO
lao|Lao|ISO
lat|Latin|ISO
lav|Latvian|ISO
lez|Lezghian|ISO
lim|Limburgan; Limburger; Limburgish|ISO
lin|Lingala|ISO
lit|Lithuanian|ISO
jbo|Lojban|ISO
nds|Low German; Low Saxon; German|ISO
dsb|Lower Sorbian|ISO
loz|Lozi|ISO
lub|Luba-Katanga|ISO
lua|Luba-Lulua|ISO
lui|Luiseno|ISO
smj|Lule Sami|ISO
lun|Lunda|ISO
luo|Luo (Kenya and Tanzania)|ISO
lus|Lushai|ISO
ltz|Luxembourgish; Letzeburgesch|ISO
ltz|Luxembourgish|ISO
mac|Macedonian|ISO
mad|Madurese|ISO
mag|Magahi|ISO
mai|Maithili|ISO
mak|Makasar|ISO
mlg|Malagasy|ISO
may|Malay|ISO
mal|Malayalam|ISO
mlt|Maltese|ISO
cmn|Mandarin|ISO
cmn|Mandarin Chinese|ISO
mnc|Manchu|ISO
mdr|Mandar|ISO
man|Mandingo|ISO
mni|Manipuri|ISO
mno|Manobo languages|ISO
glv|Manx|ISO
mao|Maori|ISO
arn|Mapudungun; Mapuche|ISO
mar|Marathi|ISO
chm|Mari|ISO
mah|Marshallese|ISO
mwr|Marwari|ISO
mas|Masai|ISO
myn|Mayan languages|ISO
myn|Mayan|ISO
men|Mende|ISO
mic|Mi'kmaq; Micmac|ISO
min|Minangkabau|ISO
mwl|Mirandese|ISO
moh|Mohawk|ISO
mdf|Moksha|ISO
mol|Moldavian; Moldovan|ISO
mol|Moldovan|ISO
mol|Moldavian|ISO
lol|Mongo|ISO
mon|Mongolian|ISO
mkh|Mon-Khmer (Other)|ISO
cnr|Montenegrin|ISO
mos|Mossi|ISO
mul|Multiple languages|ISO
mun|Munda languages|ISO
nah|Nahuatl languages|ISO
nau|Nauru|ISO
nav|Navajo; Navaho|ISO
nde|Ndebele|ISO
nde|Ndebele North; North Ndebele|ISO
nbl|Ndebele South; South Ndebele|ISO
ndo|Ndonga|ISO
nap|Neapolitan|ISO
new|Nepal Bhasa; Newari|ISO
nep|Nepali|ISO
nia|Nias|ISO
nic|Niger-Kordofanian (Other)|ISO
ssa|Nilo-Saharan (Other)|ISO
niu|Niuean|ISO
nqo|N'Ko|ISO
zxx|No linguistic content; Not applicable|ISO
nog|Nogai|ISO
nai|North American Indian|ISO
frr|Northern Frisian|ISO
sme|Northern Sami|ISO
nso|Northern Sotho|ISO
nor|Norwegian|ISO
nno|Norwegian Nynorsk; Nynorsk Norwegian|ISO
nno|Nynorsk|ISO
nub|Nubian languages|ISO
nym|Nyamwezi|ISO
nyn|Nyankole|ISO
nyo|Nyoro|ISO
nzi|Nzima|ISO
oci|Occitan (post 1500); Provençal|ISO
oci|Occitan|ISO
oji|Ojibwa|ISO
ori|Oriya|ISO
orm|Oromo|ISO
osa|Osage|ISO
oss|Ossetian; Ossetic|ISO
oto|Otomian languages|ISO
pal|Pahlavi|ISO
pau|Palauan|ISO
pli|Pali|ISO
pam|Pampanga; Kapampangan|ISO
pag|Pangasinan|ISO
pan|Panjabi|ISO
pap|Papiamento|ISO
paa|Papuan (Other)|ISO
pus|Pashto|ISO
pus|Pushto|ISO
pst|Central Pashto|ISO
pbt|Southern Pashto|ISO
pus|Pashto|ISO
nso|Pedi; Sepedi; Northern Sotho|ISO
per|Persian|ISO
phi|Philippine (Other)|ISO
phn|Phoenician|ISO
pon|Pohnpeian|ISO
pol|Polish|ISO
por|Portuguese|ISO
pra|Prakrit languages|ISO
pan|Punjabi|ISO
pus|Pushto; Pashto|ISO
que|Quechua|ISO
raj|Rajasthani|ISO
rap|Rapanui|ISO
rar|Rarotongan; Cook Islands Maori|ISO
qaa-qtz|Reserved for local use|ISO
roa|Romance (Other)|ISO
rum|Romanian|ISO
roh|Romansh|ISO
rom|Romany|ISO
run|Rundi|ISO
rus|Russian|ISO
sal|Salishan languages|ISO
sam|Samaritan Aramaic|ISO
smi|Sami languages (Other)|ISO
smo|Samoan|ISO
sad|Sandawe|ISO
sag|Sango|ISO
san|Sanskrit|ISO
sat|Santali|ISO
srd|Sardinian|ISO
sas|Sasak|ISO
sco|Scots|ISO
sel|Selkup|ISO
sem|Semitic (Other)|ISO
srp|Serbian|ISO
srr|Serer|ISO
shn|Shan|ISO
sna|Shona|ISO
iii|Sichuan Yi; Nuosu|ISO
scn|Sicilian|ISO
sid|Sidamo|ISO
sgn|Sign Languages|ISO
bla|Siksika|ISO
snd|Sindhi|ISO
sin|Sinhala; Sinhalese|ISO
sin|Sinhalese|ISO
sit|Sino-Tibetan (Other)|ISO
sio|Siouan languages|ISO
sms|Skolt Sami|ISO
den|Slave (Athapascan)|ISO
sla|Slavic (Other)|ISO
slo|Slovak|ISO
slv|Slovenian|ISO
slv|Slovene|ISO
sog|Sogdian|ISO
som|Somali|ISO
son|Songhai languages|ISO
snk|Soninke|ISO
wen|Sorbian languages|ISO
sot|Sotho Southern|ISO
sai|South American Indian (Other)|ISO
alt|Southern Altai|ISO
sma|Southern Sami|ISO
spa|Spanish; Castilian|ISO
spa|Spanish|ISO
srn|Sranan Tongo|ISO
suk|Sukuma|ISO
sux|Sumerian|ISO
sun|Sundanese|ISO
sus|Susu|ISO
swa|Swahili|ISO
ssw|Swati|ISO
swe|Swedish|ISO
gsw|Swiss German|ISO
gsw|Alemannic|ISO
gsw|Alsatian|ISO
syr|Syriac|ISO
tgl|Tagalog|ISO
tah|Tahitian|ISO
tai|Tai (Other)|ISO
tgk|Tajik|ISO
tmh|Tamashek|ISO
tam|Tamil|ISO
tat|Tatar|ISO
tel|Telugu|ISO
ter|Tereno|ISO
tet|Tetum|ISO
tha|Thai|ISO
tib|Tibetan|ISO
tig|Tigre|ISO
tir|Tigrinya|ISO
tem|Timne|ISO
tiv|Tiv|ISO
tli|Tlingit|ISO
tpi|Tok Pisin|ISO
tkl|Tokelau|ISO
tog|Tonga (Nyasa)|ISO
ton|Tonga (Tonga Islands)|ISO
tsi|Tsimshian|ISO
tso|Tsonga|ISO
tsn|Tswana|ISO
tum|Tumbuka|ISO
tup|Tupi languages|ISO
tur|Turkish|ISO
tuk|Turkmen|ISO
tvl|Tuvalu|ISO
tyv|Tuvinian|ISO
twi|Twi|ISO
udm|Udmurt|ISO
uga|Ugaritic|ISO
uig|Uighur|ISO
uig|Uyghur|ISO
ukr|Ukrainian|ISO
umb|Umbundu|ISO
mis|Uncoded languages|ISO
und|Undetermined|ISO
hsb|Upper Sorbian|ISO
urd|Urdu|ISO
uzb|Uzbek|ISO
vai|Vai|ISO
ven|Venda|ISO
vie|Vietnamese|ISO
vol|Volapük|ISO
vot|Votic|ISO
wak|Wakashan languages|ISO
wal|Walamo|ISO
wln|Walloon|ISO
war|Waray|ISO
was|Washo|ISO
wel|Welsh|ISO
fry|Western Frisian|ISO
wol|Wolof|ISO
xho|Xhosa|ISO
sah|Yakut|ISO
yao|Yao|ISO
yap|Yapese|ISO
yid|Yiddish|ISO
yor|Yoruba|ISO
ypk|Yupik languages|ISO
znd|Zande languages|ISO
zap|Zapotec|ISO
zza|Zaza; Dimili; Dimli; Kirdki; Kirmanjki; Zazaki|ISO
zen|Zenaga|ISO
zha|Zhuang; Chuang|ISO
zul|Zulu|ISO
zun|Zuni|ISO

**Documentation**:
The patient's preferred language
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

**Documentation**:
The patient's race. There is a common valueset defined for race values, but some states may choose to define different code/value combinations.

---

**Name**: patient_state

**ReportStream Internal Name**: patient_state

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:
The patient's state
---

**Name**: patient_state_zip_lookup

**ReportStream Internal Name**: patient_state_zip_lookup

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: zip-code-data

**Table Column**: state_abbr

---

**Name**: patient_street

**ReportStream Internal Name**: patient_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's street address
---

**Name**: patient_street2

**ReportStream Internal Name**: patient_street2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's second address line
---

**Name**: patient_suffix

**ReportStream Internal Name**: patient_suffix

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The suffix for the patient's name, (i.e. Jr, Sr, etc)
---

**Name**: patient_tribal_citizenship

**ReportStream Internal Name**: patient_tribal_citizenship

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
338|Village of Afognak|HL7
339|Agdaagux Tribe of King Cove|HL7
340|Native Village of Akhiok|HL7
341|Akiachak Native Community|HL7
342|Akiak Native Community|HL7
343|Native Village of Akutan|HL7
344|Village of Alakanuk|HL7
345|Alatna Village|HL7
346|Native Village of Aleknagik|HL7
347|Algaaciq Native Village (St. Mary's)|HL7
348|Allakaket Village|HL7
349|Native Village of Ambler|HL7
350|Village of Anaktuvuk Pass|HL7
351|Yupiit of Andreafski|HL7
352|Angoon Community Association|HL7
353|Village of Aniak|HL7
354|Anvik Village|HL7
355|Arctic Village (See Native Village of Venetie Trib|HL7
356|Asa carsarmiut Tribe (formerly Native Village of M|HL7
357|Native Village of Atka|HL7
358|Village of Atmautluak|HL7
359|Atqasuk Village (Atkasook)|HL7
360|Native Village of Barrow Inupiat Traditional Gover|HL7
361|Beaver Village|HL7
362|Native Village of Belkofski|HL7
363|Village of Bill Moore's Slough|HL7
364|Birch Creek Tribe|HL7
365|Native Village of Brevig Mission|HL7
366|Native Village of Buckland|HL7
367|Native Village of Cantwell|HL7
368|Native Village of Chanega (aka Chenega)|HL7
369|Chalkyitsik Village|HL7
370|Village of Chefornak|HL7
371|Chevak Native Village|HL7
372|Chickaloon Native Village|HL7
373|Native Village of Chignik|HL7
374|Native Village of Chignik Lagoon|HL7
375|Chignik Lake Village|HL7
376|Chilkat Indian Village (Klukwan)|HL7
377|Chilkoot Indian Association (Haines)|HL7
378|Chinik Eskimo Community (Golovin)|HL7
379|Native Village of Chistochina|HL7
380|Native Village of Chitina|HL7
381|Native Village of Chuathbaluk (Russian Mission, Ku|HL7
382|Chuloonawick Native Village|HL7
383|Circle Native Community|HL7
384|Village of Clark's Point|HL7
385|Native Village of Council|HL7
386|Craig Community Association|HL7
387|Village of Crooked Creek|HL7
388|Curyung Tribal Council (formerly Native Village of|HL7
389|Native Village of Deering|HL7
390|Native Village of Diomede (aka Inalik)|HL7
391|Village of Dot Lake|HL7
392|Douglas Indian Association|HL7
393|Native Village of Eagle|HL7
394|Native Village of Eek|HL7
395|Egegik Village|HL7
396|Eklutna Native Village|HL7
397|Native Village of Ekuk|HL7
398|Ekwok Village|HL7
399|Native Village of Elim|HL7
400|Emmonak Village|HL7
401|Evansville Village (aka Bettles Field)|HL7
402|Native Village of Eyak (Cordova)|HL7
403|Native Village of False Pass|HL7
404|Native Village of Fort Yukon|HL7
405|Native Village of Gakona|HL7
406|Galena Village (aka Louden Village)|HL7
407|Native Village of Gambell|HL7
408|Native Village of Georgetown|HL7
409|Native Village of Goodnews Bay|HL7
410|Organized Village of Grayling (aka Holikachuk)|HL7
411|Gulkana Village|HL7
412|Native Village of Hamilton|HL7
413|Healy Lake Village|HL7
414|Holy Cross Village|HL7
415|Hoonah Indian Association|HL7
416|Native Village of Hooper Bay|HL7
417|Hughes Village|HL7
418|Huslia Village|HL7
419|Hydaburg Cooperative Association|HL7
420|Igiugig Village|HL7
421|Village of Iliamna|HL7
422|Inupiat Community of the Arctic Slope|HL7
423|Iqurmuit Traditional Council (formerly Native Vill|HL7
424|Ivanoff Bay Village|HL7
425|Kaguyak Village|HL7
426|Organized Village of Kake|HL7
427|Kaktovik Village (aka Barter Island)|HL7
428|Village of Kalskag|HL7
429|Village of Kaltag|HL7
430|Native Village of Kanatak|HL7
431|Native Village of Karluk|HL7
432|Organized Village of Kasaan|HL7
433|Native Village of Kasigluk|HL7
434|Kenaitze Indian Tribe|HL7
435|Ketchikan Indian Corporation|HL7
436|Native Village of Kiana|HL7
437|King Island Native Community|HL7
438|King Salmon Tribe|HL7
439|Native Village of Kipnuk|HL7
440|Native Village of Kivalina|HL7
441|Klawock Cooperative Association|HL7
442|Native Village of Kluti Kaah (aka Copper Center)|HL7
443|Knik Tribe|HL7
444|Native Village of Kobuk|HL7
445|Kokhanok Village|HL7
446|Native Village of Kongiganak|HL7
447|Village of Kotlik|HL7
448|Native Village of Kotzebue|HL7
449|Native Village of Koyuk|HL7
450|Koyukuk Native Village|HL7
451|Organized Village of Kwethluk|HL7
452|Native Village of Kwigillingok|HL7
453|Native Village of Kwinhagak (aka Quinhagak)|HL7
454|Native Village of Larsen Bay|HL7
455|Levelock Village|HL7
456|Lesnoi Village (aka Woody Island)|HL7
457|Lime Village|HL7
458|Village of Lower Kalskag|HL7
459|Manley Hot Springs Village|HL7
460|Manokotak Village|HL7
461|Native Village of Marshall (aka Fortuna Ledge)|HL7
462|Native Village of Mary's Igloo|HL7
463|McGrath Native Village|HL7
464|Native Village of Mekoryuk|HL7
465|Mentasta Traditional Council|HL7
466|Metlakatla Indian Community, Annette Island Reserv|HL7
467|Native Village of Minto|HL7
468|Naknek Native Village|HL7
469|Native Village of Nanwalek (aka English Bay)|HL7
470|Native Village of Napaimute|HL7
471|Native Village of Napakiak|HL7
472|Native Village of Napaskiak|HL7
473|Native Village of Nelson Lagoon|HL7
474|Nenana Native Association|HL7
475|New Koliganek Village Council (formerly Koliganek|HL7
476|New Stuyahok Village|HL7
477|Newhalen Village|HL7
478|Newtok Village|HL7
479|Native Village of Nightmute|HL7
480|Nikolai Village|HL7
481|Native Village of Nikolski|HL7
482|Ninilchik Village|HL7
483|Native Village of Noatak|HL7
484|Nome Eskimo Community|HL7
485|Nondalton Village|HL7
486|Noorvik Native Community|HL7
487|Northway Village|HL7
488|Native Village of Nuiqsut (aka Nooiksut)|HL7
489|Nulato Village|HL7
490|Nunakauyarmiut Tribe (formerly Native Village of T|HL7
491|Native Village of Nunapitchuk|HL7
492|Village of Ohogamiut|HL7
493|Village of Old Harbor|HL7
494|Orutsararmuit Native Village (aka Bethel)|HL7
495|Oscarville Traditional Village|HL7
496|Native Village of Ouzinkie|HL7
497|Native Village of Paimiut|HL7
498|Pauloff Harbor Village|HL7
499|Pedro Bay Village|HL7
500|Native Village of Perryville|HL7
501|Petersburg Indian Association|HL7
502|Native Village of Pilot Point|HL7
503|Pilot Station Traditional Village|HL7
504|Native Village of Pitka's Point|HL7
505|Platinum Traditional Village|HL7
506|Native Village of Point Hope|HL7
507|Native Village of Point Lay|HL7
508|Native Village of Port Graham|HL7
509|Native Village of Port Heiden|HL7
510|Native Village of Port Lions|HL7
511|Portage Creek Village (aka Ohgsenakale)|HL7
512|Pribilof Islands Aleut Communities of St. Paul & S|HL7
513|Qagan Tayagungin Tribe of Sand Point Village|HL7
514|Qawalangin Tribe of Unalaska|HL7
515|Rampart Village|HL7
516|Village of Red Devil|HL7
517|Native Village of Ruby|HL7
518|Saint George Island(See Pribilof Islands Aleut Com|HL7
519|Native Village of Saint Michael|HL7
520|Saint Paul Island (See Pribilof Islands Aleut Comm|HL7
521|Village of Salamatoff|HL7
522|Native Village of Savoonga|HL7
523|Organized Village of Saxman|HL7
524|Native Village of Scammon Bay|HL7
525|Native Village of Selawik|HL7
526|Seldovia Village Tribe|HL7
527|Shageluk Native Village|HL7
528|Native Village of Shaktoolik|HL7
529|Native Village of Sheldon's Point|HL7
530|Native Village of Shishmaref|HL7
531|Shoonaq Tribe of Kodiak|HL7
532|Native Village of Shungnak|HL7
533|Sitka Tribe of Alaska|HL7
534|Skagway Village|HL7
535|Village of Sleetmute|HL7
536|Village of Solomon|HL7
537|South Naknek Village|HL7
538|Stebbins Community Association|HL7
539|Native Village of Stevens|HL7
540|Village of Stony River|HL7
541|Takotna Village|HL7
542|Native Village of Tanacross|HL7
543|Native Village of Tanana|HL7
544|Native Village of Tatitlek|HL7
545|Native Village of Tazlina|HL7
546|Telida Village|HL7
547|Native Village of Teller|HL7
548|Native Village of Tetlin|HL7
549|Central Council of the Tlingit and Haida Indian Tb|HL7
550|Traditional Village of Togiak|HL7
551|Tuluksak Native Community|HL7
552|Native Village of Tuntutuliak|HL7
553|Native Village of Tununak|HL7
554|Twin Hills Village|HL7
555|Native Village of Tyonek|HL7
556|Ugashik Village|HL7
557|Umkumiute Native Village|HL7
558|Native Village of Unalakleet|HL7
559|Native Village of Unga|HL7
560|Village of Venetie (See Native Village of Venetie|HL7
561|Native Village of Venetie Tribal Government (Arcti|HL7
562|Village of Wainwright|HL7
563|Native Village of Wales|HL7
564|Native Village of White Mountain|HL7
565|Wrangell Cooperative Association|HL7
566|Yakutat Tlingit Tribe|HL7
1|Absentee-Shawnee Tribe of Indians of Oklahoma|HL7
10|Assiniboine and Sioux Tribes of the Fort Peck Indi|HL7
100|Havasupai Tribe of the Havasupai Reservation, Ariz|HL7
101|Ho-Chunk Nation of Wisconsin (formerly known as th|HL7
102|Hoh Indian Tribe of the Hoh Indian Reservation, Wa|HL7
103|Hoopa Valley Tribe, California|HL7
104|Hopi Tribe of Arizona|HL7
105|Hopland Band of Pomo Indians of the Hopland Ranche|HL7
106|Houlton Band of Maliseet Indians of Maine|HL7
107|Hualapai Indian Tribe of the Hualapai Indian Reser|HL7
108|Huron Potawatomi, Inc., Michigan|HL7
109|Inaja Band of Diegueno Mission Indians of the Inaj|HL7
11|Augustine Band of Cahuilla Mission Indians of the|HL7
110|Ione Band of Miwok Indians of California|HL7
111|Iowa Tribe of Kansas and Nebraska|HL7
112|Iowa Tribe of Oklahoma|HL7
113|Jackson Rancheria of Me-Wuk Indians of California|HL7
114|Jamestown S'Klallam Tribe of Washington|HL7
115|Jamul Indian Village of California|HL7
116|Jena Band of Choctaw Indians, Louisiana|HL7
117|Jicarilla Apache Tribe of the Jicarilla Apache Ind|HL7
118|Kaibab Band of Paiute Indians of the Kaibab Indian|HL7
119|Kalispel Indian Community of the Kalispel Reservat|HL7
12|Bad River Band of the Lake Superior Tribe of Chipp|HL7
120|Karuk Tribe of California|HL7
121|Kashia Band of Pomo Indians of the Stewarts Point|HL7
122|Kaw Nation, Oklahoma|HL7
123|Keweenaw Bay Indian Community of L'Anse and Ontona|HL7
124|Kialegee Tribal Town, Oklahoma|HL7
125|Kickapoo Tribe of Indians of the Kickapoo Reservat|HL7
126|Kickapoo Tribe of Oklahoma|HL7
127|Kickapoo Traditional Tribe of Texas|HL7
128|Kiowa Indian Tribe of Oklahoma|HL7
129|Klamath Indian Tribe of Oregon|HL7
13|Bay Mills Indian Community of the Sault Ste. Marie|HL7
130|Kootenai Tribe of Idaho|HL7
131|La Jolla Band of Luiseno Mission Indians of the La|HL7
132|La Posta Band of Diegueno Mission Indians of the L|HL7
133|Lac Courte Oreilles Band of Lake Superior Chippewa|HL7
134|Lac du Flambeau Band of Lake Superior Chippewa Ind|HL7
135|Lac Vieux Desert Band of Lake Superior Chippewa In|HL7
136|Las Vegas Tribe of Paiute Indians of the Las Vegas|HL7
137|Little River Band of Ottawa Indians of Michigan|HL7
138|Little Traverse Bay Bands of Odawa Indians of Mich|HL7
139|Lower Lake Rancheria, California|HL7
14|Bear River Band of the Rohnerville Rancheria, Cali|HL7
140|Los Coyotes Band of Cahuilla Mission Indians of th|HL7
141|Lovelock Paiute Tribe of the Lovelock Indian Colon|HL7
142|Lower Brule Sioux Tribe of the Lower Brule Reserva|HL7
143|Lower Elwha Tribal Community of the Lower Elwha Re|HL7
144|Lower Sioux Indian Community of Minnesota Mdewakan|HL7
145|Lummi Tribe of the Lummi Reservation, Washington|HL7
146|Lytton Rancheria of California|HL7
147|Makah Indian Tribe of the Makah Indian Reservation|HL7
148|Manchester Band of Pomo Indians of the Manchester-|HL7
149|Manzanita Band of Diegueno Mission Indians of the|HL7
15|Berry Creek Rancheria of Maidu Indians of Californ|HL7
150|Mashantucket Pequot Tribe of Connecticut|HL7
151|Match-e-be-nash-she-wish Band of Pottawatomi India|HL7
152|Mechoopda Indian Tribe of Chico Rancheria, Califor|HL7
153|Menominee Indian Tribe of Wisconsin|HL7
154|Mesa Grande Band of Diegueno Mission Indians of th|HL7
155|Mescalero Apache Tribe of the Mescalero Reservatio|HL7
156|Miami Tribe of Oklahoma|HL7
157|Miccosukee Tribe of Indians of Florida|HL7
158|Middletown Rancheria of Pomo Indians of California|HL7
159|Minnesota Chippewa Tribe, Minnesota (Six component|HL7
16|Big Lagoon Rancheria, California|HL7
160|Bois Forte Band (Nett Lake); Fond du Lac Band; Gra|HL7
161|Mississippi Band of Choctaw Indians, Mississippi|HL7
162|Moapa Band of Paiute Indians of the Moapa River In|HL7
163|Modoc Tribe of Oklahoma|HL7
164|Mohegan Indian Tribe of Connecticut|HL7
165|Mooretown Rancheria of Maidu Indians of California|HL7
166|Morongo Band of Cahuilla Mission Indians of the Mo|HL7
167|Muckleshoot Indian Tribe of the Muckleshoot Reserv|HL7
168|Muscogee (Creek) Nation, Oklahoma|HL7
169|Narragansett Indian Tribe of Rhode Island|HL7
17|Big Pine Band of Owens Valley Paiute Shoshone Indi|HL7
170|Navajo Nation, Arizona, New Mexico & Utah|HL7
171|Nez Perce Tribe of Idaho|HL7
172|Nisqually Indian Tribe of the Nisqually Reservatio|HL7
173|Nooksack Indian Tribe of Washington|HL7
174|Northern Cheyenne Tribe of the Northern Cheyenne I|HL7
175|Northfork Rancheria of Mono Indians of California|HL7
176|Northwestern Band of Shoshoni Nation of Utah (Wash|HL7
177|Oglala Sioux Tribe of the Pine Ridge Reservation,|HL7
178|Omaha Tribe of Nebraska|HL7
179|Oneida Nation of New York|HL7
18|Big Sandy Rancheria of Mono Indians of California|HL7
180|Oneida Tribe of Wisconsin|HL7
181|Onondaga Nation of New York|HL7
182|Osage Tribe, Oklahoma|HL7
183|Ottawa Tribe of Oklahoma|HL7
184|Otoe-Missouria Tribe of Indians, Oklahoma|HL7
185|Paiute Indian Tribe of Utah|HL7
186|Paiute-Shoshone Indians of the Bishop Community of|HL7
187|Paiute-Shoshone Tribe of the Fallon Reservation an|HL7
188|Paiute-Shoshone Indians of the Lone Pine Community|HL7
189|Pala Band of Luiseno Mission Indians of the Pala R|HL7
19|Big Valley Band of Pomo Indians of the Big Valley|HL7
190|Pascua Yaqui Tribe of Arizona|HL7
191|Paskenta Band of Nomlaki Indians of California|HL7
192|Passamaquoddy Tribe of Maine|HL7
193|Pauma Band of Luiseno Mission Indians of the Pauma|HL7
194|Pawnee Nation of Oklahoma|HL7
195|Pechanga Band of Luiseno Mission Indians of the Pe|HL7
196|Penobscot Tribe of Maine|HL7
197|Peoria Tribe of Indians of Oklahoma|HL7
198|Picayune Rancheria of Chukchansi Indians of Califo|HL7
199|Pinoleville Rancheria of Pomo Indians of Californi|HL7
2|Agua Caliente Band of Cahuilla Indians of the Agua|HL7
20|Blackfeet Tribe of the Blackfeet Indian Reservatio|HL7
200|Pit River Tribe, California (includes Big Bend, Lo|HL7
201|Poarch Band of Creek Indians of Alabama|HL7
202|Pokagon Band of Potawatomi Indians of Michigan|HL7
203|Ponca Tribe of Indians of Oklahoma|HL7
204|Ponca Tribe of Nebraska|HL7
205|Port Gamble Indian Community of the Port Gamble Re|HL7
206|Potter Valley Rancheria of Pomo Indians of Califor|HL7
207|Prairie Band of Potawatomi Indians, Kansas|HL7
208|Prairie Island Indian Community of Minnesota Mdewa|HL7
209|Pueblo of Acoma, New Mexico|HL7
21|Blue Lake Rancheria, California|HL7
210|Pueblo of Cochiti, New Mexico|HL7
211|Pueblo of Jemez, New Mexico|HL7
212|Pueblo of Isleta, New Mexico|HL7
213|Pueblo of Laguna, New Mexico|HL7
214|Pueblo of Nambe, New Mexico|HL7
215|Pueblo of Picuris, New Mexico|HL7
216|Pueblo of Pojoaque, New Mexico|HL7
217|Pueblo of San Felipe, New Mexico|HL7
218|Pueblo of San Juan, New Mexico|HL7
219|Pueblo of San Ildefonso, New Mexico|HL7
22|Bridgeport Paiute Indian Colony of California|HL7
220|Pueblo of Sandia, New Mexico|HL7
221|Pueblo of Santa Ana, New Mexico|HL7
222|Pueblo of Santa Clara, New Mexico|HL7
223|Pueblo of Santo Domingo, New Mexico|HL7
224|Pueblo of Taos, New Mexico|HL7
225|Pueblo of Tesuque, New Mexico|HL7
226|Pueblo of Zia, New Mexico|HL7
227|Puyallup Tribe of the Puyallup Reservation, Washin|HL7
228|Pyramid Lake Paiute Tribe of the Pyramid Lake Rese|HL7
229|Quapaw Tribe of Indians, Oklahoma|HL7
23|Buena Vista Rancheria of Me-Wuk Indians of Califor|HL7
230|Quartz Valley Indian Community of the Quartz Valle|HL7
231|Quechan Tribe of the Fort Yuma Indian Reservation,|HL7
232|Quileute Tribe of the Quileute Reservation, Washin|HL7
233|Quinault Tribe of the Quinault Reservation, Washin|HL7
234|Ramona Band or Village of Cahuilla Mission Indians|HL7
235|Red Cliff Band of Lake Superior Chippewa Indians o|HL7
236|Red Lake Band of Chippewa Indians of the Red Lake|HL7
237|Redding Rancheria, California|HL7
238|Redwood Valley Rancheria of Pomo Indians of Califo|HL7
239|Reno-Sparks Indian Colony, Nevada|HL7
24|Burns Paiute Tribe of the Burns Paiute Indian Colo|HL7
240|Resighini Rancheria, California (formerly known as|HL7
241|Rincon Band of Luiseno Mission Indians of the Rinc|HL7
242|Robinson Rancheria of Pomo Indians of California|HL7
243|Rosebud Sioux Tribe of the Rosebud Indian Reservat|HL7
244|Round Valley Indian Tribes of the Round Valley Res|HL7
245|Rumsey Indian Rancheria of Wintun Indians of Calif|HL7
246|Sac and Fox Tribe of the Mississippi in Iowa|HL7
247|Sac and Fox Nation of Missouri in Kansas and Nebra|HL7
248|Sac and Fox Nation, Oklahoma|HL7
249|Saginaw Chippewa Indian Tribe of Michigan, Isabell|HL7
25|Cabazon Band of Cahuilla Mission Indians of the Ca|HL7
250|Salt River Pima-Maricopa Indian Community of the S|HL7
251|Samish Indian Tribe, Washington|HL7
252|San Carlos Apache Tribe of the San Carlos Reservat|HL7
253|San Juan Southern Paiute Tribe of Arizona|HL7
254|San Manual Band of Serrano Mission Indians of the|HL7
255|San Pasqual Band of Diegueno Mission Indians of Ca|HL7
256|Santa Rosa Indian Community of the Santa Rosa Ranc|HL7
257|Santa Rosa Band of Cahuilla Mission Indians of the|HL7
258|Santa Ynez Band of Chumash Mission Indians of the|HL7
259|Santa Ysabel Band of Diegueno Mission Indians of t|HL7
26|Cachil DeHe Band of Wintun Indians of the Colusa I|HL7
260|Santee Sioux Tribe of the Santee Reservation of Ne|HL7
261|Sauk-Suiattle Indian Tribe of Washington|HL7
262|Sault Ste. Marie Tribe of Chippewa Indians of Mich|HL7
263|Scotts Valley Band of Pomo Indians of California|HL7
264|Seminole Nation of Oklahoma|HL7
265|Seminole Tribe of Florida, Dania, Big Cypress, Bri|HL7
266|Seneca Nation of New York|HL7
267|Seneca-Cayuga Tribe of Oklahoma|HL7
268|Shakopee Mdewakanton Sioux Community of Minnesota|HL7
269|Shawnee Tribe, Oklahoma|HL7
27|Caddo Indian Tribe of Oklahoma|HL7
270|Sherwood Valley Rancheria of Pomo Indians of Calif|HL7
271|Shingle Springs Band of Miwok Indians, Shingle Spr|HL7
272|Shoalwater Bay Tribe of the Shoalwater Bay Indian|HL7
273|Shoshone Tribe of the Wind River Reservation, Wyom|HL7
274|Shoshone-Bannock Tribes of the Fort Hall Reservati|HL7
275|Shoshone-Paiute Tribes of the Duck Valley Reservat|HL7
276|Sisseton-Wahpeton Sioux Tribe of the Lake Traverse|HL7
277|Skokomish Indian Tribe of the Skokomish Reservatio|HL7
278|Skull Valley Band of Goshute Indians of Utah|HL7
279|Smith River Rancheria, California|HL7
28|Cahuilla Band of Mission Indians of the Cahuilla R|HL7
280|Snoqualmie Tribe, Washington|HL7
281|Soboba Band of Luiseno Indians, California (former|HL7
282|Sokaogon Chippewa Community of the Mole Lake Band|HL7
283|Southern Ute Indian Tribe of the Southern Ute Rese|HL7
284|Spirit Lake Tribe, North Dakota (formerly known as|HL7
285|Spokane Tribe of the Spokane Reservation, Washingt|HL7
286|Squaxin Island Tribe of the Squaxin Island Reserva|HL7
287|St. Croix Chippewa Indians of Wisconsin, St. Croix|HL7
288|St. Regis Band of Mohawk Indians of New York|HL7
289|Standing Rock Sioux Tribe of North & South Dakota|HL7
29|Cahto Indian Tribe of the Laytonville Rancheria, C|HL7
290|Stockbridge-Munsee Community of Mohican Indians of|HL7
291|Stillaguamish Tribe of Washington|HL7
292|Summit Lake Paiute Tribe of Nevada|HL7
293|Suquamish Indian Tribe of the Port Madison Reserva|HL7
294|Susanville Indian Rancheria, California|HL7
295|Swinomish Indians of the Swinomish Reservation, Wa|HL7
296|Sycuan Band of Diegueno Mission Indians of Califor|HL7
297|Table Bluff Reservation - Wiyot Tribe, California|HL7
298|Table Mountain Rancheria of California|HL7
299|Te-Moak Tribe of Western Shoshone Indians of Nevad|HL7
3|Ak Chin Indian Community of the Maricopa (Ak Chin)|HL7
30|California Valley Miwok Tribe, California (formerl|HL7
300|Thlopthlocco Tribal Town, Oklahoma|HL7
301|Three Affiliated Tribes of the Fort Berthold Reser|HL7
302|Tohono O'odham Nation of Arizona|HL7
303|Tonawanda Band of Seneca Indians of New York|HL7
304|Tonkawa Tribe of Indians of Oklahoma|HL7
305|Tonto Apache Tribe of Arizona|HL7
306|Torres-Martinez Band of Cahuilla Mission Indians o|HL7
307|Tule River Indian Tribe of the Tule River Reservat|HL7
308|Tulalip Tribes of the Tulalip Reservation, Washing|HL7
309|Tunica-Biloxi Indian Tribe of Louisiana|HL7
31|Campo Band of Diegueno Mission Indians of the Camp|HL7
310|Tuolumne Band of Me-Wuk Indians of the Tuolumne Ra|HL7
311|Turtle Mountain Band of Chippewa Indians of North|HL7
312|Tuscarora Nation of New York|HL7
313|Twenty-Nine Palms Band of Mission Indians of Calif|HL7
314|United Auburn Indian Community of the Auburn Ranch|HL7
315|United Keetoowah Band of Cherokee Indians of Oklah|HL7
316|Upper Lake Band of Pomo Indians of Upper Lake Ranc|HL7
317|Upper Sioux Indian Community of the Upper Sioux Re|HL7
318|Upper Skagit Indian Tribe of Washington|HL7
319|Ute Indian Tribe of the Uintah & Ouray Reservation|HL7
32|Capitan Grande Band of Diegueno Mission Indians of|HL7
320|Ute Mountain Tribe of the Ute Mountain Reservation|HL7
321|Utu Utu Gwaitu Paiute Tribe of the Benton Paiute R|HL7
322|Walker River Paiute Tribe of the Walker River Rese|HL7
323|Wampanoag Tribe of Gay Head (Aquinnah) of Massachu|HL7
324|Washoe Tribe of Nevada & California (Carson Colony|HL7
325|White Mountain Apache Tribe of the Fort Apache Res|HL7
326|Wichita and Affiliated Tribes (Wichita, Keechi, Wa|HL7
327|Winnebago Tribe of Nebraska|HL7
328|Winnemucca Indian Colony of Nevada|HL7
329|Wyandotte Tribe of Oklahoma|HL7
33|Barona Group of Capitan Grande Band of Mission Ind|HL7
330|Yankton Sioux Tribe of South Dakota|HL7
331|Yavapai-Apache Nation of the Camp Verde Indian Res|HL7
332|Yavapai-Prescott Tribe of the Yavapai Reservation,|HL7
333|Yerington Paiute Tribe of the Yerington Colony & C|HL7
334|Yomba Shoshone Tribe of the Yomba Reservation, Nev|HL7
335|Ysleta Del Sur Pueblo of Texas|HL7
336|Yurok Tribe of the Yurok Reservation, California|HL7
337|Zuni Tribe of the Zuni Reservation, New Mexico|HL7
34|Viejas (Baron Long) Group of Capitan Grande Band o|HL7
35|Catawba Indian Nation (aka Catawba Tribe of South|HL7
36|Cayuga Nation of New York|HL7
37|Cedarville Rancheria, California|HL7
38|Chemehuevi Indian Tribe of the Chemehuevi Reservat|HL7
39|Cher-Ae Heights Indian Community of the Trinidad R|HL7
4|Alabama-Coushatta Tribes of Texas|HL7
40|Cherokee Nation, Oklahoma|HL7
41|Cheyenne-Arapaho Tribes of Oklahoma|HL7
42|Cheyenne River Sioux Tribe of the Cheyenne River|HL7
43|Chickasaw Nation, Oklahoma|HL7
44|Chicken Ranch Rancheria of Me-Wuk Indians of Calif|HL7
45|Chippewa-Cree Indians of the Rocky Boy's Reservati|HL7
46|Chitimacha Tribe of Louisiana|HL7
47|Choctaw Nation of Oklahoma|HL7
48|Citizen Potawatomi Nation, Oklahoma|HL7
49|Cloverdale Rancheria of Pomo Indians of California|HL7
5|Alabama-Quassarte Tribal Town, Oklahoma|HL7
50|Cocopah Tribe of Arizona|HL7
51|Coeur D'Alene Tribe of the Coeur D'Alene Reservati|HL7
52|Cold Springs Rancheria of Mono Indians of Californ|HL7
53|Colorado River Indian Tribes of the Colorado River|HL7
54|Comanche Indian Tribe, Oklahoma|HL7
55|Confederated Salish & Kootenai Tribes of the Flath|HL7
56|Confederated Tribes of the Chehalis Reservation, W|HL7
57|Confederated Tribes of the Colville Reservation, W|HL7
58|Confederated Tribes of the Coos, Lower Umpqua and|HL7
59|Confederated Tribes of the Goshute Reservation, Ne|HL7
6|Alturas Indian Rancheria, California|HL7
60|Confederated Tribes of the Grand Ronde Community o|HL7
61|Confederated Tribes of the Siletz Reservation, Ore|HL7
62|Confederated Tribes of the Umatilla Reservation, O|HL7
63|Confederated Tribes of the Warm Springs Reservatio|HL7
64|Confederated Tribes and Bands of the Yakama Indian|HL7
65|Coquille Tribe of Oregon|HL7
66|Cortina Indian Rancheria of Wintun Indians of Cali|HL7
67|Coushatta Tribe of Louisiana|HL7
68|Cow Creek Band of Umpqua Indians of Oregon|HL7
69|Coyote Valley Band of Pomo Indians of California|HL7
7|Apache Tribe of Oklahoma|HL7
70|Crow Tribe of Montana|HL7
71|Crow Creek Sioux Tribe of the Crow Creek Reservati|HL7
72|Cuyapaipe Community of Diegueno Mission Indians of|HL7
73|Death Valley Timbi-Sha Shoshone Band of California|HL7
74|Delaware Nation, Oklahoma (formerly Delaware Tribe|HL7
75|Delaware Tribe of Indians, Oklahoma|HL7
76|Dry Creek Rancheria of Pomo Indians of California|HL7
77|Duckwater Shoshone Tribe of the Duckwater Reservat|HL7
78|Eastern Band of Cherokee Indians of North Carolina|HL7
79|Eastern Shawnee Tribe of Oklahoma|HL7
8|Arapahoe Tribe of the Wind River Reservation, Wyom|HL7
80|Elem Indian Colony of Pomo Indians of the Sulphur|HL7
81|Elk Valley Rancheria, California|HL7
82|Ely Shoshone Tribe of Nevada|HL7
83|Enterprise Rancheria of Maidu Indians of Californi|HL7
84|Flandreau Santee Sioux Tribe of South Dakota|HL7
85|Forest County Potawatomi Community of Wisconsin Po|HL7
86|Fort Belknap Indian Community of the Fort Belknap|HL7
87|Fort Bidwell Indian Community of the Fort Bidwell|HL7
88|Fort Independence Indian Community of Paiute India|HL7
89|Fort McDermitt Paiute and Shoshone Tribes of the F|HL7
9|Aroostook Band of Micmac Indians of Maine|HL7
90|Fort McDowell Yavapai Nation, Arizona (formerly th|HL7
91|Fort Mojave Indian Tribe of Arizona, California|HL7
92|Fort Sill Apache Tribe of Oklahoma|HL7
93|Gila River Indian Community of the Gila River Indi|HL7
94|Grand Traverse Band of Ottawa & Chippewa Indians o|HL7
95|Graton Rancheria, California|HL7
96|Greenville Rancheria of Maidu Indians of Californi|HL7
97|Grindstone Indian Rancheria of Wintun-Wailaki Indi|HL7
98|Guidiville Rancheria of California|HL7
99|Hannahville Indian Community of Wisconsin Potawato|HL7

**Documentation**:
The tribal citizenship of the patient using the TribalEntityUS (OID 2.16.840.1.113883.5.140) table
---

**Name**: patient_zip_code

**ReportStream Internal Name**: patient_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The patient's zip code
---

**Name**: placer_clia

**ReportStream Internal Name**: placer_clia

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**

- [OBR-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.3)
- [ORC-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.3)
- [ORC-4-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.4.3)

**Cardinality**: [0..1]

**Documentation**:
The CLIA of the order placer
---

**Name**: placer_name

**ReportStream Internal Name**: placer_name

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [OBR-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.2)
- [ORC-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.2)
- [ORC-4-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.4.2)

**Cardinality**: [0..1]

**Documentation**:
The name of the placer of the lab order
---

**Name**: placer_order_group_id

**ReportStream Internal Name**: placer_order_group_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

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

**Name**: prescription_home_testing

**ReportStream Internal Name**: prescription_home_testing

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Prescription Home Testing

---

**Name**: previous_message_id

**ReportStream Internal Name**: previous_message_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
pointer/link to the unique id of a previously submitted result.  Usually blank. Or, if an item modifies/corrects a prior item, this field holds the message_id of the prior item.
---

**Name**: prime_patient_id

**ReportStream Internal Name**: prime_patient_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

---

**Name**: prime_patient_id_assigner

**ReportStream Internal Name**: prime_patient_id_assigner

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

---

**Name**: processing_mode_code

**ReportStream Internal Name**: processing_mode_code

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Default Value**: P

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
D|Debugging|HL7
P|Production|HL7
T|Training|HL7

**Documentation**:
P, D, or T for Production, Debugging, or Training
---

**Name**: reason_for_study

**ReportStream Internal Name**: reason_for_study

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: receiving_application

**ReportStream Internal Name**: receiving_application

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The receiving application for the message (specified by the receiver)
---

**Name**: receiving_facility

**ReportStream Internal Name**: receiving_facility

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The receiving facility for the message (specified by the receiver)
---

**Name**: reference_range

**ReportStream Internal Name**: reference_range

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The reference range of the lab result, such as “Negative” or “Normal”. For IgG, IgM and CT results that provide a value you MUST fill out this filed.
---

**Name**: reporting_facility

**ReportStream Internal Name**: reporting_facility

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The reporting facility for the message, as specified by the receiver. This is typically used if PRIME is the
aggregator

---

**Name**: reporting_facility_clia

**ReportStream Internal Name**: reporting_facility_clia

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**

- [MSH-4-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/MSH.4.2)
- [PID-3-4-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4.2)
- [PID-3-6-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.6.2)
- [SPM-2-1-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.1.3)
- [SPM-2-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.2.3)

**Cardinality**: [0..1]

**Documentation**:
The reporting facility's CLIA
---

**Name**: reporting_facility_name

**ReportStream Internal Name**: reporting_facility_name

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [MSH-4-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/MSH.4.1)
- [PID-3-4-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4.1)
- [PID-3-6-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.6.1)
- [SPM-2-1-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.1.2)
- [SPM-2-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.2.2)

**Cardinality**: [0..1]

**Documentation**:
The reporting facility's name
---

**Name**: residence_type

**ReportStream Internal Name**: residence_type

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 75617-1

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
22232009|Hospital|SNOMED_CT
2081004|Hospital Ship|SNOMED_CT
32074000|Long Term Care Hospital|SNOMED_CT
224929004|Secure Hospital|SNOMED_CT
42665001|Nursing Home|SNOMED_CT
30629002|Retirement Home|SNOMED_CT
74056004|Orphanage|SNOMED_CT
722173008|Prison-based Care Site|SNOMED_CT
20078004|Substance Abuse Treatment Center|SNOMED_CT
257573002|Boarding House|SNOMED_CT
224683003|Military Accommodation|SNOMED_CT
284546000|Hospice|SNOMED_CT
257628001|Hostel|SNOMED_CT
310207003|Sheltered Housing|SNOMED_CT
57656006|Penal Institution|SNOMED_CT
285113009|Religious Institutional Residence|SNOMED_CT
285141008|Work (environment)|SNOMED_CT
32911000|Homeless|SNOMED_CT

**Documentation**:
Congregate Setting Residence Type?
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

**Name**: result_format

**ReportStream Internal Name**: result_format

**Type**: TEXT

**PII**: No

**Default Value**: CWE

**Cardinality**: [0..1]

---

**Name**: sender_id

**ReportStream Internal Name**: sender_id

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
ID name of org that is sending this data to ReportStream.  Suitable for provenance or chain of custody tracking.  Not to be confused with sending_application, in which ReportStream acts as the 'sender' to the downstream jurisdiction.
---

**Name**: sending_application

**ReportStream Internal Name**: sending_application

**Type**: HD

**PII**: No

**Default Value**: 

**Cardinality**: [0..1]

**Documentation**:
The name and OID for the application sending information to the receivers

---

**Name**: serial_screening

**ReportStream Internal Name**: serial_screening

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Serial Screening

---

**Name**: site_of_care

**ReportStream Internal Name**: site_of_care

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The type of facility providing care (Hospital, Nursing Home, etc.).  This is a CUSTOM internal field. DO NOT use this for the COVID AOE residence_type.
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

**Name**: specimen_collection_method

**ReportStream Internal Name**: specimen_collection_method

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
ANP|Plates, Anaerobic|HL7
BAP|Plates, Blood Agar|HL7
BCAE|Blood Culture, Aerobic Bottle|HL7
BCAN|Blood Culture, Anaerobic Bottle|HL7
BCPD|Blood Culture, Pediatric Bottle|HL7
BIO|Biopsy|HL7
CAP|Capillary Specimen|HL7
CATH|Catheterized|HL7
CVP|Line, CVP|HL7
EPLA|Environmental, Plate|HL7
ESWA|Environmental, Swab|HL7
FNA|Aspiration, Fine Needle|HL7
KOFFP|Plate, Cough|HL7
LNA|Line, Arterial|HL7
LNV|Line, Venous|HL7
MARTL|Martin-Lewis Agar|HL7
ML11|Mod. Martin-Lewis Agar|HL7
MLP|Plate, Martin-Lewis|HL7
NYP|Plate, New York City|HL7
PACE|Pace, Gen-Probe|HL7
PIN|Pinworm Prep|HL7
PNA|Aterial puncture|HL7
PRIME|Pump Prime|HL7
PUMP|Pump Specimen|HL7
QC5|Quality Control For Micro|HL7
SCLP|Scalp, Fetal Vein|HL7
SCRAPS|Scrapings|HL7
SHA|Shaving|HL7
SWA|Swab|HL7
SWD|Swab, Dacron tipped|HL7
TMAN|Transport Media, Anaerobic|HL7
TMCH|Transport Media, Chalamydia|HL7
TMM4|Transport Media, M4|HL7
TMMY|Transport Media, Mycoplasma|HL7
TMOT|Transport Media|HL7
TMP|Plate, Thayer-Martin|HL7
TMPV|Transport Media, PVA|HL7
TMSC|Transport Media, Stool Culture|HL7
TMUP|Transport Media, Ureaplasma|HL7
TMVI|Transport Media, Viral|HL7
VENIP|Venipuncture|HL7
WOOD|Swab, Wooden Shaft|HL7

---

**Name**: specimen_collection_site

**ReportStream Internal Name**: specimen_collection_site

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.10](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.10) 
                
---

**Name**: specimen_description

**ReportStream Internal Name**: specimen_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.8/Fields/SPM.14](https://hl7-definition.caristix.com/v2/HL7v2.8/Fields/SPM.14) 
                
---

**Name**: specimen_id

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

**Name**: specimen_role

**ReportStream Internal Name**: specimen_role

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
B|Blind sample|FHIR
E|Electronic QC|FHIR
F|Filer|FHIR
G|Group|FHIR
L|Pool|FHIR
O|Operator proficiency|FHIR
P|Patient|FHIR
Q|Control specimen|FHIR
R|Replicate|FHIR
V|Verifying collaborator|FHIR

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
123851003|Mouth region structure (body structure)|SNOMED_CT
31389004|Oropharyngeal structure (body structure)|SNOMED_CT
39607008|Lung structure (body structure)|SNOMED_CT
955009|Bronchial structure (body structure)|SNOMED_CT
1797002|Structure of anterior nares (body structure)|SNOMED_CT

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
258560004|Oral saliva sample|SNOMED_CT
258607008|Bronchoalveolar lavage fluid sample|SNOMED_CT
119364003|Serum specimen|SNOMED_CT
119361006|Plasma specimen|SNOMED_CT
440500007|Dried blood spot specimen|SNOMED_CT
258580003|Whole blood sample|SNOMED_CT
122555007|Venous blood specimen|SNOMED_CT
119297000|Blood specimen|SNOMED_CT
122554006|Capillary blood specimen|SNOMED_CT
258467004|Nasopharyngeal washings|SNOMED_CT
418932006|Oral swab specimen|SNOMED_CT
433801000124107|Nasopharyngeal and oropharyngeal swab|SNOMED_CT
309171007|Lower respiratory fluid sample|SNOMED_CT
433871000124101|Nasal washings|SNOMED_CT
441620008|Sputum specimen obtained by sputum induction|SNOMED_CT
441903006|Coughed sputum specimen|SNOMED_CT
119336008|Specimen from trachea obtained by aspiration|SNOMED_CT
258610001|Oral fluid specimen|SNOMED_CT
119335007|Specimen obtained by bronchial aspiration|SNOMED_CT
445447003|Exhaled air specimen|SNOMED_CT

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

**Name**: telehealth_proctor_supervised

**ReportStream Internal Name**: telehealth_proctor_supervised

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Telehealth Proctor Supervised

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

**Name**: test_correction_reason

**ReportStream Internal Name**: test_correction_reason

**Type**: TEXT

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: test_kit_name_id

**ReportStream Internal Name**: test_kit_name_id

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: Testkit Name ID

**Documentation**:
Follows guidence for OBX-17 as defined in the HL7 Confluence page
---

**Name**: test_kit_name_id_cwe_version

**ReportStream Internal Name**: test_kit_name_id_cwe_version

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: LOINC Version ID

**Documentation**:
Follows guidance for OBX-17-7 where the version of the CWE field is passed along
---

**Name**: test_kit_name_id_type

**ReportStream Internal Name**: test_kit_name_id_type

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: Testkit Name ID Type

---

**Name**: test_method

**ReportStream Internal Name**: test_method

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Method

---

**Name**: test_method_description

**ReportStream Internal Name**: test_method_description

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
A text field that allows the lab to provide more information aboout the test method
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

**Name**: test_performed_system_abbr

**ReportStream Internal Name**: test_performed_system_abbr

**Type**: TEXT

**PII**: No

**Default Value**: LN

**Cardinality**: [0..1]

---

**Name**: test_performed_system_version

**ReportStream Internal Name**: test_performed_system_version

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: LOINC Version ID

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

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
C|Record coming over is a correction and thus replaces a final result|HL7
D|Deletes the OBX record|HL7
F|Final results; Can only be changed with a corrected result|HL7
I|Specimen in lab; results pending|HL7
N|Not asked; used to affirmatively document that the observation identified in the OBX was not sought when the universal service ID in OBR-4 implies that it would be sought.|HL7
O|Order detail description only (no result)|HL7
P|Preliminary results|HL7
R|Results entered -- not verified|HL7
S|Partial results|HL7
U|Results status change to final without retransmitting results already sent as ‘preliminary.’  E.g., radiology changes status from preliminary to final|HL7
W|Post original as wrong, e.g., transmitted for wrong patient|HL7
X|Results cannot be obtained for this observation|HL7

**Documentation**:
The test result status, which is different from the test result itself. Per the valueset, this indicates if
the test result is in some intermediate status, is a correction, or is the final result.

---

**Name**: test_result_sub_id

**ReportStream Internal Name**: test_result_sub_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

---

**Name**: test_result_units

**ReportStream Internal Name**: test_result_units

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The units the test result is measured in.
---

**Name**: test_type

**ReportStream Internal Name**: test_type

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Type

---

**Name**: testing_lab_accession_number

**ReportStream Internal Name**: testing_lab_accession_number

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The accession number of the specimen collected
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

**Name**: testing_lab_county_code

**ReportStream Internal Name**: testing_lab_county_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

**Documentation**:
The county code for the testing lab from the FIPS dataset. This is the standard code used in ELR reporting.

---

**Name**: testing_lab_id

**ReportStream Internal Name**: testing_lab_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Typically this will be the same as the `testing_lab_clia`, but potentially could not be.
---

**Name**: testing_lab_id_assigner

**ReportStream Internal Name**: testing_lab_id_assigner

**Type**: HD

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
This is the assigner of the CLIA for the testing lab. If the testing lab has a CLIA, this field will be filled in.
---

**Name**: testing_lab_id_universal_id

**ReportStream Internal Name**: testing_lab_id_universal_id

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: testing_lab_id_universal_id_type

**ReportStream Internal Name**: testing_lab_id_universal_id_type

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

**Name**: testing_lab_name_type_code

**ReportStream Internal Name**: testing_lab_name_type_code

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: testing_lab_phone_number

**ReportStream Internal Name**: testing_lab_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The phone number of the testing lab
---

**Name**: testing_lab_specimen_id

**ReportStream Internal Name**: testing_lab_specimen_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The specimen-id from the testing lab
---

**Name**: testing_lab_specimen_received_datetime

**ReportStream Internal Name**: testing_lab_specimen_received_datetime

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The received date time for the specimen. This field is very important to many states for their HL7,
but for most of our senders, the received date time is the same as the collected date time. Unfortunately,
setting them to the same time breaks many validation rules. Most ELR systems apparently look for them to
be offset, so this field takes the `specimen_collection_date_time` field and offsets it by a small amount.

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

**Name**: testing_lab_street2

**ReportStream Internal Name**: testing_lab_street2

**Type**: STREET_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Street 2 field for the testing lab
---

**Name**: testing_lab_zip_code

**ReportStream Internal Name**: testing_lab_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The postal code for the testing lab
---

**Name**: value_type

**ReportStream Internal Name**: value_type

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
AD|Address|HL7
CE|Coded Entry|HL7
CF|Coded Element With Formatted Values|HL7
CK|Composite ID With Check Digit|HL7
CN|Composite ID And Name|HL7
CP|Composite Price|HL7
CWE|Coded With Exceptions|HL7
CX|Extended Composite ID With Check Digit|HL7
DT|Date|HL7
ED|Encapsulated Data|HL7
FT|Formatted Text (Display)|HL7
MO|Money|HL7
NM|Numeric|HL7
PN|Person Name|HL7
RP|Reference Pointer|HL7
SN|Structured Numeric|HL7
ST|String Data|HL7
TM|Time|HL7
TN|Telephone Number|HL7
TS|Time Stamp (Date & Time)|HL7
TX|Text Data (Display)|HL7
XAD|Extended Address|HL7
XCN|Extended Composite Name and Number For Persons|HL7
XON|Extended Composite Name and Number For Organizations|HL7
XPN|Extended Person Name|HL7
XTN|Extended Telecommunications Number|HL7

---
