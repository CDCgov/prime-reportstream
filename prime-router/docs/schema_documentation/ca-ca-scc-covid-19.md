
### Schema:         ca/ca-scc-covid-19
#### Description:   CSV download schema for Santa Clara County Public Health Department

---

**Name**: Facility Name

**Type**: TEXT

**PII**: No

**HL7 Field**: ORC-21-1

**Cardinality**: [0..1]

**Documentation**:

The name of the facility which the test was ordered from

---

**Name**: Facility CLIA

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**: OBX-15-1, OBX-23-10, ORC-3-3, OBR-3-3, OBR-2-3, ORC-2-3

**Cardinality**: [1..1]

**Documentation**:

CLIA Number from the laboratory that sends the message to DOH

An example of the ID is 03D2159846


---

**Name**: Facility Street Address

**Type**: STREET

**PII**: No

**HL7 Field**: ORC-22-1

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: Facility City

**Type**: CITY

**PII**: No

**HL7 Field**: ORC-22-3

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: Facility State

**Type**: TABLE

**PII**: No

**HL7 Field**: ORC-22-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---

**Name**: Facility Zip

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: ORC-22-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: Facility Phone

**Type**: TELEPHONE

**PII**: No

**HL7 Field**: ORC-23

**Cardinality**: [0..1]

**Documentation**:

The phone number of the facility which the test was ordered from

---

**Name**: Patient Identifier

**Type**: TEXT

**PII**: No

**HL7 Field**: PID-3-1

**Cardinality**: [0..1]

---

**Name**: Patient First Name

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-2

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: Patient Middle Initial

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-3

**Cardinality**: [0..1]

---

**Name**: Patient Last Name

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-1

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: Patient Date of Birth

**Type**: DATE

**PII**: Yes

**HL7 Field**: PID-7

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: Patient Sex

**Type**: CODE

**PII**: No

**Format**: $code

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

**Name**: Race

**Type**: CODE

**PII**: No

**Format**: $code

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

**Name**: Ethnicity

**Type**: CODE

**PII**: No

**Format**: $code

**HL7 Field**: PID-22

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
H|Hispanic or Latino
N|Non Hispanic or Latino
U|Unknown

**Documentation**:

The patient's ethnicity. There is a valueset defined based on the values in PID-22, but downstream
consumers are free to define their own values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: Language

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Patient Street Address

**Type**: STREET

**PII**: Yes

**HL7 Field**: PID-11-1

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: Patient City

**Type**: CITY

**PII**: Yes

**HL7 Field**: PID-11-3

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: Patient State

**Type**: TABLE

**PII**: No

**HL7 Field**: PID-11-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: Patient Zip

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: PID-11-5

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: Patient County

**Type**: TABLE_OR_BLANK

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: Patient Phone Number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Field**: PID-13

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: OK to Contact Patient

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Provider First Name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**: ORC-12-3, OBR-16-3

**Cardinality**: [0..1]

**Documentation**:

The first name of the provider who ordered the test

---

**Name**: Provider Last Name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**: ORC-12-2, OBR-16-2

**Cardinality**: [0..1]

**Documentation**:

The last name of provider who ordered the test

---

**Name**: Provider Facility Name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Provider ID/ NPI

**Type**: ID_NPI

**PII**: No

**HL7 Fields**: ORC-12-1, OBR-16-1

**Cardinality**: [0..1]

**Documentation**:

The ordering provider’s National Provider Identifier

---

**Name**: Provider Street Address

**Type**: STREET

**PII**: Yes

**HL7 Field**: ORC-24-1

**Cardinality**: [0..1]

**Documentation**:

The street address of the provider

---

**Name**: Provider City

**Type**: CITY

**PII**: Yes

**HL7 Field**: ORC-24-3

**Cardinality**: [0..1]

**Documentation**:

The city of the provider

---

**Name**: Provider State

**Type**: TABLE

**PII**: No

**HL7 Field**: ORC-24-4

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the provider

---

**Name**: Provider ZIP

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: ORC-24-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the provider

---

**Name**: Provider Phone Number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Fields**: ORC-14, OBR-17

**Cardinality**: [0..1]

**Documentation**:

The phone number of the provider

---

**Name**: Specimen ID

**Type**: EI

**PII**: No

**HL7 Fields**: SPM-2

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2) 

**Documentation**:

A unique code for this specimen

---

**Name**: Specimen Collection Date

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

**HL7 Fields**: SPM-17-1, OBR-7, OBR-8, OBX-14

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: Date Test Ordered

**Type**: DATE

**PII**: No

**HL7 Field**: ORC-15

**Cardinality**: [0..1]

---

**Name**: Result Date

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

**HL7 Field**: OBX-19

**Cardinality**: [0..1]

---

**Name**: Date Reported

**Type**: DATETIME

**PII**: No

**HL7 Field**: OBR-22

**Cardinality**: [0..1]

---

**Name**: Specimen Type

**Type**: CODE

**PII**: No

**Format**: $display

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

**Name**: Specimen Site

**Type**: CODE

**PII**: No

**Format**: $display

**HL7 Field**: SPM-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
119297000|Blood specimen (specimen)
71836000|Nasopharyngeal structure (body structure)
45206002|Nasal structure (body structure)

---

**Name**: Test Name

**Type**: TABLE

**PII**: No

**HL7 Field**: OBX-3-2

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Performed LOINC Long Name

**Documentation**:

The LOINC description of the test performed as related to the LOINC code.

---

**Name**: Result

**Type**: CODE

**PII**: No

**Format**: $display

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

**Name**: Accession Number

**Type**: ID

**PII**: No

**HL7 Fields**: ORC-3-1, SPM-2-2, OBR-3-1

**Cardinality**: [0..1]

**Documentation**:

Accension number

---

**Name**: Test Code

**Type**: TABLE

**PII**: No

**HL7 Field**: OBX-3-1

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Performed LOINC Code

**Documentation**:

The LOINC code of the test performed. This is a standardized coded value describing the test

---

**Name**: Result Code

**Type**: CODE

**PII**: No

**Format**: $code

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

---

**Name**: Device Identifier

**Type**: TABLE

**PII**: No

**HL7 Fields**: OBX-17-1, OBX-17-9

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Documentation**:

Device_id is a generated value for the OBX-17 field. It is based on the device model and the LIVD table.

---

**Name**: Notes

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---
