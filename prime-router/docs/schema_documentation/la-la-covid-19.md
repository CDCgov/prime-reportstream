
### Schema:         la/la-covid-19
#### Description:   Louisiana COVID-19 CSV Flat-File

---

**Name**: LabName

**Type**: TEXT

**HL7 Field**: OBX-23-1

**Cardinality**: [0..1]

**Documentation**:

The name of the laboratory which performed the test, can be the same as the sending facility name

---

**Name**: MRN

**Type**: ID

**HL7 Field**: SPM-2-1

**Cardinality**: [0..1]

---

**Name**: Last Name

**Type**: PERSON_NAME

**HL7 Field**: PID-5-1

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: Middle Name

**Type**: PERSON_NAME

**HL7 Field**: PID-5-3

**Cardinality**: [0..1]

---

**Name**: First Name

**Type**: PERSON_NAME

**HL7 Field**: PID-5-2

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: Patient Address

**Cardinality**: [0..1]

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

**Name**: Patient Zipcode

**Type**: POSTAL_CODE

**HL7 Field**: PID-11-5

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: Patient Phone

**Type**: TELEPHONE

**HL7 Field**: PID-13

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: SSN

**Type**: TEXT

**Cardinality**: [0..1]

---

**Name**: Birth Date

**Type**: DATE

**HL7 Field**: PID-7

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: Sex

**Type**: CODE

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

**Name**: Ethnicity

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

**Name**: DateColl

**Type**: DATETIME

**HL7 Field**: SPM-17-1

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: ResultDate

**Type**: DATETIME

**HL7 Field**: OBX-22

**Cardinality**: [0..1]

---

**Name**: LOINC

**Type**: TABLE

**HL7 Field**: OBX-3-1

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Performed LOINC Code

**Documentation**:

The LOINC code of the test performed. This is a standardized coded value describing the test

---

**Name**: TestName

**Type**: TABLE

**HL7 Field**: OBX-3-2

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2-2021-01-20

**Table Column**: Test Performed LOINC Long Name

**Documentation**:

The LOINC description of the test performed as related to the LOINC code.

---

**Name**: LabResult

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

**Name**: ResultUnits

**Type**: TEXT

**HL7 Field**: OBX-6

**Cardinality**: [0..1]

---

**Name**: Reference Range

**Type**: TEXT

**HL7 Field**: OBX-7

**Cardinality**: [0..1]

**Documentation**:

The reference range of the lab result, such as “Negative” or “Normal”. For IgG, IgM and CT results that provide a value you MUST fill out this filed.

---

**Name**: Phyname

**Cardinality**: [0..1]

---

**Name**: PhyAddress1

**Type**: STREET

**HL7 Field**: ORC-24-1

**Cardinality**: [0..1]

**Documentation**:

The street address of the provider

---

**Name**: Physcity

**Type**: CITY

**HL7 Field**: ORC-24-3

**Cardinality**: [0..1]

**Documentation**:

The city of the provider

---

**Name**: Physst

**Type**: TABLE

**HL7 Field**: ORC-24-4

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the provider

---

**Name**: Physzip

**Type**: POSTAL_CODE

**HL7 Field**: ORC-24-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the provider

---

**Name**: PhysPhone

**Type**: TELEPHONE

**HL7 Field**: ORC-14

**Cardinality**: [0..1]

**Documentation**:

The phone number of the provider

---

**Name**: Facility

**Type**: TEXT

**HL7 Field**: ORC-21-1

**Cardinality**: [0..1]

**Documentation**:

The name of the facility which the test was ordered from

---

**Name**: FAC_ADDR1

**Type**: STREET

**HL7 Field**: ORC-22-1

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: FAC_CITY

**Type**: CITY

**HL7 Field**: ORC-22-3

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: FAC_STATE

**Type**: TABLE

**HL7 Field**: ORC-22-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---

**Name**: FAC_ZIP

**Type**: POSTAL_CODE

**HL7 Field**: ORC-22-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: FAC_PHONE

**Type**: TELEPHONE

**HL7 Field**: ORC-23

**Cardinality**: [1..1]

**Documentation**:

The phone number of the facility which the test was ordered from

---

**Name**: Comment

**Type**: TEXT

**HL7 Field**: NTE-3

**Cardinality**: [0..1]

---

**Name**: CLIA NO

**Type**: ID_CLIA

**HL7 Field**: OBX-23-10

**Cardinality**: [1..1]

**Documentation**:

CLIA Number from the laboratory that sends the message to DOH

An example of the ID is 03D2159846


---

**Name**: ACCESSION_NO

**Type**: ID

**HL7 Field**: OBR-3-1

**Cardinality**: [0..1]

**Documentation**:

The accession number of the specimen collected

---

**Name**: Specimen_Type

**Type**: CODE

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

**Name**: Pregnant

**Type**: CODE

**HL7 Field**: AOE

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
77386006|Pregnant
60001007|Not Pregnant
261665006|Unknown

**Documentation**:

Is the patient pregnant?

---

**Name**: patient_street

**Type**: STREET

**HL7 Field**: PID-11-1

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: patient_street2

**Type**: STREET_OR_BLANK

**HL7 Field**: PID-11-2

**Cardinality**: [0..1]

**Documentation**:

The patient's second address line

---

**Name**: ordering_provider_last_name

**Type**: PERSON_NAME

**Cardinality**: [0..1]

**Documentation**:

The last name of provider who ordered the test

---

**Name**: ordering_provider_first_name

**Type**: PERSON_NAME

**HL7 Field**: ORC-12-3

**Cardinality**: [0..1]

**Documentation**:

The first name of the provider who ordered the test

---
