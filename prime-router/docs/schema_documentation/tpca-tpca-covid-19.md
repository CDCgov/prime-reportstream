
### Schema:         tpca/tpca-covid-19
#### Description:   A COVID-19 schema for TPCA working through A6

---

**Name**: Patient ID

**Type**: TEXT

**PII**: No

**HL7 Field**: PID-3-1

**Cardinality**: [0..1]

---

**Name**: Name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: DOB

**Type**: DATE

**PII**: Yes

**Format**: M/d/yyyy

**HL7 Field**: PID-7

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: Age

**PII**: No

**Cardinality**: [0..1]

---

**Name**: patient_age

**Type**: NUMBER

**PII**: No

**HL7 Field**: AOE

**LOINC Code**: 30525-0

**Cardinality**: [0..1]

---

**Name**: patient_age_units

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
min|minutes
h|hours
d|days
wk|weeks
mo|months
a|years

**Documentation**:

Always filled when `patient_age` is filled

---

**Name**: Race

**Type**: CODE

**PII**: No

**Format**: $alt

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
1002-5|American Indian or Alaska Native
2028-9|Asian
2054-5|Black or African American
2054-5|Black or African American
2076-8|Native Hawaiian or Other Pacific Islander
2106-3|White
2016-3|Caucasian
2131-1|Other
UNK|Unknown
N/A|N/A
ASKU|Asked, but unknown

**Alt Value Sets**

Code | Display
---- | -------
1002-5|American Indian or Alaska Native
2028-9|Asian
2054-5|Black or African American
2054-5|Black
2076-8|Native Hawaiian or Other Pacific Islander
2106-3|White
2016-3|Caucasian
2131-1|Other
UNK|Undefined
N/A|N/A
ASKU|Asked, but unknown

**Documentation**:

The patient's race. There is a common valueset defined for race values, but some states may choose to define different code/value combinations.


---

**Name**: Provider

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Address

**Type**: STREET

**PII**: Yes

**HL7 Field**: PID-11-1

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: City

**Type**: CITY

**PII**: Yes

**HL7 Field**: PID-11-3

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: State

**Type**: TABLE

**PII**: No

**HL7 Field**: PID-11-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: Zip

**Type**: TABLE

**PII**: No

**HL7 Field**: PID-11-5

**Cardinality**: [0..1]

**Table**: zip-code-data

**Table Column**: zipcode

**Documentation**:

The patient's zip code

---

**Name**: Phone

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: patient_phone_number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Field**: PID-13

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: Result Descr

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Appr Date

**Type**: DATE

**PII**: No

**Format**: M/d/yyyy

**Cardinality**: [0..1]

---

**Name**: LOINC

**Type**: TABLE

**PII**: No

**HL7 Field**: OBX-3-1

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Performed LOINC Code

**Documentation**:

The LOINC code of the test performed. This is a standardized coded value describing the test

---

**Name**: LOINC Test Descr

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: SNOMED Rslt Code

**Type**: CODE

**PII**: No

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

**Documentation**:

The result of the test performed. For IgG, IgM and CT results that give a numeric value put that here.

---

**Name**: Sample ID

**Type**: EI

**PII**: No

**HL7 Fields**: SPM-2

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2) 

**Documentation**:

A unique code for this specimen

---

**Name**: Draw Date

**Type**: DATETIME

**PII**: No

**Format**: M/d/yyyy

**HL7 Fields**: SPM-17-1, OBR-7, OBR-8, OBX-14

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: Sample Type

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: 1st Test?

**Type**: CODE

**PII**: No

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

**Name**: HCW?

**Type**: CODE

**PII**: No

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

**Name**: Symptomatic

**Type**: CODE

**PII**: No

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

**Name**: Sympt Date

**Type**: DATE

**PII**: No

**Format**: M/d/yyyy

**HL7 Field**: AOE

**LOINC Code**: 65222-2

**Cardinality**: [0..1]

---

**Name**: Hosp?

**Type**: CODE

**PII**: No

**HL7 Field**: AOE

**LOINC Code**: 77974-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Is the patient hospitalized?

---

**Name**: ICU?

**Type**: CODE

**PII**: No

**HL7 Field**: AOE

**LOINC Code**: 95420-6

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Documentation**:

Is the patient in the ICU?

---

**Name**: Nsg Home?

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Preg?

**Type**: CODE

**PII**: No

**Format**: $alt

**HL7 Field**: AOE

**LOINC Code**: 82810-3

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
77386006|Pregnant
60001007|Not Pregnant
261665006|Unknown
77386006|Pregnant
60001007|Not Pregnant
261665006|Unknown

**Alt Value Sets**

Code | Display
---- | -------
77386006|Y
60001007|N
261665006|Unknown

**Documentation**:

Is the patient pregnant?

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

**Name**: sending_application

**Type**: HD

**PII**: No

**HL7 Field**: MSH-3

**Cardinality**: [0..1]

---

**Name**: message_profile_id

**Type**: EI

**PII**: No

**HL7 Field**: MSH-21

**Cardinality**: [0..1]

**Documentation**:

The message profile identifer

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

**Name**: ordering_provider_first_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**: ORC-12-3, OBR-16-3

**Cardinality**: [0..1]

**Documentation**:

The first name of the provider who ordered the test

---

**Name**: ordering_provider_last_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**: ORC-12-2, OBR-16-2

**Cardinality**: [0..1]

**Documentation**:

The last name of provider who ordered the test

---

**Name**: testing_lab_name

**Type**: TEXT

**PII**: No

**HL7 Fields**: ORC-2-2, OBR-2-2, ORC-3-2, OBR-3-2, OBX-23-1

**Cardinality**: [0..1]

**Documentation**:

The name of the laboratory which performed the test, can be the same as the sending facility name

---

**Name**: testing_lab_city

**Type**: CITY

**PII**: No

**HL7 Field**: OBX-24-3

**Cardinality**: [0..1]

---

**Name**: testing_lab_street

**Type**: STREET

**PII**: No

**HL7 Field**: OBX-24-1

**Cardinality**: [0..1]

---

**Name**: testing_lab_state

**Type**: TABLE

**PII**: No

**HL7 Field**: OBX-24-4

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

---

**Name**: testing_lab_zip_code

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: OBX-24-5

**Cardinality**: [0..1]

---

**Name**: testing_lab_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: testing_lab_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

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

**Name**: patient_county

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: zip-code-data

**Table Column**: county

---

**Name**: ordering_facility_state

**Type**: TABLE

**PII**: No

**HL7 Field**: ORC-22-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---

**Name**: ordering_facility_street

**Type**: STREET

**PII**: No

**HL7 Field**: ORC-22-1

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: ordering_facility_city

**Type**: CITY

**PII**: No

**HL7 Field**: ORC-22-3

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: ordering_facility_zip_code

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: ORC-22-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: ordering_facility_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: ordering_facility_phone_number

**Type**: TELEPHONE

**PII**: No

**HL7 Field**: ORC-23

**Cardinality**: [0..1]

**Documentation**:

The phone number of the facility which the test was ordered from

---

**Name**: message_id

**Type**: ID

**PII**: No

**HL7 Field**: MSH-10

**Cardinality**: [1..1]

**Documentation**:

unique id to track the usage of the message

---
