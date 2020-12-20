
### Schema:         fl/fl-covid-19
#### Description:   Florida COVID-19 flat file

---

**Name**: Sending Facility CLIA

---

**Name**: Sending Facility Name

---

**Name**: Medical Record Number

**Documentation**:

Medical Record number for the patient

---

**Name**: Patient Last Name

---

**Name**: Patient First Name

---

**Name**: Patient Date of Birth

**Format**: MM/dd/yyyy

**Documentation**:

The patient's date of birth in this format "MM/dd/yyyy"

---

**Name**: Patient Race

---

**Name**: Patient Ethnicity

---

**Name**: Patient Gender

**Documentation**:

The patient's gender. Expects M, F, or U

---

**Name**: Patient Street Address

---

**Name**: Patient Street Address2

---

**Name**: Patient City

---

**Name**: Patient State

---

**Name**: Patient Zip

---

**Name**: Patient Phone Number

---

**Name**: Patient Social Security Number

**Type**: TEXT

**HL7 Field**: PID-19

**Documentation**:

The patient's SSN formatted without dashes

---

**Name**: Ordering Provider NPI Number

---

**Name**: Ordering Provider Last Name

---

**Name**: Ordering Provider First Name

---

**Name**: Ordering Provider Street Address

---

**Name**: Ordering Provider Street Address2

---

**Name**: Ordering Provider City

---

**Name**: Ordering Provider State

---

**Name**: Ordering Provider Zip

---

**Name**: Ordering Provider Phone Number

---

**Name**: Ordering Facility Name

---

**Name**: Ordering Facility Address1

---

**Name**: Ordering Facility Address2

---

**Name**: Ordering Facility City

---

**Name**: Ordering Facility State

---

**Name**: Ordering Facility Zip

---

**Name**: Ordering Facility Phone Number

---

**Name**: Accession Number

---

**Name**: Specimen Collected Date

---

**Name**: Specimen Source

---

**Name**: Specimen Received Date

---

**Name**: Finalized Date

**Type**: DATE

**Format**: MM/dd/yyyy

**Documentation**:

The date which the result was finalized

---

**Name**: Observation Code

---

**Name**: Observation Description

---

**Name**: local_code

**Type**: TEXT

**Documentation**:

This is a localized coded value that the facility may use for this test (Optional- Local Code is equal to LOINC code, so if you are providing LOINC Code, you may leave this field blank and vice versa)

---

**Name**: local_code_description

**Type**: TEXT

**Documentation**:

This is a localized description of the localized coded value that the facility may use for this test (Optional unless LOINC Code and Description are not provided)

---

**Name**: Test Result

---

**Name**: Reference Range

---

**Name**: Abnormal Flag

---

**Name**: SNOMED Code for Result

**Type**: TEXT

**Documentation**:

This is the coded value that describes the result. For IgG, IgM and CT results that provide a value leave this field blank.

---

**Name**: Performing Lab Name

---

**Name**: Performing Lab CLIA

---

**Name**: Age at time of collection

**Type**: TEXT

**Documentation**:

The patient's age as a numeric value and a unit value, for example, "3 months", "25 years", etc


---

**Name**: Kit^Device^IDType

**Type**: TEXT

**HL7 Field**: OBX-17-2

**Documentation**:

A concatenation of three values: Manufacturer Name, Device's unique ID, Device Type


---

**Name**: First Test for Condition

**Documentation**:

Expects Y, N, or U

---

**Name**: Employment in Health Care

**Documentation**:

Expects Y, N, or U

---

**Name**: Occupation

**Type**: TEXT


**Reference URL**:
[https://covid-19-ig.logicalhealth.org/ValueSet-healthcare-occupation-value-set.html](https://covid-19-ig.logicalhealth.org/ValueSet-healthcare-occupation-value-set.html) 

**Documentation**:

FL expects the SNOMED code that maps to one of the values outlined at [https://covid-19-ig.logicalhealth.org/ValueSet-healthcare-occupation-value-set.html](https://covid-19-ig.logicalhealth.org/ValueSet-healthcare-occupation-value-set.html)


---

**Name**: Symptomatic

**Documentation**:

Expects Y, N, or U

---

**Name**: Symptom

**Type**: TEXT

**Documentation**:

Expects a list of the symptoms the patient is experiencing as as a set of SNOMED codes

---

**Name**: Date of Symptom Onset

**Format**: MM/dd/yyyy

---

**Name**: Hospitalized for this condition

**Documentation**:

Expects Y, N, or U

---

**Name**: In ICU

**Documentation**:

Expects Y, N, or U

---

**Name**: Resides in Congregate Care setting

**Documentation**:

Expects Y, N, or U

---

**Name**: Specify Congregate Care Setting

**Type**: TEXT


**Reference URL**:
[https://confluence.hl7.org/pages/viewpage.action?pageId=86967947](https://confluence.hl7.org/pages/viewpage.action?pageId=86967947) 

**Documentation**:

The type of congregate care setting.
Based on the value set specified at [https://confluence.hl7.org/pages/viewpage.action?pageId=86967947](https://confluence.hl7.org/pages/viewpage.action?pageId=86967947) item 7a.


---

**Name**: Pregnancy Status

---

**Name**: Is the patient a student, teacher, or other faculty member

**Type**: TEXT

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

**Documentation**:

AOE question for Florida.

---
