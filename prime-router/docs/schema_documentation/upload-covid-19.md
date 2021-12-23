
### Schema: upload-covid-19
### Topic: covid-19
### Tracking Element: testId (message_id)
### Base On: [covid-19](./covid-19.md)
### Extends: none
#### Description: Schema for CSV Upload Tool

---

**Name**: healthcareEmployee

**ReportStream Internal Name**: employed_in_healthcare

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 95418-0

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

AOE - Is the patient employed in a healthcare setting?

---

**Name**: deviceName

**ReportStream Internal Name**: equipment_model_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-latest

**Table Column**: Model

**Documentation**:

Must match a value from LIVD column B, "Model". eg,  "BD Veritor System for Rapid Detection of SARS-CoV-2 & Flu A+B"

---

**Name**: firstTest

**ReportStream Internal Name**: first_test

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 95417-2

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

AOE - Is this the patient's first test for COVID-19

---

**Name**: hospitalized

**ReportStream Internal Name**: hospitalized

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 77974-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

AOE - Was the patient hospitalized because of COVID-19?

---

**Name**: admittedToIcu

**ReportStream Internal Name**: icu

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 95420-6

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

AOE - Was the patient admitted to the intensive care unit for COVID-19?

---

**Name**: symptomOnsetDate

**ReportStream Internal Name**: illness_onset_date

**Type**: DATE

**PII**: No

**LOINC Code**: 65222-2

**Cardinality**: [0..1]

**Documentation**:

AOE - Date of symptom onset

---

**Name**: testId

**ReportStream Internal Name**: message_id

**Type**: ID

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

Required. A unique id for this submission of data.  Do not re-use when correcting previous results.  Rather, submit a new testId, and use the correctedTestId to refer to the testId of the old submission.

---

**Name**: testOrderedDate

**ReportStream Internal Name**: order_test_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

eg, 20210108

---

**Name**: testOrdered

**ReportStream Internal Name**: ordered_test_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-latest

**Table Column**: Test Ordered LOINC Code

**Documentation**:

Leave this blank and we'll fill it in automatically.

---

**Name**: orderingFacilityCity

**ReportStream Internal Name**: ordering_facility_city

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: orderingFacilityCounty

**ReportStream Internal Name**: ordering_facility_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: orderingFacilityEmail

**ReportStream Internal Name**: ordering_facility_email

**Type**: EMAIL

**PII**: No

**Cardinality**: [0..1]

---

**Name**: orderingFacilityName

**ReportStream Internal Name**: ordering_facility_name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The name of the facility which the test was ordered from

---

**Name**: orderingFacilityPhone

**ReportStream Internal Name**: ordering_facility_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The phone number of the facility which the test was ordered from

---

**Name**: orderingFacilityState

**ReportStream Internal Name**: ordering_facility_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

Note that many states expect this field to be available, or ReportStream is not able to route data to them.  Please provide if possible in order for us to route to as many states as possible.

---

**Name**: orderingFacilityStreet

**ReportStream Internal Name**: ordering_facility_street

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: orderingFacilityStreet2

**ReportStream Internal Name**: ordering_facility_street2

**Type**: STREET_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The secondary address of the facility which the test was ordered from

---

**Name**: orderingFacilityZip

**ReportStream Internal Name**: ordering_facility_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: orderingProviderCity

**ReportStream Internal Name**: ordering_provider_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The city of the provider

---

**Name**: orderingProviderFname

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

**Name**: orderingProviderNpi

**ReportStream Internal Name**: ordering_provider_id

**Type**: ID_NPI

**PII**: No

**HL7 Fields**

- [OBR-16-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.1)
- [ORC-12-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.1)

**Cardinality**: [0..1]

**Documentation**:

eg, "1265050918"

---

**Name**: orderingProviderLname

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

**Name**: orderingProviderPhone

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

**Name**: orderingProviderState

**ReportStream Internal Name**: ordering_provider_state

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the provider

---

**Name**: orderingProviderAddress

**ReportStream Internal Name**: ordering_provider_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The street address of the provider

---

**Name**: orderingProviderAddress2

**ReportStream Internal Name**: ordering_provider_street2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The street second address of the provider

---

**Name**: orderingProviderZip

**ReportStream Internal Name**: ordering_provider_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The zip code of the provider

---

**Name**: patientAge

**ReportStream Internal Name**: patient_age

**Type**: NUMBER

**PII**: No

**LOINC Code**: 30525-0

**Cardinality**: [0..1]

---

**Name**: patientAgeUnits

**ReportStream Internal Name**: patient_age_units

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Default Value**: yr

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
mo|months
yr|years

**Documentation**:

User does not need to include this column.  Default to 'yr' if not specified.

---

**Name**: patientCity

**ReportStream Internal Name**: patient_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: patientCounty

**ReportStream Internal Name**: patient_county

**Type**: TABLE_OR_BLANK

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: patientDob

**ReportStream Internal Name**: patient_dob

**Type**: DATE

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: patientEmail

**ReportStream Internal Name**: patient_email

**Type**: EMAIL

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patientEthnicity

**ReportStream Internal Name**: patient_ethnicity

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
H|Hispanic or Latino
N|Non Hispanic or Latino
U|Unknown
H|Hispanic or Latino
N|Non Hispanic or Latino
U|Unknown
U|Unknown

**Alt Value Sets**

Code | Display
---- | -------
H|2135-2
N|2186-5
U|UNK
U|ASKU

**Documentation**:

Use the required HHS values. (2135-2, 2186-5, UNK, ASKU)

---

**Name**: patientNameFirst

**ReportStream Internal Name**: patient_first_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

Required

---

**Name**: patientSex

**ReportStream Internal Name**: patient_gender

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

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

**Name**: patientUniqueId

**ReportStream Internal Name**: patient_id

**Type**: TEXT

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The ID for the patient within one of the reporting entities for this lab result. It could be the
the patient ID from the testing lab, the oder placer, the ordering provider, or even within the PRIME system itself.


---

**Name**: patientNameLast

**ReportStream Internal Name**: patient_last_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

Required

---

**Name**: patientNameMiddle

**ReportStream Internal Name**: patient_middle_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patientPhone

**ReportStream Internal Name**: patient_phone_number

**Type**: TELEPHONE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: patientRace

**ReportStream Internal Name**: patient_race

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

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

**Name**: patientState

**ReportStream Internal Name**: patient_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

Required. Extremely important field for routing data to states.

---

**Name**: patientHomeAddress

**ReportStream Internal Name**: patient_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: patientHomeAddress2

**ReportStream Internal Name**: patient_street2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's second address line

---

**Name**: patientZip

**ReportStream Internal Name**: patient_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: pregnant

**ReportStream Internal Name**: pregnant

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 82810-3

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
77386006|Pregnant
60001007|Not Pregnant
261665006|Unknown

**Documentation**:

AOE - Is the patient pregnant? Use 77386006 for Pregnant, 60001007 for Not Pregnant, and 261665006 for Unknown.

---

**Name**: correctedTestId

**ReportStream Internal Name**: previous_message_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

pointer/link to the unique id of a previously submitted result.  Usually blank. Or, if an item modifies/corrects a prior item, this field holds the message_id of the prior item.

---

**Name**: processingModeCode

**ReportStream Internal Name**: processing_mode_code

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Default Value**: P

**Cardinality**: [1..1]

**Value Sets**

Code | Display
---- | -------
D|Debugging
P|Production
T|Training

**Documentation**:

User does not need to include this column, and we'll default to 'P'

---

**Name**: congregateResident

**ReportStream Internal Name**: resident_congregate_setting

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 95421-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

AOE - Does the patient reside in a congregate care setting?

---

**Name**: senderId

**ReportStream Internal Name**: sender_id

**Type**: TEXT

**PII**: No

**Default Value**: ManualUpload

**Cardinality**: [1..1]

**Documentation**:

Required. User should place their sender organization name in this field.

---

**Name**: specimenCollectedDate

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

eg, 20210113

---

**Name**: specimenId

**ReportStream Internal Name**: specimen_id

**Type**: EI

**PII**: No

**HL7 Fields**

- [SPM-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2)

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2) 

**Documentation**:

User does not need to include this column.  We'll copy the value from the testId if none is provided here.

---

**Name**: specimenSource

**ReportStream Internal Name**: specimen_type

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [1..1]

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
119297000|Blood specimen
122554006|Capillary blood specimen

**Documentation**:

The specimen source, such as Blood or Serum

---

**Name**: symptomatic

**ReportStream Internal Name**: symptomatic_for_disease

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**LOINC Code**: 95419-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

AOE - Does the patient have symptoms related to COVID-19?

---

**Name**: deviceIdentifier

**ReportStream Internal Name**: test_kit_name_id

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-latest

**Table Column**: Testkit Name ID

**Documentation**:

Required; we'll fill in if blank.  If filled in, must match a value from LIVD column M, "Test Kit Name ID"

---

**Name**: testPerformed

**ReportStream Internal Name**: test_performed_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-latest

**Table Column**: Test Performed LOINC Code

**Documentation**:

User does not need to include this column.  It'll get filled in automatically.

---

**Name**: testResult

**ReportStream Internal Name**: test_result

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [1..1]

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
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)
373121007|Test not done
82334004|Indeterminate

**Documentation**:

Specify a code.  For example, 260373001

---

**Name**: testResultDate

**ReportStream Internal Name**: test_result_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

eg, 20210111

---

**Name**: testResultStatus

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

User does not need to include this column.  It'll get filled in automatically

---

**Name**: performingFacilityCity

**ReportStream Internal Name**: testing_lab_city

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The city of the testing lab

---

**Name**: performingFacilityClia

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

Expecting a CLIA number here.  eg, "10D2218834"

---

**Name**: performingFacilityCounty

**ReportStream Internal Name**: testing_lab_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

**Documentation**:

The text value for the testing lab county. This is used to do the lookup in the FIPS dataset.

---

**Name**: performingFacilityName

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

**Name**: performingFacilityPhone

**ReportStream Internal Name**: testing_lab_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The phone number of the testing lab

---

**Name**: specimenReceivedDate

**ReportStream Internal Name**: testing_lab_specimen_received_datetime

**Type**: DATETIME

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Optional. User does not need to include this column - We'll fill it in from the specimenCollectedDate

---

**Name**: performingFacilityState

**ReportStream Internal Name**: testing_lab_state

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state for the testing lab

---

**Name**: performingFacilityStreet

**ReportStream Internal Name**: testing_lab_street

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The street address for the testing lab

---

**Name**: performingFacilityStreet2

**ReportStream Internal Name**: testing_lab_street2

**Type**: STREET_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Street 2 field for the testing lab

---

**Name**: performingFacilityZip

**ReportStream Internal Name**: testing_lab_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The postal code for the testing lab

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

User does not need to include this column; we'll populate it.

---

**Name**: patient_id_type

**ReportStream Internal Name**: patient_id_type

**Type**: TEXT

**PII**: No

**Default Value**: PI

**Cardinality**: [0..1]

**Documentation**:

User does not need to include this column.

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

User does not need to include this column; we'll populate it.

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

**Name**: test_authorized_for_home

**ReportStream Internal Name**: test_authorized_for_home

**Type**: TABLE

**PII**: No

**Default Value**: N

**Cardinality**: [0..1]


**Reference URL**:
[https://www.fda.gov/news-events/fda-newsroom/press-announcements](https://www.fda.gov/news-events/fda-newsroom/press-announcements) 

**Table**: LIVD-Supplemental-2021-06-07

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

**Table**: LIVD-Supplemental-2021-06-07

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

**Table**: LIVD-Supplemental-2021-06-07

**Table Column**: is_unproctored

**Documentation**:

Is the test authorized for unproctored administration by the FDA (Y, N, UNK)

---
