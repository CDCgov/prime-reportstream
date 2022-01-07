
### Schema: upload-covid-19
### Topic: covid-19
### Tracking Element: (message_id)
### Base On: [covid-19](./covid-19.md)
### Extends: none
#### Description: Schema for CSV Upload Tool

---

**Name**: date_result_released

**ReportStream Internal Name**: date_result_released

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

**Cardinality**: [1..1]

---

**Name**: date_result_released

**ReportStream Internal Name**: date_result_released_temp

**PII**: No

**Format**: yyyyMMdd

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

Code | Display
---- | -------
Y|YES
Y|Y
N|NO
N|N
UNK|Unknown
UNK|U
UNK|UNK
UNK|N/A
UNK|NA
UNK|NR
UNK|NP
UNK|maybe

**Documentation**:

Translate multiple inbound Y/N/U AOE values to RS values

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

**Name**: accession_number

**ReportStream Internal Name**: filler_order_id_temp

**PII**: No

**Cardinality**: [0..1]

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

Code | Display
---- | -------
Y|YES
Y|Y
N|NO
N|N
UNK|Unknown
UNK|U
UNK|UNK
UNK|N/A
UNK|NA
UNK|NR
UNK|NP
UNK|maybe

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

Code | Display
---- | -------
Y|YES
Y|Y
N|NO
N|N
UNK|Unknown
UNK|U
UNK|UNK
UNK|N/A
UNK|NA
UNK|NR
UNK|NP
UNK|maybe

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

Code | Display
---- | -------
Y|YES
Y|Y
N|NO
N|N
UNK|Unknown
UNK|U
UNK|UNK
UNK|N/A
UNK|NA
UNK|NR
UNK|NP
UNK|maybe

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

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

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

**Name**: ordering_facility_city

**ReportStream Internal Name**: ordering_facility_city_temp

**PII**: No

**Cardinality**: [0..1]

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

**Name**: ordering_facility_phone_number

**ReportStream Internal Name**: ordering_facility_phone_number_temp

**PII**: No

**Cardinality**: [0..1]

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

**Name**: ordering_facility_state

**ReportStream Internal Name**: ordering_facility_state_temp

**PII**: No

**Cardinality**: [0..1]

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

**Name**: ordering_facility_street2

**ReportStream Internal Name**: ordering_facility_street2_temp

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordering_facility_street

**ReportStream Internal Name**: ordering_facility_street_temp

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

**Name**: ordering_facility_zip_code

**ReportStream Internal Name**: ordering_facility_zip_code_temp

**PII**: No

**Cardinality**: [0..1]

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

**Format**: yyyyMMdd

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

Code | Display
---- | -------
H|Hispanic or Latino
H|Hispanic
H|Latino
H|Mex. Amer./Hispanic
H|H
N|Non Hispanic or Latino
N|Non Hispanic
N|Not Hispanic or Latino
N|Not Hispanic
N|N
U|Unknown
U|U
U|UNK
U|Black
U|White
U|African American
U|NULL
U|Patient Declines

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

Code | Display
---- | -------
F|Female
F|F
M|Male
M|M
U|U
U|UNK
U|UNKNOWN
O|O
O|Other
O|OTH
A|A
A|Ambiguous

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

Code | Display
---- | -------
2106-3|White
2106-3|W
2106-3|Caucasian
2106-3|C
2106-3|2106-3
1002-5|American Indian or Alaska Native
1002-5|American Indian
1002-5|Native American
2054-5|Black or African American
2054-5|African American
2054-5|African American Alaska Native
2054-5|African American Black
2054-5|Black
2054-5|B
2054-5|2054-5
2076-8|Native Hawaiian or Other Pacific Islander
2076-8|Hawaiian
2076-8|NH
2076-8|2076-8
2131-1|Other
2131-1|OTH
2131-1|O
2131-1|Other Race
2131-1|Other Race White
2131-1|Other Race,White
2131-1|Other Race Black
2131-1|Other Race,Black
2131-1|2131-1
2028-9|Asian
2028-9|Asian Indian
2028-9|2028-9
UNK|Unknown
UNK|UNK
UNK|U
UNK|Patient Declines
UNK|null
ASKU|Asked, but unknown

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

Code | Display
---- | -------
77386006|Pregnant
77386006|Currently Pregnant
77386006|Y
77386006|YES
77386006|77386006
60001007|Not Pregnant
60001007|Not Currently Pregnant
60001007|N
60001007|NO
60001007|60001007
261665006|Unknown
261665006|U
261665006|UNK
261665006|N/A
261665006|NA
261665006|NR
261665006|NP
261665006|maybe
261665006|261665006

**Documentation**:

Translate multiple inbound values into the Pregnancy SNOMED Codes

---

**Name**: sender_id

**ReportStream Internal Name**: processing_mode_code

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: P

**Cardinality**: [1..1]

**Value Sets**

Code | Display
---- | -------
P|manualupload

**Documentation**:

Retrieve the processing_mode_code for a Sender from the sender-automation.valueset

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

Code | Display
---- | -------
Y|YES
Y|Y
N|NO
N|N
UNK|Unknown
UNK|U
UNK|UNK
UNK|N/A
UNK|NA
UNK|NR
UNK|NP
UNK|maybe

**Documentation**:

Translate multiple inbound Y/N/U AOE values to RS values

---

**Name**: sender_id

**ReportStream Internal Name**: sender_id

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

**Value Sets**

Code | Display
---- | -------
manualupload|manualupload

**Documentation**:

Validate the sender_id received against the valid list of Senders in the sender-automation.valueset

---

**Name**: specimen_collection_date

**ReportStream Internal Name**: specimen_collection_date_temp

**PII**: No

**Cardinality**: [0..1]

---

**Name**: specimen_collection_date

**ReportStream Internal Name**: specimen_collection_date_time

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

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

Code | Display
---- | -------
71836000|Nasopharyngeal structure (body structure)
71836000|Varied
71836000|Nasal
71836000|Nasopharyngeal swab
71836000|258500001
71836000|Nasopharyngeal aspirate
71836000|258411007
71836000|71836000
45206002|Nasal structure (body structure)
45206002|45206002
53342003|Internal nose structure (body structure)
53342003|Swab of internal nose
53342003|Anterior nares swab
53342003|Mid-turbinate nasal swab
53342003|Nasal Swab
53342003|445297001
53342003|697989009
53342003|53342003
29092000|Serum
29092000|Serum specimen
29092000|Plasma
29092000|Plasma specimen
29092000|Whole Blood
29092000|Whole Blood Sample
29092000|Blood specimen
29092000|Venous blood specimen
29092000|Capillary blood specimen
29092000|fingerstick whole blood
29092000|122554006
29092000|258580003
29092000|119361006
29092000|119364003
29092000|119297000
31389004|Oral
31389004|Throat Swab
31389004|Oropharyngeal
31389004|Oropharyngeal Swab
31389004|31389004

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

Code | Display
---- | -------
445297001|Swab of internal nose
445297001|Nasal Swab
445297001|445297001
258500001|Nasopharyngeal swab
258500001|Nasal
258500001|Varied
258500001|258500001
871810001|Mid-turbinate nasal swab
871810001|871810001
697989009|Anterior nares swab
697989009|697989009
258411007|Nasopharyngeal aspirate
258411007|258411007
429931000124105|Nasal aspirate
429931000124105|429931000124105
258529004|Throat swab
258529004|Throat
258529004|Oral
258529004|Oropharyngeal
258529004|Oropharyngeal Swab
258529004|258529004
119334006|Sputum specimen
119334006|119334006
119342007|Saliva specimen
119342007|119342007
258607008|Bronchoalveolar lavage fluid sample
258607008|258607008
119364003|Serum specimen
119364003|Serum
119364003|119364003
119361006|Plasma specimen
119361006|Plasma
119361006|119361006
440500007|Dried blood spot specimen
440500007|440500007
258580003|Whole blood sample
258580003|Whole blood
258580003|258580003
122555007|Venous blood specimen
122555007|122555007
119297000|Blood specimen
119297000|119297000
122554006|Capillary blood specimen
122554006|fingerstick whole blood
122554006|122554006

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

Code | Display
---- | -------
Y|YES
Y|Y
N|NO
N|N
UNK|Unknown
UNK|U
UNK|UNK
UNK|N/A
UNK|NA
UNK|NR
UNK|NP
UNK|maybe

**Documentation**:

Translate multiple inbound Y/N/U AOE values to RS values

---

**Name**: test_kit_name_id

**ReportStream Internal Name**: test_kit_name_id

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-09-29

**Table Column**: Testkit Name ID

**Documentation**:

Follows guidence for OBX-17 as defined in the HL7 Confluence page

---

**Name**: test_performed_code

**ReportStream Internal Name**: test_performed_code

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

**Table**: LIVD-SARS-CoV-2-2021-09-29

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

Code | Display
---- | -------
260385009|Negative
260385009|Neg
260385009|Negative *** High ***
260385009|Negative *** Low ***
260385009|260385009
260415000|Not detected
260415000|NDET
260415000|260415000
260373001|Detected
260373001|DET
260373001|260373001
10828004|Positive
10828004|Pos
10828004|Positive (Abnormal)
10828004|Positive (Alpha Abnormal)
10828004|Positive *** High ***
10828004|Positive  *** High ***
10828004|Positive  *** Low ***
10828004|Positive 
10828004|10828004
720735008|Presumptive positive
720735008|720735008
419984006|Inconclusive
419984006|Inconclusive Result
419984006|419984006
42425007|Equivocal
42425007|42425007
895231008|Not detected in pooled specimen
895231008|895231008
462371000124108|Detected in pooled specimen
462371000124108|462371000124108
455371000124106|Invalid result
455371000124106|Invalid
455371000124106|455371000124106
125154007|Specimen unsatisfactory for evaluation
125154007|125154007
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)
840539006|840539006
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)
840544004|840544004
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)
840546002|840546002
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)
840533007|840533007
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)
840536004|840536004
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)
840535000|840535000
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)
840534001|840534001
373121007|Test not done
373121007|373121007

**Documentation**:

Translate multiple inbound Test Result values to RS values

---

**Name**: test_result_date

**ReportStream Internal Name**: test_result_date

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

**Cardinality**: [1..1]

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

**PII**: No

**Format**: yyyyMMdd

**Cardinality**: [0..1]

---

**Name**: testing_lab_specimen_received_date

**ReportStream Internal Name**: testing_lab_specimen_received_datetime

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

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

**Cardinality**: [0..1]

**Documentation**:

The postal code for the testing lab

---

**Name**: accession_number_temp

**ReportStream Internal Name**: accession_number_temp

**PII**: No

**Cardinality**: [0..1]

---

**Name**: equipment_model_name

**ReportStream Internal Name**: equipment_model_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-09-29

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
