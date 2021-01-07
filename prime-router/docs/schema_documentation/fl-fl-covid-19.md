
### Schema:         fl/fl-covid-19
#### Description:   Florida COVID-19 flat file

---

**Name**: Sending Facility CLIA

**Type**: ID_CLIA

**HL7 Field**: OBX-23-10

**Cardinality**: [1..1]

**Documentation**:

CLIA Number from the laboratory that sends the message to DOH

An example of the ID is 03D2159846


---

**Name**: Sending Facility Name

**Type**: TEXT

**HL7 Field**: OBX-23-1

**Cardinality**: [0..1]

**Documentation**:

The name of the laboratory which performed the test, can be the same as the sending facility name

---

**Name**: Medical Record Number

**Type**: ID

**HL7 Field**: SPM-2-1

**Cardinality**: [0..1]

**Documentation**:

Medical Record number for the patient

---

**Name**: Patient Last Name

**Type**: PERSON_NAME

**HL7 Field**: PID-5-1

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: Patient First Name

**Type**: PERSON_NAME

**HL7 Field**: PID-5-2

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: Patient Date of Birth

**Type**: DATE

**Format**: MM/dd/yyyy

**HL7 Field**: PID-7

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth in this format "MM/dd/yyyy"

---

**Name**: Patient Race

**Type**: CODE

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

**Name**: Patient Ethnicity

**Type**: CODE

**HL7 Field**: PID-22

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
H|Hispanic or Latino
N|Non Hispanic or Latino
U|Unknown

**Documentation**:

The patient's ethnicity. There is a valueset defined based on the values in PID-22, but downstream consumers are free to define their own values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: Patient Gender

**Type**: CODE

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

**Alt Value Sets**

Code | Display
---- | -------
M|Male
F|Female
U|Unknown

**Documentation**:

The patient's gender. Expects M, F, or U

---

**Name**: Patient Street Address

**Type**: STREET

**HL7 Field**: PID-11-1

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: Patient Street Address2

**Type**: STREET_OR_BLANK

**HL7 Field**: PID-11-2

**Cardinality**: [0..1]

**Documentation**:

The patient's second address line

---

**Name**: Patient City

**Type**: CITY

**HL7 Field**: PID-11-3

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: Patient State

**Type**: TABLE

**HL7 Field**: PID-11-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: Patient Zip

**Type**: POSTAL_CODE

**HL7 Field**: PID-11-5

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: Patient Phone Number

**Type**: TELEPHONE

**HL7 Field**: PID-13

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: Patient Social Security Number

**Type**: BLANK

**HL7 Field**: PID-19

**Cardinality**: [0..1]

**Documentation**:

The patient's SSN formatted without dashes

---

**Name**: Ordering Provider NPI Number

**Type**: ID_NPI

**HL7 Field**: ORC-12-1

**Cardinality**: [0..1]

**Documentation**:

The ordering provider’s National Provider Identifier

---

**Name**: Ordering Provider Last Name

**Type**: PERSON_NAME

**Cardinality**: [0..1]

**Documentation**:

The last name of provider who ordered the test

---

**Name**: Ordering Provider First Name

**Type**: PERSON_NAME

**HL7 Field**: ORC-12-3

**Cardinality**: [0..1]

**Documentation**:

The first name of the provider who ordered the test

---

**Name**: Ordering Provider Street Address

**Type**: STREET

**HL7 Field**: ORC-24-1

**Cardinality**: [0..1]

**Documentation**:

The street address of the provider

---

**Name**: Ordering Provider Street Address2

**Type**: STREET_OR_BLANK

**HL7 Field**: ORC-24-2

**Cardinality**: [0..1]

**Documentation**:

The street second address of the provider

---

**Name**: Ordering Provider City

**Type**: CITY

**HL7 Field**: ORC-24-3

**Cardinality**: [0..1]

**Documentation**:

The city of the provider

---

**Name**: Ordering Provider State

**Type**: TABLE

**HL7 Field**: ORC-24-4

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the provider

---

**Name**: Ordering Provider Zip

**Type**: POSTAL_CODE

**HL7 Field**: ORC-24-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the provider

---

**Name**: Ordering Provider Phone Number

**Type**: TELEPHONE

**HL7 Field**: ORC-14

**Cardinality**: [0..1]

**Documentation**:

The phone number of the provider

---

**Name**: Ordering Facility Name

**Type**: TEXT

**HL7 Field**: ORC-21-1

**Cardinality**: [0..1]

**Documentation**:

The name of the facility which the test was ordered from

---

**Name**: Ordering Facility Address1

**Type**: STREET

**HL7 Field**: ORC-22-1

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: Ordering Facility Address2

**Type**: STREET_OR_BLANK

**HL7 Field**: ORC-22-2

**Cardinality**: [0..1]

**Documentation**:

The secondary address of the facility which the test was ordered from

---

**Name**: Ordering Facility City

**Type**: CITY

**HL7 Field**: ORC-22-3

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: Ordering Facility State

**Type**: TABLE

**HL7 Field**: ORC-22-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---

**Name**: Ordering Facility Zip

**Type**: POSTAL_CODE

**HL7 Field**: ORC-22-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: Ordering Facility Phone Number

**Type**: TELEPHONE

**HL7 Field**: ORC-23

**Cardinality**: [1..1]

**Documentation**:

The phone number of the facility which the test was ordered from

---

**Name**: Accession Number

**Type**: ID

**HL7 Field**: OBR-3-1

**Cardinality**: [0..1]

**Documentation**:

The accession number of the specimen collected

---

**Name**: Specimen Collected Date

**Type**: DATETIME

**HL7 Field**: SPM-17-1

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: Specimen Source

**Type**: CODE

**HL7 Field**: SPM-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
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

**Name**: Specimen Received Date

**Type**: DATETIME

**HL7 Field**: SPM-18

**Cardinality**: [0..1]

---

**Name**: Finalized Date

**Type**: DATE

**Format**: MM/dd/yyyy

**Cardinality**: [0..1]

**Documentation**:

The date which the result was finalized

---

**Name**: Observation Code

**Type**: TABLE

**HL7 Field**: OBX-3-1

**Cardinality**: [0..1]

**Table**: LIVD-2020-11-18

**Table Column**: LOINC Code

**Documentation**:

The LOINC code of the test performed. This is a standardized coded value describing the test

---

**Name**: Observation Description

**Type**: TABLE

**HL7 Field**: OBX-3-2

**Cardinality**: [0..1]

**Table**: LIVD-2020-11-18

**Table Column**: LOINC Long Name

**Documentation**:

The LOINC description of the test performed as related to the LOINC code.

---

**Name**: Local Code

**Type**: TEXT

**Cardinality**: [0..1]

**Documentation**:

This is a localized coded value that the facility may use for this test (Optional- Local Code is equal to LOINC code, so if you are providing LOINC Code, you may leave this field blank and vice versa)

---

**Name**: Local Code Description

**Type**: TEXT

**Cardinality**: [0..1]

**Documentation**:

This is a localized description of the localized coded value that the facility may use for this test (Optional unless LOINC Code and Description are not provided)

---

**Name**: Test Result

**Type**: CODE

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

**Documentation**:

The result of the test performed. For IgG, IgM and CT results that give a numeric value put that here.

---

**Name**: Reference Range

**Type**: TEXT

**HL7 Field**: OBX-7

**Cardinality**: [0..1]

**Documentation**:

The reference range of the lab result, such as “Negative” or “Normal”. For IgG, IgM and CT results that provide a value you MUST fill out this filed.

---

**Name**: Abnormal Flag

**Type**: CODE

**HL7 Field**: OBX-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
A|Abnormal
N|Normal

**Documentation**:

This field contains a table lookup indicating the normalcy status of the result.  A = abnormal; N = normal

---

**Name**: SNOMED Code for Result

**Type**: TEXT

**Cardinality**: [0..1]

**Documentation**:

This is the coded value that describes the result. For IgG, IgM and CT results that provide a value leave this field blank.

---

**Name**: Performing Lab Name

**Type**: TEXT

**Cardinality**: [0..1]

---

**Name**: Performing Lab CLIA

**Type**: TEXT

**Cardinality**: [0..1]

---

**Name**: Age at time of collection

**Type**: TEXT

**Cardinality**: [0..1]

**Documentation**:

The patient's age as a numeric value and a unit value, for example, "3 months", "25 years", etc


---

**Name**: Kit^Device^IDType

**Type**: TEXT

**HL7 Field**: OBX-17-2

**Cardinality**: [0..1]

**Documentation**:

A concatenation of three values: Manufacturer Name, Device's unique ID, Device Type


---

**Name**: First Test for Condition

**Type**: CODE

**Format**: $code

**HL7 Field**: AOE

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Alt Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
U|Unknown

**Documentation**:

Expects Y, N, or U

---

**Name**: Employment in Health Care

**Type**: CODE

**Format**: $code

**HL7 Field**: AOE

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Alt Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
U|Unknown

**Documentation**:

Expects Y, N, or U

---

**Name**: Occupation

**Type**: TEXT

**Cardinality**: [0..1]


**Reference URL**:
[https://covid-19-ig.logicalhealth.org/ValueSet-healthcare-occupation-value-set.html](https://covid-19-ig.logicalhealth.org/ValueSet-healthcare-occupation-value-set.html) 

**Documentation**:

FL expects the SNOMED code that maps to one of the values outlined at [https://covid-19-ig.logicalhealth.org/ValueSet-healthcare-occupation-value-set.html](https://covid-19-ig.logicalhealth.org/ValueSet-healthcare-occupation-value-set.html)


---

**Name**: Symptomatic

**Type**: CODE

**Format**: $code

**HL7 Field**: AOE

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Alt Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
U|Unknown

**Documentation**:

Expects Y, N, or U

---

**Name**: Symptom

**Type**: TEXT

**Cardinality**: [0..1]

**Documentation**:

Expects a list of the symptoms the patient is experiencing as as a set of SNOMED codes

---

**Name**: Date of Symptom Onset

**Type**: DATE

**Format**: MM/dd/yyyy

**HL7 Field**: AOE

**Cardinality**: [0..1]

---

**Name**: Hospitalized for this condition

**Type**: CODE

**Format**: $code

**HL7 Field**: AOE

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Alt Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
U|Unknown

**Documentation**:

Expects Y, N, or U

---

**Name**: In ICU

**Type**: CODE

**Format**: $code

**HL7 Field**: AOE

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Alt Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
U|Unknown

**Documentation**:

Expects Y, N, or U

---

**Name**: Resides in Congregate Care setting

**Type**: CODE

**Format**: $code

**HL7 Field**: AOE

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
UNK|Unknown

**Alt Value Sets**

Code | Display
---- | -------
Y|Yes
N|No
U|Unknown

**Documentation**:

Expects Y, N, or U

---

**Name**: Specify Congregate Care Setting

**Type**: CODE

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/pages/viewpage.action?pageId=86967947](https://confluence.hl7.org/pages/viewpage.action?pageId=86967947) 

**Value Sets**

Code | Display
---- | -------
22232009|Hospital
32074000|Long Term Care Hospital
224929004|Secure Hospital
42665001|Nursing Home
30629002|Retirement Home
74056004|Orphanage
722173008|Prison-based care site
20078004|Substance Abuse Treatment Center
257573002|Boarding House
224683003|Military Accommodation
284546000|Hospice
257628001|Hostel
310207003|Sheltered Housing
57656006|Penal Institution
32911000|Homeless

**Documentation**:

The type of congregate care setting.
Based on the value set specified at [https://confluence.hl7.org/pages/viewpage.action?pageId=86967947](https://confluence.hl7.org/pages/viewpage.action?pageId=86967947) item 7a.


---

**Name**: Pregnancy Status

**Type**: CODE

**Format**: $code

**HL7 Field**: AOE

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
77386006|Pregnant
60001007|Not Pregnant
261665006|Unknown

**Alt Value Sets**

Code | Display
---- | -------
Y|Pregnant
N|Not Pregnant
U|Unknown

**Documentation**:

Is the patient pregnant?

---

**Name**: Is the patient a student, teacher, or other faculty member

**Type**: CODE

**Format**: $code

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
Student|Student
Teacher|Teacher
Other (Faculty Member)|Other (Faculty Member)
N|No
U|Unknown

**Documentation**:

AOE question for Florida. Expects one of the following values:

    - Student
    - Teacher
    - Other (Faculty Member)
    - N
    - U


---

**Name**: What is the name of the school

**Type**: TEXT

**Cardinality**: [0..1]

**Documentation**:

AOE question for Florida. Will likely be the same value as the ordering facility.

---
