
### Schema:         ipatientcare/ipatientcare-covid-19
#### Description:   iPatientCare CSV lab report schema

---

**Name**: Comments

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: LOINC

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: Lab name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: PhyAddress1

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: PhysCity

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: PhysPhone

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: PhysST

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: PhysZip

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: Reference Range

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: ResultUnits

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: SSN

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored.

---

**Name**: Specimen_Type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

This field is ignored because it does not contain a valid specimen type.  Set the specimen_type in the facility-specific schema.

---

**Name**: equipment_model_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-04-28

**Table Column**: Model

---

**Name**: CLIA No

**Type**: ID_CLIA

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Facility

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

**Name**: hospitalized

**Type**: CODE

**PII**: No

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

**Type**: CODE

**PII**: No

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

**Name**: DateColl

**Type**: DATETIME

**PII**: No

**Format**: M/d/yyyy

**Cardinality**: [0..1]

**Documentation**:

DateColl populates multiple fields.  This instance populates order_test_date.

---

**Name**: TestName

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-08-11

**Table Column**: Test Ordered LOINC Long Name

**Documentation**:

TestName populates multiple fields.  This instance populates ordered_test_name.

---

**Name**: Fac_City

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_City populates multiple fields.  This instance populates ordering_facility_city.

---

**Name**: Facility

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Facility populates multiple fields.  This instance populates ordering_facility_name.

---

**Name**: Fac_Phone

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_Phone populates multiple fields.  This instance populates ordering_facility_phone_number.

---

**Name**: Fac_State

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

Fac_State populates multiple fields.  This instance populates ordering_facility_state.

---

**Name**: Fac_Addr1

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_Addr1 populates multiple fields.  This instance populates ordering_facility_street.

---

**Name**: Fac_Zip

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_Zip populates multiple fields.  This instance populates ordering_facility_zip_code.

---

**Name**: Fac_City

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

Fac_City populates multiple fields.  This instance populates ordering_provider_city.

---

**Name**: Fac_Phone

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

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

Fac_State populates multiple fields.  This instance populates ordering_provider_state.

---

**Name**: Fac_Addr1

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

Fac_Addr1 populates multiple fields.  This instance populates ordering_provider_street.

---

**Name**: Fac_Zip

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_Zip populates multiple fields.  This instance populates ordering_provider_zip_code.

---

**Name**: Patient City

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: patient_county

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: zip-code-data

**Table Column**: county

---

**Name**: Birth Date

**Type**: DATE

**PII**: Yes

**Format**: M/d/yyyy H:nn

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: Ethnicity

**Type**: CODE

**PII**: No

**Format**: $alt

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
H|Hispanic or Latino
N|Non Hispanic or Latino
U|Unknown
H|Hispanic or Latino
N|Non Hispanic or Latino
N|Non Hispanic or Latino
U|Unknown
U|Unknown

**Alt Value Sets**

Code | Display
---- | -------
H|Hispanic or Latino
N|Non Hispanic or Latino
N|Not Hispanic or Latino
U|Patient Declines
U|Unknown

**Documentation**:

The patient's ethnicity. There is a valueset defined based on the values in PID-22, but downstream
consumers are free to define their own values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: First Name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: Sex

**Type**: CODE

**PII**: No

**Format**: $display

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

**Name**: MRN

**Type**: TEXT

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The ID for the patient within one of the reporting entities for this lab result. It could be the
the patient ID from the testing lab, the oder placer, the ordering provider, or even within the PRIME system itself.


---

**Name**: Facility

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

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: Middle Name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: Patient phone

**Type**: TELEPHONE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: RACE

**Type**: CODE

**PII**: No

**Format**: $alt

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
2106-3|White
2106-3|White
2106-3|White
2106-3|White
1002-5|American Indian or Alaska Native
2028-9|Asian
UNK|Unknown
2054-5|Black or African American
2054-5|Black or African American
2054-5|Black or African American
2054-5|Black or African American
2076-8|Native Hawaiian or Other Pacific Islander
2131-1|Other
2131-1|Other
2131-1|Other
UNK|Unknown
UNK|Unknown
UNK|Unknown
ASKU|Asked, but unknown

**Alt Value Sets**

Code | Display
---- | -------
2106-3|White
2106-3|W
2106-3|CAUCASIAN
2106-3|C
1002-5|American Indian or Alaska Native
2028-9|Asian
UNK|ASIAN INDIAN
2054-5|Black
2054-5|African American
2054-5|AFRICAN AMERICAN,BLACK
2054-5|B
2076-8|Native Hawaiian or Other Pacific Islander
2131-1|Other
2131-1|OTHER RACE
2131-1|OTHER RACE,WHITE
UNK|Unknown
UNK|null
UNK|NULL
ASKU|Asked, but unknown

**Documentation**:

The patient's race. There is a common valueset defined for race values, but some states may choose to define different code/value combinations.


---

**Name**: Patient State

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: Patient Address

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: Patient ZipCode

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: Pregnant

**Type**: CODE

**PII**: No

**Format**: $alt

**LOINC Code**: 82810-3

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
77386006|Pregnant
60001007|Not Pregnant
261665006|Unknown
77386006|Pregnant
77386006|Pregnant
77386006|Pregnant
77386006|Pregnant
60001007|Not Pregnant
60001007|Not Pregnant
60001007|Not Pregnant
60001007|Not Pregnant
60001007|Not Pregnant
60001007|Not Pregnant
60001007|Not Pregnant
261665006|Unknown
261665006|Unknown
261665006|Unknown

**Alt Value Sets**

Code | Display
---- | -------
77386006|Y
77386006|YES
77386006|Pregnant
77386006|Currently Pregnant
60001007|N
60001007|NO
60001007| No
60001007| No *** High ***
60001007| No *** Low ***
60001007|Not Pregnant
60001007|Not Currently Pregnant
261665006|U
261665006|UNK
261665006|Unknown

**Documentation**:

Is the patient pregnant?

---

**Name**: processing_mode_code

**Type**: CODE

**PII**: No

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

**Name**: result_format

**Type**: TEXT

**PII**: No

**Default Value**: CE

**Cardinality**: [0..1]

---

**Name**: sender_id

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

ID name of org that is sending this data to ReportStream.  Suitable for provenance or chain of custody tracking.  Not to be confused with sending_application, in which ReportStream acts as the 'sender' to the downstream jurisdiction.

---

**Name**: DateColl

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

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-08-11

**Table Column**: Test Performed LOINC Long Name

**Documentation**:

TestName populates multiple fields.  This instance populates test_performed_name.

---

**Name**: LabResult

**Type**: CODE

**PII**: No

**Format**: $alt

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
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)
373121007|Test not done
260385009|Negative
260385009|Negative
260385009|Negative
260385009|Negative
10828004|Positive
10828004|Positive
10828004|Positive
10828004|Positive
10828004|Positive
10828004|Positive

**Alt Value Sets**

Code | Display
---- | -------
260385009|Negative
260385009|Negative *** High ***
260385009|Negative *** Low ***
260385009|Neg
10828004|Positive
10828004|Positive 
10828004|Positive *** High ***
10828004|Positive  *** High ***
10828004|Positive  *** Low ***
10828004|Pos

**Documentation**:

For now, Positive and Negative are the only results

---

**Name**: ResultDate

**Type**: DATETIME

**PII**: No

**Format**: M/d/yyyy

**Cardinality**: [0..1]

**Documentation**:

ResultDate populates multiple fields.  This instance populates test_result_date.

---

**Name**: ResultDate

**Type**: DATETIME

**PII**: No

**Format**: M/d/yyyy

**Cardinality**: [0..1]

**Documentation**:

ResultDate populates multiple fields.  This instance populates test_result_report_date.

---

**Name**: test_result_status

**Type**: CODE

**PII**: No

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

**Name**: Fac_City

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_City populates multiple fields.  This instance populates testing_lab_city.

---

**Name**: CLIA No

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

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Typically this will be the same as the `testing_lab_clia`, but potentially could not be.

---

**Name**: Facility

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

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_Phone populates multiple fields.  This instance populates testing_lab_phone_number.

---

**Name**: DateColl

**Type**: DATETIME

**PII**: No

**Format**: M/d/yyyy

**Cardinality**: [0..1]

**Documentation**:

DateColl populates multiple fields.  This instance populates testing_lab_specimen_received_datetime.

---

**Name**: Fac_State

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

Fac_State populates multiple fields.  This instance populates testing_lab_state.

---

**Name**: Fac_Addr1

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_Addr1 populates multiple fields.  This instance populates testing_lab_street.

---

**Name**: Fac_Zip

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Fac_Zip populates multiple fields.  This instance populates testing_lab_zip_code.

---
