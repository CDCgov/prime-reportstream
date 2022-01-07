
### Schema: ipatientcare/ipatientcare-covid-19
### Topic: covid-19
### Tracking Element: none
### Base On: [covid-19](./covid-19.md)
### Extends: none
#### Description: iPatientCare CSV lab report schema

---

**Name**: Comments

**ReportStream Internal Name**: Comments_Ignore

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: LOINC

**ReportStream Internal Name**: LOINC_Ignore

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: Lab name

**ReportStream Internal Name**: Lab name_Ignore

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: PhyAddress1

**ReportStream Internal Name**: PhyAddress1_Ignore

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: PhysCity

**ReportStream Internal Name**: PhysCity_Ignore

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: PhysPhone

**ReportStream Internal Name**: PhysPhone_Ignore

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: PhysST

**ReportStream Internal Name**: PhysST_Ignore

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: PhysZip

**ReportStream Internal Name**: PhysZip_Ignore

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: Reference Range

**ReportStream Internal Name**: Reference Range_Ignore

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: ResultUnits

**ReportStream Internal Name**: ResultUnits_Ignore

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: SSN

**ReportStream Internal Name**: SSN_Ignore

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: ResultDate

**ReportStream Internal Name**: date_result_released

**Type**: DATETIME

**PII**: No

**Format**: M/d/yyyy

**Cardinality**: [0..1]

**Documentation**:

ResultDate populates multiple fields.  This instance populates date_result_released.

---

**Name**: CLIA No

**ReportStream Internal Name**: filler_clia

**Type**: ID_CLIA

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Facility

**ReportStream Internal Name**: filler_name

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [OBR-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.2)
- [ORC-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.2)

**Cardinality**: [0..1]

**Documentation**:

Facility populates multiple fields.  This instance populates filler_name.

---

**Name**: Accession_no

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

**Name**: DateColl

**ReportStream Internal Name**: order_test_date

**Type**: DATETIME

**PII**: No

**Format**: M/d/yyyy

**Cardinality**: [0..1]

**Documentation**:

DateColl populates multiple fields.  This instance populates order_test_date.

---

**Name**: TestName

**ReportStream Internal Name**: ordered_test_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-latest

**Table Column**: Test Ordered LOINC Long Name

**Documentation**:

TestName populates multiple fields.  This instance populates ordered_test_name.

---

**Name**: Fac_City

**ReportStream Internal Name**: ordering_facility_city

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_City populates multiple fields.  This instance populates ordering_facility_city.

---

**Name**: Facility

**ReportStream Internal Name**: ordering_facility_name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Facility populates multiple fields.  This instance populates ordering_facility_name.

---

**Name**: Fac_Phone

**ReportStream Internal Name**: ordering_facility_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_Phone populates multiple fields.  This instance populates ordering_facility_phone_number.

---

**Name**: Fac_State

**ReportStream Internal Name**: ordering_facility_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

Fac_State populates multiple fields.  This instance populates ordering_facility_state.

---

**Name**: Fac_Addr1

**ReportStream Internal Name**: ordering_facility_street

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_Addr1 populates multiple fields.  This instance populates ordering_facility_street.

---

**Name**: Fac_Zip

**ReportStream Internal Name**: ordering_facility_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_Zip populates multiple fields.  This instance populates ordering_facility_zip_code.

---

**Name**: Fac_City

**ReportStream Internal Name**: ordering_provider_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

Fac_City populates multiple fields.  This instance populates ordering_provider_city.

---

**Name**: Fac_Phone

**ReportStream Internal Name**: ordering_provider_phone_number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Fields**

- [OBR-17](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.17)
- [ORC-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.14)

**Cardinality**: [0..1]

**Documentation**:

Fac_Phone populates multiple fields.  This instance populates ordering_provider_phone_number.

---

**Name**: Fac_State

**ReportStream Internal Name**: ordering_provider_state

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

Fac_State populates multiple fields.  This instance populates ordering_provider_state.

---

**Name**: Fac_Addr1

**ReportStream Internal Name**: ordering_provider_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

Fac_Addr1 populates multiple fields.  This instance populates ordering_provider_street.

---

**Name**: Fac_Zip

**ReportStream Internal Name**: ordering_provider_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_Zip populates multiple fields.  This instance populates ordering_provider_zip_code.

---

**Name**: Patient City

**ReportStream Internal Name**: patient_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: Birth Date

**ReportStream Internal Name**: patient_dob

**Type**: DATE

**PII**: Yes

**Format**: M/d/yyyy H:nn

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: Ethnicity

**ReportStream Internal Name**: patient_ethnicity

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: U

**Cardinality**: [0..1]

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

**Name**: First Name

**ReportStream Internal Name**: patient_first_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: Sex

**ReportStream Internal Name**: patient_gender

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: U

**Cardinality**: [0..1]

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

**Name**: MRN

**ReportStream Internal Name**: patient_id

**Type**: TEXT

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The ID for the patient within one of the reporting entities for this lab result. It could be the
the patient ID from the testing lab, the oder placer, the ordering provider, or even within the PRIME system itself.


---

**Name**: Facility

**ReportStream Internal Name**: patient_id_assigner

**Type**: HD

**PII**: No

**HL7 Fields**

- [PID-3-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4)
- [PID-3-6-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.6.2)

**Cardinality**: [0..1]

**Documentation**:

Facility populates multiple fields.  This instance populates patient_id_assigner.

---

**Name**: Last Name

**ReportStream Internal Name**: patient_last_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: Middle Name

**ReportStream Internal Name**: patient_middle_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: Patient phone

**ReportStream Internal Name**: patient_phone_number

**Type**: TELEPHONE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: RACE

**ReportStream Internal Name**: patient_race

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: UNK

**Cardinality**: [0..1]

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

**Name**: Patient State

**ReportStream Internal Name**: patient_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: Patient Address

**ReportStream Internal Name**: patient_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: Patient ZipCode

**ReportStream Internal Name**: patient_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: Accession_no

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

**Name**: Pregnant

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

**Name**: processing_mode_code

**ReportStream Internal Name**: processing_mode_code

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Default Value**: P

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

**Name**: CLIA No

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

**Name**: Facility

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

Facility populates multiple fields.  This instance populates reporting_facility_name.

---

**Name**: sender_id

**ReportStream Internal Name**: sender_id

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

ID name of org that is sending this data to ReportStream.  Suitable for provenance or chain of custody tracking.  Not to be confused with sending_application, in which ReportStream acts as the 'sender' to the downstream jurisdiction.

---

**Name**: DateColl

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

DateColl populates multiple fields.  This instance populates specimen_collection_date_time.

---

**Name**: TestName

**ReportStream Internal Name**: test_performed_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-latest

**Table Column**: Test Performed LOINC Long Name

**Documentation**:

TestName populates multiple fields.  This instance populates test_performed_name.

---

**Name**: LabResult

**ReportStream Internal Name**: test_result

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

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

**Name**: ResultDate

**ReportStream Internal Name**: test_result_date

**Type**: DATETIME

**PII**: No

**Format**: M/d/yyyy

**Cardinality**: [0..1]

**Documentation**:

ResultDate populates multiple fields.  This instance populates test_result_date.

---

**Name**: Fac_City

**ReportStream Internal Name**: testing_lab_city

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_City populates multiple fields.  This instance populates testing_lab_city.

---

**Name**: CLIA No

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

**Name**: CLIA No

**ReportStream Internal Name**: testing_lab_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Typically this will be the same as the `testing_lab_clia`, but potentially could not be.

---

**Name**: Facility

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

Facility populates multiple fields.  This instance populates testing_lab_name.

---

**Name**: Fac_Phone

**ReportStream Internal Name**: testing_lab_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_Phone populates multiple fields.  This instance populates testing_lab_phone_number.

---

**Name**: DateColl

**ReportStream Internal Name**: testing_lab_specimen_received_datetime

**Type**: DATETIME

**PII**: No

**Format**: M/d/yyyy

**Cardinality**: [0..1]

**Documentation**:

DateColl populates multiple fields.  This instance populates testing_lab_specimen_received_datetime.

---

**Name**: Fac_State

**ReportStream Internal Name**: testing_lab_state

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

Fac_State populates multiple fields.  This instance populates testing_lab_state.

---

**Name**: Fac_Addr1

**ReportStream Internal Name**: testing_lab_street

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_Addr1 populates multiple fields.  This instance populates testing_lab_street.

---

**Name**: Fac_Zip

**ReportStream Internal Name**: testing_lab_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_Zip populates multiple fields.  This instance populates testing_lab_zip_code.

---

**Name**: equipment_model_name

**ReportStream Internal Name**: equipment_model_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Model

---

**Name**: hospitalized

**ReportStream Internal Name**: hospitalized

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Default Value**: N

**LOINC Code**: 77974-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

iPatientCare is an ambulatory EMR, so this field is defaulted to 'N'.

---

**Name**: icu

**ReportStream Internal Name**: icu

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Default Value**: N

**LOINC Code**: 95420-6

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

iPatientCare is an ambulatory EMR, so this field is defaulted to 'N'.

---

**Name**: message_id

**ReportStream Internal Name**: message_id

**Type**: ID

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

unique id to track the usage of the message

---

**Name**: patient_county

**ReportStream Internal Name**: patient_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: zip-code-data

**Table Column**: county

---

**Name**: patient_id_type

**ReportStream Internal Name**: patient_id_type

**Type**: TEXT

**PII**: No

**Default Value**: PI

**Cardinality**: [0..1]

---

**Name**: result_format

**ReportStream Internal Name**: result_format

**Type**: TEXT

**PII**: No

**Default Value**: CE

**Cardinality**: [0..1]

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
