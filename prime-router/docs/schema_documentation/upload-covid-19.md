
### Schema: upload-covid-19
### Topic: covid-19
### Tracking Element: (message_id)
### Base On: [covid-19](./covid-19.md)
### Extends: none
#### Description: Schema for CSV Upload Tool

---

**Name**: comment

**ReportStream Internal Name**: comment

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: date_result_released

**ReportStream Internal Name**: date_result_released

**Type**: DATETIME

**PII**: No

**Cardinality**: [1..1]

---

**Name**: date_result_released

**ReportStream Internal Name**: date_result_released_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: employed_in_healthcare

**ReportStream Internal Name**: employed_in_healthcare

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 95418-0

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|YES|LOCAL
Y|Y|LOCAL
N|NO|LOCAL
N|N|LOCAL
UNK|Unknown|LOCAL
UNK|U|LOCAL
UNK|UNK|LOCAL
UNK|N/A|LOCAL
UNK|NA|LOCAL
UNK|NR|LOCAL
UNK|NP|LOCAL
UNK|maybe|LOCAL

**Documentation**:

Translate multiple inbound Y/N/U AOE values to RS values

---

**Name**: equipment_model_name

**ReportStream Internal Name**: equipment_model_name

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-latest

**Table Column**: Model

---

**Name**: testing_lab_clia

**ReportStream Internal Name**: filler_clia

**Type**: ID_CLIA

**PII**: No

**Cardinality**: [0..1]

---

**Name**: accession_number

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

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 95417-2

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|YES|LOCAL
Y|Y|LOCAL
N|NO|LOCAL
N|N|LOCAL
UNK|Unknown|LOCAL
UNK|U|LOCAL
UNK|UNK|LOCAL
UNK|N/A|LOCAL
UNK|NA|LOCAL
UNK|NR|LOCAL
UNK|NP|LOCAL
UNK|maybe|LOCAL

**Documentation**:

Translate multiple inbound Y/N/U AOE values to RS values

---

**Name**: hospitalized

**ReportStream Internal Name**: hospitalized

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 77974-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|YES|LOCAL
Y|Y|LOCAL
N|NO|LOCAL
N|N|LOCAL
UNK|Unknown|LOCAL
UNK|U|LOCAL
UNK|UNK|LOCAL
UNK|N/A|LOCAL
UNK|NA|LOCAL
UNK|NR|LOCAL
UNK|NP|LOCAL
UNK|maybe|LOCAL

**Documentation**:

Translate multiple inbound Y/N/U AOE values to RS values

---

**Name**: icu

**ReportStream Internal Name**: icu

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 95420-6

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|YES|LOCAL
Y|Y|LOCAL
N|NO|LOCAL
N|N|LOCAL
UNK|Unknown|LOCAL
UNK|U|LOCAL
UNK|UNK|LOCAL
UNK|N/A|LOCAL
UNK|NA|LOCAL
UNK|NR|LOCAL
UNK|NP|LOCAL
UNK|maybe|LOCAL

**Documentation**:

Translate multiple inbound Y/N/U AOE values to RS values

---

**Name**: illness_onset_date

**ReportStream Internal Name**: illness_onset_date

**Type**: DATE

**PII**: No

**Format**: yyyyMMdd

**Default Value**: 

**LOINC Code**: 65222-2

**Cardinality**: [0..1]

---

**Name**: order_test_date

**ReportStream Internal Name**: order_test_date

**Type**: DATE

**PII**: No

**Cardinality**: [1..1]

---

**Name**: ordering_facility_city

**ReportStream Internal Name**: ordering_facility_city

**Type**: CITY

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: ordering_facility_name

**ReportStream Internal Name**: ordering_facility_name

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

The name of the facility which the test was ordered from

---

**Name**: ordering_facility_name

**ReportStream Internal Name**: ordering_facility_name_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordering_facility_phone_number

**ReportStream Internal Name**: ordering_facility_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [1..1]

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

**Cardinality**: [1..1]

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

**Name**: ordering_facility_street

**ReportStream Internal Name**: ordering_facility_street_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordering_facility_zip_code

**ReportStream Internal Name**: ordering_facility_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: ordering_provider_city

**ReportStream Internal Name**: ordering_provider_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

The city of the provider

---

**Name**: ordering_provider_first_name

**ReportStream Internal Name**: ordering_provider_first_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.3)
- [ORC-12-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.3)

**Cardinality**: [1..1]

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

**Cardinality**: [1..1]

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

**Cardinality**: [1..1]

**Documentation**:

The last name of provider who ordered the test

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

**Name**: ordering_provider_phone_number

**ReportStream Internal Name**: ordering_provider_phone_number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Fields**

- [OBR-17](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.17)
- [ORC-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.14)

**Cardinality**: [1..1]

**Documentation**:

The phone number of the provider

---

**Name**: ordering_provider_state

**ReportStream Internal Name**: ordering_provider_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the provider

---

**Name**: ordering_provider_street

**ReportStream Internal Name**: ordering_provider_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [1..1]

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

**Cardinality**: [1..1]

**Documentation**:

The zip code of the provider

---

**Name**: patient_city

**ReportStream Internal Name**: patient_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

The patient's city

---

**Name**: patient_county

**ReportStream Internal Name**: patient_county

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: patient_dob

**ReportStream Internal Name**: patient_dob

**Type**: DATE

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: patient_ethnicity

**ReportStream Internal Name**: patient_ethnicity

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
H|Hispanic or Latino|LOCAL
H|Hispanic|LOCAL
H|Latino|LOCAL
H|Mex. Amer./Hispanic|LOCAL
H|H|LOCAL
N|Non Hispanic or Latino|LOCAL
N|Non Hispanic|LOCAL
N|Not Hispanic or Latino|LOCAL
N|Not Hispanic|LOCAL
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

Translate multiple inbound ethnicity values to RS / OMB values

---

**Name**: patient_first_name

**ReportStream Internal Name**: patient_first_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

The patient's first name

---

**Name**: patient_gender

**ReportStream Internal Name**: patient_gender

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
F|Female|LOCAL
F|F|LOCAL
M|Male|LOCAL
M|M|LOCAL
U|U|LOCAL
U|UNK|LOCAL
U|UNKNOWN|LOCAL
O|O|LOCAL
O|Other|LOCAL
O|OTH|LOCAL
A|A|LOCAL
A|Ambiguous|LOCAL

**Documentation**:

Translate multiple inbound Gender values to RS values

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

**Name**: ordering_facility_name

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

**Format**: use value found in the Display column

**Cardinality**: [1..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
2106-3|White|LOCAL
2106-3|W|LOCAL
2106-3|Caucasian|LOCAL
2106-3|C|LOCAL
2106-3|2106-3|LOCAL
1002-5|American Indian or Alaska Native|LOCAL
1002-5|American Indian|LOCAL
1002-5|Native American|LOCAL
1002-5|1002-5|LOCAL
2054-5|Black or African American|LOCAL
2054-5|African American|LOCAL
2054-5|African American Alaska Native|LOCAL
2054-5|African American Black|LOCAL
2054-5|Black|LOCAL
2054-5|B|LOCAL
2054-5|2054-5|LOCAL
2076-8|Native Hawaiian or Other Pacific Islander|LOCAL
2076-8|Hawaiian|LOCAL
2076-8|NH|LOCAL
2076-8|2076-8|LOCAL
2131-1|Other|LOCAL
2131-1|OTH|LOCAL
2131-1|O|LOCAL
2131-1|Other Race|LOCAL
2131-1|Other Race White|LOCAL
2131-1|Other Race,White|LOCAL
2131-1|Other Race Black|LOCAL
2131-1|Other Race,Black|LOCAL
2131-1|2131-1|LOCAL
2028-9|Asian|LOCAL
2028-9|Asian Indian|LOCAL
2028-9|2028-9|LOCAL
UNK|Unknown|LOCAL
UNK|UNK|LOCAL
UNK|U|LOCAL
UNK|Patient Declines|LOCAL
UNK|null|LOCAL
ASKU|Asked, but unknown|LOCAL

**Documentation**:

Translate multiple inbound Race values to RS / OMB values

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

**Cardinality**: [1..1]

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

**Name**: patient_zip_code

**ReportStream Internal Name**: patient_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

The patient's zip code

---

**Name**: pregnant

**ReportStream Internal Name**: pregnant

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 82810-3

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
77386006|Pregnant|SNOMED_CT
77386006|Currently Pregnant|SNOMED_CT
77386006|Y|SNOMED_CT
77386006|YES|SNOMED_CT
77386006|77386006|SNOMED_CT
60001007|Not Pregnant|SNOMED_CT
60001007|Not Currently Pregnant|SNOMED_CT
60001007|N|SNOMED_CT
60001007|NO|SNOMED_CT
60001007|60001007|SNOMED_CT
261665006|Unknown|SNOMED_CT
261665006|U|SNOMED_CT
261665006|UNK|SNOMED_CT
261665006|N/A|SNOMED_CT
261665006|NA|SNOMED_CT
261665006|NR|SNOMED_CT
261665006|NP|SNOMED_CT
261665006|maybe|SNOMED_CT
261665006|261665006|SNOMED_CT

**Documentation**:

Translate multiple inbound values into the Pregnancy SNOMED Codes

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

**Name**: testing_lab_clia

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

**Name**: testing_lab_name

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

**Name**: resident_congregate_setting

**ReportStream Internal Name**: resident_congregate_setting

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 95421-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|YES|LOCAL
Y|Y|LOCAL
N|NO|LOCAL
N|N|LOCAL
UNK|Unknown|LOCAL
UNK|U|LOCAL
UNK|UNK|LOCAL
UNK|N/A|LOCAL
UNK|NA|LOCAL
UNK|NR|LOCAL
UNK|NP|LOCAL
UNK|maybe|LOCAL

**Documentation**:

Translate multiple inbound Y/N/U AOE values to RS values

---

**Name**: specimen_collection_date

**ReportStream Internal Name**: specimen_collection_date_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: specimen_collection_date

**ReportStream Internal Name**: specimen_collection_date_time

**Type**: DATETIME

**PII**: No

**HL7 Fields**

- [OBR-7](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.7)
- [OBR-8](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.8)
- [OBX-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.14)
- [SPM-17-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.17.1)

**Cardinality**: [1..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: specimen_type

**ReportStream Internal Name**: specimen_source_site_code

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
71836000|Nasopharyngeal structure (body structure)|SNOMED_CT
71836000|Varied|SNOMED_CT
71836000|Nasopharyngeal swab|SNOMED_CT
71836000|258500001|SNOMED_CT
71836000|Nasopharyngeal aspirate|SNOMED_CT
71836000|258411007|SNOMED_CT
71836000|71836000|SNOMED_CT
45206002|Nasal structure (body structure)|SNOMED_CT
45206002|45206002|SNOMED_CT
53342003|Internal nose structure (body structure)|SNOMED_CT
53342003|Swab of internal nose|SNOMED_CT
53342003|Anterior nares swab|SNOMED_CT
53342003|Mid-turbinate nasal swab|SNOMED_CT
53342003|Nasal|SNOMED_CT
53342003|Nasal Swab|SNOMED_CT
53342003|445297001|SNOMED_CT
53342003|697989009|SNOMED_CT
53342003|53342003|SNOMED_CT
29092000|Serum|SNOMED_CT
29092000|Serum specimen|SNOMED_CT
29092000|Plasma|SNOMED_CT
29092000|Plasma specimen|SNOMED_CT
29092000|Whole Blood|SNOMED_CT
29092000|Whole Blood Sample|SNOMED_CT
29092000|Blood specimen|SNOMED_CT
29092000|Venous blood specimen|SNOMED_CT
29092000|Capillary blood specimen|SNOMED_CT
29092000|fingerstick whole blood|SNOMED_CT
29092000|122554006|SNOMED_CT
29092000|258580003|SNOMED_CT
29092000|119361006|SNOMED_CT
29092000|119364003|SNOMED_CT
29092000|119297000|SNOMED_CT
31389004|Oral|SNOMED_CT
31389004|Throat Swab|SNOMED_CT
31389004|Oropharyngeal|SNOMED_CT
31389004|Oropharyngeal Swab|SNOMED_CT
31389004|31389004|SNOMED_CT

**Documentation**:

Translate inbound text to outbound SNOMED Codes

---

**Name**: specimen_type

**ReportStream Internal Name**: specimen_type

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
445297001|Swab of internal nose|SNOMED_CT
445297001|Nasal Swab|SNOMED_CT
445297001|445297001|SNOMED_CT
258500001|Nasopharyngeal swab|SNOMED_CT
258500001|Nasal|SNOMED_CT
258500001|Varied|SNOMED_CT
258500001|258500001|SNOMED_CT
871810001|Mid-turbinate nasal swab|SNOMED_CT
871810001|871810001|SNOMED_CT
697989009|Anterior nares swab|SNOMED_CT
697989009|697989009|SNOMED_CT
258411007|Nasopharyngeal aspirate|SNOMED_CT
258411007|258411007|SNOMED_CT
429931000124105|Nasal aspirate|SNOMED_CT
429931000124105|429931000124105|SNOMED_CT
258529004|Throat swab|SNOMED_CT
258529004|Throat|SNOMED_CT
258529004|Oral|SNOMED_CT
258529004|Oropharyngeal|SNOMED_CT
258529004|Oropharyngeal Swab|SNOMED_CT
258529004|258529004|SNOMED_CT
119334006|Sputum specimen|SNOMED_CT
119334006|119334006|SNOMED_CT
119342007|Saliva specimen|SNOMED_CT
119342007|119342007|SNOMED_CT
258607008|Bronchoalveolar lavage fluid sample|SNOMED_CT
258607008|258607008|SNOMED_CT
119364003|Serum specimen|SNOMED_CT
119364003|Serum|SNOMED_CT
119364003|119364003|SNOMED_CT
119361006|Plasma specimen|SNOMED_CT
119361006|Plasma|SNOMED_CT
119361006|119361006|SNOMED_CT
440500007|Dried blood spot specimen|SNOMED_CT
440500007|440500007|SNOMED_CT
258580003|Whole blood sample|SNOMED_CT
258580003|Whole blood|SNOMED_CT
258580003|258580003|SNOMED_CT
122555007|Venous blood specimen|SNOMED_CT
122555007|122555007|SNOMED_CT
119297000|Blood specimen|SNOMED_CT
119297000|119297000|SNOMED_CT
122554006|Capillary blood specimen|SNOMED_CT
122554006|fingerstick whole blood|SNOMED_CT
122554006|122554006|SNOMED_CT

**Documentation**:

Translate inbound text to outbound SNOMED Codes

---

**Name**: symptomatic_for_disease

**ReportStream Internal Name**: symptomatic_for_disease

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 95419-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|YES|LOCAL
Y|Y|LOCAL
N|NO|LOCAL
N|N|LOCAL
UNK|Unknown|LOCAL
UNK|U|LOCAL
UNK|UNK|LOCAL
UNK|N/A|LOCAL
UNK|NA|LOCAL
UNK|NR|LOCAL
UNK|NP|LOCAL
UNK|maybe|LOCAL

**Documentation**:

Translate multiple inbound Y/N/U AOE values to RS values

---

**Name**: test_performed_code

**ReportStream Internal Name**: test_performed_code

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

**Table**: LIVD-SARS-CoV-2-latest

**Table Column**: Test Performed LOINC Code

**Documentation**:

The LOINC code of the test performed. This is a standardized coded value describing the test

---

**Name**: test_result

**ReportStream Internal Name**: test_result

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
260385009|Negative|SNOMED_CT
260385009|Neg|SNOMED_CT
260385009|Negative *** High ***|SNOMED_CT
260385009|Negative *** Low ***|SNOMED_CT
260385009|260385009|SNOMED_CT
260415000|Not detected|SNOMED_CT
260415000|NDET|SNOMED_CT
260415000|260415000|SNOMED_CT
260373001|Detected|SNOMED_CT
260373001|DET|SNOMED_CT
260373001|260373001|SNOMED_CT
10828004|Positive|SNOMED_CT
10828004|Pos|SNOMED_CT
10828004|Positive (Abnormal)|SNOMED_CT
10828004|Positive (Alpha Abnormal)|SNOMED_CT
10828004|Positive *** High ***|SNOMED_CT
10828004|Positive  *** High ***|SNOMED_CT
10828004|Positive  *** Low ***|SNOMED_CT
10828004|Positive |SNOMED_CT
10828004|10828004|SNOMED_CT
720735008|Presumptive positive|SNOMED_CT
720735008|720735008|SNOMED_CT
419984006|Inconclusive|SNOMED_CT
419984006|Inconclusive Result|SNOMED_CT
419984006|419984006|SNOMED_CT
42425007|Equivocal|SNOMED_CT
42425007|42425007|SNOMED_CT
895231008|Not detected in pooled specimen|SNOMED_CT
895231008|895231008|SNOMED_CT
462371000124108|Detected in pooled specimen|SNOMED_CT
462371000124108|462371000124108|SNOMED_CT
455371000124106|Invalid result|SNOMED_CT
455371000124106|Invalid|SNOMED_CT
455371000124106|455371000124106|SNOMED_CT
125154007|Specimen unsatisfactory for evaluation|SNOMED_CT
125154007|125154007|SNOMED_CT
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)|SNOMED_CT
840539006|840539006|SNOMED_CT
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)|SNOMED_CT
840544004|840544004|SNOMED_CT
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)|SNOMED_CT
840546002|840546002|SNOMED_CT
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)|SNOMED_CT
840533007|840533007|SNOMED_CT
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)|SNOMED_CT
840536004|840536004|SNOMED_CT
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)|SNOMED_CT
840535000|840535000|SNOMED_CT
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)|SNOMED_CT
840534001|840534001|SNOMED_CT
373121007|Test not done|SNOMED_CT
373121007|373121007|SNOMED_CT

**Documentation**:

Translate multiple inbound Test Result values to RS values

---

**Name**: test_result_date

**ReportStream Internal Name**: test_result_date

**Type**: DATE

**PII**: No

**Cardinality**: [1..1]

---

**Name**: testing_lab_city

**ReportStream Internal Name**: testing_lab_city

**Type**: CITY

**PII**: No

**Cardinality**: [1..1]

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

**Name**: testing_lab_clia

**ReportStream Internal Name**: testing_lab_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Typically this will be the same as the `testing_lab_clia`, but potentially could not be.

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

**Cardinality**: [1..1]

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

**Name**: testing_lab_specimen_received_date

**ReportStream Internal Name**: testing_lab_specimen_received_date_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: testing_lab_specimen_received_date

**ReportStream Internal Name**: testing_lab_specimen_received_datetime

**Type**: DATETIME

**PII**: No

**Cardinality**: [1..1]

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

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state for the testing lab

---

**Name**: testing_lab_street

**ReportStream Internal Name**: testing_lab_street

**Type**: STREET

**PII**: No

**Cardinality**: [1..1]

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

**Cardinality**: [1..1]

**Documentation**:

The postal code for the testing lab

---

**Name**: accession_number_temp

**ReportStream Internal Name**: accession_number_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: comment_source

**ReportStream Internal Name**: comment_source

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
L|Ancillary (filler) department is source of comment|HL7
O|Other system is source of comment|HL7
P|Orderer (placer) is source of comment|HL7

---

**Name**: message_id

**ReportStream Internal Name**: message_id

**Type**: ID

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

unique id to track the usage of the message

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

**Name**: patient_id_type

**ReportStream Internal Name**: patient_id_type

**Type**: TEXT

**PII**: No

**Default Value**: PI

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

**Name**: sender_id

**ReportStream Internal Name**: sender_id

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

ID name of org that is sending this data to ReportStream.  Suitable for provenance or chain of custody tracking.  Not to be confused with sending_application, in which ReportStream acts as the 'sender' to the downstream jurisdiction.

---

**Name**: test_result_status

**ReportStream Internal Name**: test_result_status

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Default Value**: F

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

**Documentation**:

The test result status, which is different from the test result itself. Per the valueset, this indicates if
the test result is in some intermediate status, is a correction, or is the final result.


---

**Name**: testing_lab_specimen_id

**ReportStream Internal Name**: testing_lab_specimen_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The specimen-id from the testing lab

---
