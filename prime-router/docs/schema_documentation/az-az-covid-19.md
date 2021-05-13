
### Schema:         az/az-covid-19
#### Description:   AZ COVID-19 flat file

---

**Name**: Sending_Application

**Type**: HD

**PII**: No

**Format**: $name

**HL7 Field**: MSH-3

**Cardinality**: [0..1]

---

**Name**: Lab_Name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Lab_CLIA

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**: OBX-15-1, OBX-23-10, ORC-3-3, OBR-3-3, OBR-2-3, ORC-2-3

**Cardinality**: [1..1]

**Documentation**:

CLIA Number from the laboratory that sends the message to DOH

An example of the ID is 03D2159846


---

**Name**: Lab_Address

**Type**: STREET

**PII**: No

**HL7 Field**: OBX-24-1

**Cardinality**: [0..1]

---

**Name**: Lab_City

**Type**: CITY

**PII**: No

**HL7 Field**: OBX-24-3

**Cardinality**: [0..1]

---

**Name**: Lab_State

**Type**: TABLE

**PII**: No

**HL7 Field**: OBX-24-4

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

---

**Name**: Lab_Zip

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: OBX-24-5

**Cardinality**: [0..1]

---

**Name**: Lab_Phone

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Patient_ID

**Type**: TEXT

**PII**: No

**HL7 Field**: PID-3-1

**Cardinality**: [0..1]

---

**Name**: Patient_First_Name

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-2

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: Patient_Last_Name

**Type**: PERSON_NAME

**PII**: Yes

**HL7 Field**: PID-5-1

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: Patient_Date_of_Birth

**Type**: DATE

**PII**: Yes

**HL7 Field**: PID-7

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: Patient_Sex

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
2076-8|Native Hawaiian or Other Pacific Islander
2106-3|White
2131-1|Other
UNK|Unknown
ASKU|Asked, but unknown

**Alt Value Sets**

Code | Display
---- | -------
1002-5|N
2028-9|A
2054-5|B
2076-8|P
2106-3|W
2131-1|O
UNK|O
ASKU|O

**Documentation**:

The patient's race. There is a common valueset defined for race values, but some states may choose to define different code/value combinations.


---

**Name**: Ethnicity

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

**Name**: Patient_Street_Address

**Type**: STREET

**PII**: Yes

**HL7 Field**: PID-11-1

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: Patient_City

**Type**: CITY

**PII**: Yes

**HL7 Field**: PID-11-3

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: Patient_State

**Type**: TABLE

**PII**: No

**HL7 Field**: PID-11-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: Patient_Zip

**Type**: POSTAL_CODE

**PII**: No

**Format**: $zip

**HL7 Field**: PID-11-5

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: Patient_County

**Type**: TABLE_OR_BLANK

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: Patient_Phone_Number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Field**: PID-13

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: Ordering_facility_name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Ordering_Facility_Address

**Type**: STREET

**PII**: No

**HL7 Field**: ORC-22-1

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: Ordering_Facility_City

**Type**: CITY

**PII**: No

**HL7 Field**: ORC-22-3

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: Ordering_Facility_State

**Type**: TABLE

**PII**: No

**HL7 Field**: ORC-22-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---

**Name**: Ordering_Facility_Zip

**Type**: POSTAL_CODE

**PII**: No

**HL7 Field**: ORC-22-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: Provider_First_Name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**: ORC-12-3, OBR-16-3

**Cardinality**: [0..1]

**Documentation**:

The first name of the provider who ordered the test

---

**Name**: Provider_Last_Name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**: ORC-12-2, OBR-16-2

**Cardinality**: [0..1]

**Documentation**:

The last name of provider who ordered the test

---

**Name**: Provider_Phone_Number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Fields**: ORC-14, OBR-17

**Cardinality**: [0..1]

**Documentation**:

The phone number of the provider

---

**Name**: Specimen_ID

**Type**: ID

**PII**: No

**HL7 Field**: SPM-2-1

**Cardinality**: [0..1]

**Documentation**:

The specimen-id from the testing lab

---

**Name**: Collection_Date

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

**HL7 Fields**: SPM-17-1, OBR-7, OBR-8, OBX-14

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: Specimen_Type

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

**Name**: Specimen_Site

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

**Name**: Test_Code

**Type**: TABLE

**PII**: No

**HL7 Field**: OBX-3-1

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Performed LOINC Code

**Documentation**:

The LOINC code of the test performed. This is a standardized coded value describing the test

---

**Name**: Test_Name

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

**Name**: Result_Date

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

**HL7 Field**: OBX-19

**Cardinality**: [0..1]

---

**Name**: Notes

**Type**: TEXT

**PII**: No

**HL7 Field**: NTE-3

**Cardinality**: [0..1]

---
