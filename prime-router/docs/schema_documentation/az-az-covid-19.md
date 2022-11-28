
### Schema: az/az-covid-19
### Topic: covid-19
### Tracking Element: (testing_lab_specimen_id)
### Base On: [covid-19](./covid-19.md)
### Extends: none
#### Description: AZ COVID-19 flat file

---

**Name**: Notes

**ReportStream Internal Name**: comment

**Type**: TEXT

**PII**: No

**Default Value**: 

**Cardinality**: [0..1]

---

**Name**: Ordering_Facility_City

**ReportStream Internal Name**: ordering_facility_city

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: Ordering_Facility_State

**ReportStream Internal Name**: ordering_facility_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---

**Name**: Ordering_Facility_Address

**ReportStream Internal Name**: ordering_facility_street

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: Ordering_Facility_Zip

**ReportStream Internal Name**: ordering_facility_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: Provider_First_Name

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

**Name**: Provider_Last_Name

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

**Name**: Provider_Phone_Number

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

**Name**: Ordering_facility_name

**ReportStream Internal Name**: organization_and_facility

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Lab_Name

**ReportStream Internal Name**: organization_name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The SimpleReport concept of organization. It refers to organization for the ordering & performing facility

---

**Name**: Patient_City

**ReportStream Internal Name**: patient_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: Patient_County

**ReportStream Internal Name**: patient_county

**Type**: TABLE_OR_BLANK

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: Patient_Date_of_Birth

**ReportStream Internal Name**: patient_dob

**Type**: DATE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: Ethnicity

**ReportStream Internal Name**: patient_ethnicity

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
H|Hispanic or Latino|HL7
N|Non Hispanic or Latino|HL7
U|Unknown|HL7

**Documentation**:

The patient's ethnicity. There is a valueset defined based on the values in PID-22, but downstream
consumers are free to define their own values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: Patient_First_Name

**ReportStream Internal Name**: patient_first_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: Patient_Sex

**ReportStream Internal Name**: patient_gender

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
M|Male|HL7
F|Female|HL7
O|Other|HL7
A|Ambiguous|HL7
U|Unknown|HL7
N|Not applicable|HL7

**Documentation**:

The patient's gender. There is a valueset defined based on the values in PID-8-1, but downstream consumers are free to define their own accepted values. Please refer to the consumer-specific schema if you have questions.


---

**Name**: Patient_ID

**ReportStream Internal Name**: patient_id

**Type**: TEXT

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The ID for the patient within one of the reporting entities for this lab result. It could be the
the patient ID from the testing lab, the oder placer, the ordering provider, or even within the PRIME system itself.


---

**Name**: Patient_Last_Name

**ReportStream Internal Name**: patient_last_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: Patient_Phone_Number

**ReportStream Internal Name**: patient_phone_number

**Type**: TELEPHONE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: Race

**ReportStream Internal Name**: patient_race

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
1002-5|American Indian or Alaska Native|HL7
2028-9|Asian|HL7
2054-5|Black or African American|HL7
2076-8|Native Hawaiian or Other Pacific Islander|HL7
2106-3|White|HL7
2131-1|Other|HL7
UNK|Unknown|NULLFL
ASKU|Asked, but unknown|NULLFL
1002-5|American Indian or Alaska Native|HL7
2028-9|Asian|HL7
2054-5|Black or African American|HL7
2076-8|Native Hawaiian or Other Pacific Islander|HL7
2106-3|White|HL7
2131-1|Other|HL7
UNK|Unknown|NULLFL
ASKU|Asked, but unknown|NULLFL

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
1002-5|N|HL7
2028-9|A|HL7
2054-5|B|HL7
2076-8|P|HL7
2106-3|W|HL7
2131-1|O|HL7
UNK|O|HL7
ASKU|O|HL7

**Documentation**:

The patient's race. There is a common valueset defined for race values, but some states may choose to define different code/value combinations.


---

**Name**: Patient_State

**ReportStream Internal Name**: patient_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: Patient_Street_Address

**ReportStream Internal Name**: patient_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:

The patient's street address

---

**Name**: Patient_Zip

**ReportStream Internal Name**: patient_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Format**: $zip

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: Sending_Application

**ReportStream Internal Name**: sending_application

**Type**: HD

**PII**: No

**Format**: $name

**Default Value**: CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO

**Cardinality**: [0..1]

**Documentation**:

The name and OID for the application sending information to the receivers


---

**Name**: Collection_Date

**ReportStream Internal Name**: specimen_collection_date_time

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

**HL7 Fields**

- [OBR-7](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.7)
- [OBR-8](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.8)
- [OBX-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.14)
- [SPM-17-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.17.1)

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: Specimen_ID

**ReportStream Internal Name**: specimen_id

**Type**: EI

**PII**: No

**HL7 Fields**

- [SPM-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2)

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2) 

**Documentation**:

A unique code for this specimen

---

**Name**: Specimen_Site

**ReportStream Internal Name**: specimen_source_site_code

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
119297000|Blood specimen (specimen)|SNOMED_CT
71836000|Nasopharyngeal structure (body structure)|SNOMED_CT
45206002|Nasal structure (body structure)|SNOMED_CT
53342003|Internal nose structure (body structure)|SNOMED_CT
29092000|Venous structure (body structure)|SNOMED_CT
123851003|Mouth region structure (body structure)|SNOMED_CT
31389004|Oropharyngeal structure (body structure)|SNOMED_CT
39607008|Lung structure (body structure)|SNOMED_CT
955009|Bronchial structure (body structure)|SNOMED_CT
1797002|Structure of anterior nares (body structure)|SNOMED_CT

**Documentation**:

Refers back to the specimen source site, which is then encoded into the SPM-8 segment

---

**Name**: Specimen_Type

**ReportStream Internal Name**: specimen_type

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
445297001|Swab of internal nose|SNOMED_CT
258500001|Nasopharyngeal swab|SNOMED_CT
871810001|Mid-turbinate nasal swab|SNOMED_CT
697989009|Anterior nares swab|SNOMED_CT
258411007|Nasopharyngeal aspirate|SNOMED_CT
429931000124105|Nasal aspirate|SNOMED_CT
258529004|Throat swab|SNOMED_CT
119334006|Sputum specimen|SNOMED_CT
119342007|Saliva specimen|SNOMED_CT
258560004|Oral saliva sample|SNOMED_CT
258607008|Bronchoalveolar lavage fluid sample|SNOMED_CT
119364003|Serum specimen|SNOMED_CT
119361006|Plasma specimen|SNOMED_CT
440500007|Dried blood spot specimen|SNOMED_CT
258580003|Whole blood sample|SNOMED_CT
122555007|Venous blood specimen|SNOMED_CT
119297000|Blood specimen|SNOMED_CT
122554006|Capillary blood specimen|SNOMED_CT
258467004|Nasopharyngeal washings|SNOMED_CT
418932006|Oral swab specimen|SNOMED_CT
433801000124107|Nasopharyngeal and oropharyngeal swab|SNOMED_CT
309171007|Lower respiratory fluid sample|SNOMED_CT

**Documentation**:

The specimen source, such as Blood or Serum

---

**Name**: Test_Code

**ReportStream Internal Name**: test_performed_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Performed LOINC Code

**Documentation**:

The LOINC code of the test performed. This is a standardized coded value describing the test

---

**Name**: Test_Name

**ReportStream Internal Name**: test_performed_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Performed LOINC Long Name

**Documentation**:

The LOINC description of the test performed as related to the LOINC code.

---

**Name**: Result

**ReportStream Internal Name**: test_result

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
260373001|Detected|SNOMED_CT
260415000|Not detected|SNOMED_CT
720735008|Presumptive positive|SNOMED_CT
10828004|Positive|SNOMED_CT
42425007|Equivocal|SNOMED_CT
260385009|Negative|SNOMED_CT
895231008|Not detected in pooled specimen|SNOMED_CT
462371000124108|Detected in pooled specimen|SNOMED_CT
419984006|Inconclusive|SNOMED_CT
125154007|Specimen unsatisfactory for evaluation|SNOMED_CT
455371000124106|Invalid result|SNOMED_CT
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)|SNOMED_CT
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)|SNOMED_CT
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)|SNOMED_CT
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)|SNOMED_CT
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)|SNOMED_CT
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)|SNOMED_CT
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)|SNOMED_CT
373121007|Test not done|SNOMED_CT
82334004|Indeterminate|SNOMED_CT

**Documentation**:

The result of the test performed. For IgG, IgM and CT results that give a numeric value put that here.

---

**Name**: Result_Date

**ReportStream Internal Name**: test_result_date

**Type**: DATETIME

**PII**: No

**Format**: yyyyMMdd

**Cardinality**: [0..1]

---

**Name**: Lab_City

**ReportStream Internal Name**: testing_lab_city

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The city of the testing lab

---

**Name**: Lab_CLIA

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

**Name**: Lab_Phone

**ReportStream Internal Name**: testing_lab_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The phone number of the testing lab

---

**Name**: Lab_State

**ReportStream Internal Name**: testing_lab_state

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state for the testing lab

---

**Name**: Lab_Address

**ReportStream Internal Name**: testing_lab_street

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The street address for the testing lab

---

**Name**: Lab_Zip

**ReportStream Internal Name**: testing_lab_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

The postal code for the testing lab

---
