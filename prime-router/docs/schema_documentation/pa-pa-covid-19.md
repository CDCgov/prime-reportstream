
### Schema:         pa/pa-covid-19
#### Description:   PA COVID-19 flat file

---

**Name**: PatientFirstName

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-2

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: PatientMiddleInitial

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-3

**Cardinality**: [0..1]

---

**Name**: PatientLastName

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-1

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: PatientSuffix

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-4

**Cardinality**: [0..1]

---

**Name**: PatientDOB

**Type**: DATE

**PII**: Yes

**HL7 Field**: PID-7

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: PatientAddress1

**Type**: STREET

**PII**: Yes

**HL7 Field**: PID-11-1

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: PatientCity

**Type**: CITY

**PII**: Yes

**HL7 Field**: PID-11-3

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: PatientState

**Type**: TABLE

**PII**: No

**HL7 Field**: PID-11-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: PatientZipCode

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: PID-11-5

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: PatientPhoneNumber

**Type**: TELEPHONE

**PII**: Yes

**HL7 Field**: PID-13

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: PatientGender

**Type**: CODE

**PII**: No

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

**Name**: PatientRace

**Type**: CODE

**PII**: No

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

**Name**: PatientEthnicity

**Type**: CODE

**PII**: No

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

**Name**: TestID

**Type**: ID

**PII**: No

**HL7 Field**: SPM-2-1

**Cardinality**: [0..1]

**Documentation**:

The specimen-id from the testing lab

---

**Name**: SpecimenCollectedDate

**Type**: DATETIME

**PII**: No

**HL7 Fields**: SPM-17-1, OBR-7, OBR-8, OBX-14

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: SpecimenSource

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

**Name**: TestName

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

**Name**: TestQualitativeResult

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

**Name**: Notes

**Type**: TEXT

**PII**: No

**HL7 Field**: NTE-3

**Cardinality**: [0..1]

---

**Name**: PerformingFacilityName

**Type**: TEXT

**PII**: No

**HL7 Field**: ORC-21-1

**Cardinality**: [0..1]

**Documentation**:

The name of the facility which the test was ordered from

---
