
### Schema: ipatientcare/reddyfmc-la-covid-19
### Topic: covid-19
### Tracking Element: Accession_no (message_id)
### Base On: none
### Extends: [ipatientCare/ipatientcare-covid-19](./ipatientCare-ipatientcare-covid-19.md)
#### Description: iPatientCare CSV lab report schema, Reddy Family Medical Clinic, LA

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

**Cardinality**: [1..1]

**Documentation**:

ResultDate populates multiple fields.  This instance populates date_result_released.

---

**Name**: TestName

**ReportStream Internal Name**: equipment_model_name

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
BinaxNOW COVID-19 Ag Card|SARS-CoV-2 (COVID-19) Ag|NULLFL
BinaxNOW COVID-19 Ag Card|     SARS-CoV-2 (COVID-19) Ag|NULLFL

---

**Name**: Accession_no

**ReportStream Internal Name**: message_id

**Type**: ID

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

unique id to track the usage of the message

---

**Name**: DateColl

**ReportStream Internal Name**: order_test_date

**Type**: DATETIME

**PII**: No

**Format**: M/d/yyyy

**Cardinality**: [1..1]

**Documentation**:

DateColl populates multiple fields.  This instance populates order_test_date.

---

**Name**: TestName

**ReportStream Internal Name**: ordered_test_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Ordered LOINC Long Name

**Documentation**:

TestName populates multiple fields.  This instance populates ordered_test_name.

---

**Name**: Fac_City

**ReportStream Internal Name**: ordering_facility_city

**Type**: CITY

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

Fac_City populates multiple fields.  This instance populates ordering_facility_city.

---

**Name**: Facility

**ReportStream Internal Name**: ordering_facility_name

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

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

**Cardinality**: [1..1]

**Documentation**:

Fac_Addr1 populates multiple fields.  This instance populates ordering_facility_street.

---

**Name**: Fac_Zip

**ReportStream Internal Name**: ordering_facility_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

Fac_Zip populates multiple fields.  This instance populates ordering_facility_zip_code.

---

**Name**: Fac_City

**ReportStream Internal Name**: ordering_provider_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

Fac_City populates multiple fields.  This instance populates ordering_provider_city.

---

**Name**: PhyName

**ReportStream Internal Name**: ordering_provider_first_name

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
Holly|Holly Delatte NP|NULLFL
De Anna|De Anna Dark FNP-C|NULLFL
DeAnna|DeAnna Dark FNP-C|NULLFL
Nagaratna|Nagaratna Reddy MD.|NULLFL
Cassandra|Cassandra Hill-Selders NP|NULLFL
Crystal|Crystal Rivet NP|NULLFL
Darrell|Darrell Davis NP|NULLFL
Wanda|Wanda Jefferson Wilson FNP-C|NULLFL
Wanda|Wanda Jefferson FNP-C|NULLFL
Raveen|Raveen Coon FNP-C|NULLFL

---

**Name**: PhyName

**ReportStream Internal Name**: ordering_provider_id

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
1275978678|Holly Delatte NP|NULLFL
1205306602|De Anna Dark FNP-C|NULLFL
1205306602|DeAnna Dark FNP-C|NULLFL
1770580508|Nagaratna Reddy MD.|NULLFL
1912515651|Cassandra Hill-Selders NP|NULLFL
1699120493|Crystal Rivet NP|NULLFL
1821458373|Darrell Davis NP|NULLFL
1518378009|Wanda Jefferson Wilson FNP-C|NULLFL
1518378009|Wanda Jefferson FNP-C|NULLFL
1215690334|Raveen Coon FNP-C|NULLFL

---

**Name**: PhyName

**ReportStream Internal Name**: ordering_provider_last_name

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
Delatte NP|Holly Delatte NP|NULLFL
Dark FNP-C|De Anna Dark FNP-C|NULLFL
Dark FNP-C|DeAnna Dark FNP-C|NULLFL
Reddy MD|Nagaratna Reddy MD.|NULLFL
Hill-Selders NP|Cassandra Hill-Selders NP|NULLFL
Rivet NP|Crystal Rivet NP|NULLFL
Davis NP|Darrell Davis NP|NULLFL
Jefferson Wilson FNP-C|Wanda Jefferson Wilson FNP-C|NULLFL
Jefferson FNP-C|Wanda Jefferson FNP-C|NULLFL
Coon FNP-C|Raveen Coon FNP-C|NULLFL

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

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

Fac_State populates multiple fields.  This instance populates ordering_provider_state.

---

**Name**: Fac_Addr1

**ReportStream Internal Name**: ordering_provider_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

Fac_Addr1 populates multiple fields.  This instance populates ordering_provider_street.

---

**Name**: Fac_Zip

**ReportStream Internal Name**: ordering_provider_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

Fac_Zip populates multiple fields.  This instance populates ordering_provider_zip_code.

---

**Name**: Patient City

**ReportStream Internal Name**: patient_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:

The patient's city

---

**Name**: Birth Date

**ReportStream Internal Name**: patient_dob

**Type**: DATE

**PII**: Yes

**Format**: M/d/yyyy H:nn

**Cardinality**: [1..1]

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

Code | Display | System
---- | ------- | ------
H|Hispanic or Latino|LOCAL
H|Hispanic|LOCAL
H|Latino|LOCAL
H|Mex. Amer./Hispanic|LOCAL
H|2135-2|LOCAL
H|H|LOCAL
N|Non Hispanic or Latino|LOCAL
N|Non-Hispanic or Latino|LOCAL
N|Non Hispanic|LOCAL
N|Non-Hispanic|LOCAL
N|Not Hispanic or Latino|LOCAL
N|Not Hispanic|LOCAL
N|2186-5|LOCAL
N|N|LOCAL
U|Unknown|LOCAL
U|U|LOCAL
U|UNK|LOCAL
U|Black|LOCAL
U|White|LOCAL
U|African American|LOCAL
U|NULL|LOCAL
U|Patient Declines|LOCAL

**Documentation**:

Translate multiple inbound ethnicity values to RS / OMB values

---

**Name**: First Name

**ReportStream Internal Name**: patient_first_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

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

Code | Display | System
---- | ------- | ------
F|Female|LOCAL
F|Woman|LOCAL
F|F|LOCAL
M|Male|LOCAL
M|Man|LOCAL
M|M|LOCAL
U|U|LOCAL
U|UNK|LOCAL
U|UNKNOWN|LOCAL
O|O|LOCAL
O|Other|LOCAL
O|OTH|LOCAL
A|A|LOCAL
A|Ambiguous|LOCAL

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

Code | Display | System
---- | ------- | ------
2106-3|White|LOCAL
2106-3|W|LOCAL
2106-3|Caucasian|LOCAL
2106-3|C|LOCAL
2106-3|2106-3|LOCAL
1002-5|American Indian or Alaska Native|LOCAL
1002-5|American Indian|LOCAL
1002-5|Native American|LOCAL
1002-5|1002-5|LOCAL
2054-5|Black or African American|LOCAL
2054-5|African American|LOCAL
2054-5|African American Alaska Native|LOCAL
2054-5|African American Black|LOCAL
2054-5|Black|LOCAL
2054-5|B|LOCAL
2054-5|2054-5|LOCAL
2076-8|Native Hawaiian or Other Pacific Islander|LOCAL
2076-8|Hawaiian|LOCAL
2076-8|NH|LOCAL
2076-8|2076-8|LOCAL
2131-1|Other|LOCAL
2131-1|OTH|LOCAL
2131-1|O|LOCAL
2131-1|Other Race|LOCAL
2131-1|Other Race White|LOCAL
2131-1|Other Race,White|LOCAL
2131-1|Other Race Black|LOCAL
2131-1|Other Race,Black|LOCAL
2131-1|2131-1|LOCAL
2028-9|Asian|LOCAL
2028-9|Asian Indian|LOCAL
2028-9|2028-9|LOCAL
UNK|Unknown|LOCAL
UNK|UNK|LOCAL
UNK|U|LOCAL
UNK|Patient Declines|LOCAL
UNK|null|LOCAL
ASKU|Asked, but unknown|LOCAL
ASKU|ASKU|LOCAL

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

**Cardinality**: [1..1]

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

**Name**: Pregnant

**ReportStream Internal Name**: pregnant

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 82810-3

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
77386006|Pregnant|SNOMED_CT
77386006|Currently Pregnant|SNOMED_CT
77386006|Y|SNOMED_CT
77386006|YES|SNOMED_CT
77386006|77386006|SNOMED_CT
60001007|Not Pregnant|SNOMED_CT
60001007|Not Currently Pregnant|SNOMED_CT
60001007|N|SNOMED_CT
60001007|NO|SNOMED_CT
60001007|60001007|SNOMED_CT
261665006|Unknown|SNOMED_CT
261665006|U|SNOMED_CT
261665006|UNK|SNOMED_CT
261665006|N/A|SNOMED_CT
261665006|NA|SNOMED_CT
261665006|NR|SNOMED_CT
261665006|NP|SNOMED_CT
261665006|maybe|SNOMED_CT
261665006|261665006|SNOMED_CT

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

Code | Display | System
---- | ------- | ------
D|Debugging|HL7
P|Production|HL7
T|Training|HL7

**Documentation**:

P, D, or T for Production, Debugging, or Training

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

**Cardinality**: [1..1]

**Documentation**:

Facility populates multiple fields.  This instance populates reporting_facility_name.

---

**Name**: sender_id

**ReportStream Internal Name**: sender_id

**Type**: TEXT

**PII**: No

**Default Value**: reddyfmc-la

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

**Cardinality**: [1..1]

**Documentation**:

DateColl populates multiple fields.  This instance populates specimen_collection_date_time.

---

**Name**: Specimen_Type

**ReportStream Internal Name**: specimen_source_site_code

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 53342003

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
71836000|Nasopharyngeal structure (body structure)|SNOMED_CT
71836000|Nasopharyngeal swab|SNOMED_CT
71836000|258500001|SNOMED_CT
71836000|Nasopharyngeal aspirate|SNOMED_CT
71836000|258411007|SNOMED_CT
71836000|Nasopharyngeal washings|SNOMED_CT
71836000|Nasopharyngeal wash|SNOMED_CT
71836000|258467004|SNOMED_CT
71836000|71836000|SNOMED_CT
45206002|Nasal structure (body structure)|SNOMED_CT
45206002|Nasal aspirate|SNOMED_CT
45206002|Nasal aspirate specimen|SNOMED_CT
45206002|429931000124105|SNOMED_CT
45206002|45206002|SNOMED_CT
53342003|Internal nose structure (body structure)|SNOMED_CT
53342003|Varied|SNOMED_CT
53342003|Swab of internal nose|SNOMED_CT
53342003|Anterior nares swab|SNOMED_CT
53342003|Anterior nasal swab|SNOMED_CT
53342003|697989009|SNOMED_CT
53342003|Mid-turbinate nasal swab|SNOMED_CT
53342003|871810001|SNOMED_CT
53342003|Nasal|SNOMED_CT
53342003|Nasal Swab|SNOMED_CT
53342003|445297001|SNOMED_CT
53342003|53342003|SNOMED_CT
29092000|Serum|SNOMED_CT
29092000|Serum specimen|SNOMED_CT
29092000|119364003|SNOMED_CT
29092000|Plasma|SNOMED_CT
29092000|Plasma specimen|SNOMED_CT
29092000|119361006|SNOMED_CT
29092000|Whole Blood|SNOMED_CT
29092000|Whole Blood Sample|SNOMED_CT
29092000|258580003|SNOMED_CT
29092000|Blood specimen|SNOMED_CT
29092000|119297000|SNOMED_CT
29092000|Venous blood specimen|SNOMED_CT
29092000|Venous whole blood|SNOMED_CT
29092000|122555007|SNOMED_CT
29092000|Capillary blood specimen|SNOMED_CT
29092000|fingerstick whole blood|SNOMED_CT
29092000|122554006|SNOMED_CT
29092000|Dried blood spot specimen|SNOMED_CT
29092000|Dried blood spot|SNOMED_CT
29092000|fingerstick blood dried blood spot|SNOMED_CT
29092000|440500007|SNOMED_CT
31389004|Throat Swab|SNOMED_CT
31389004|Oropharyngeal Swab|SNOMED_CT
31389004|258529004|SNOMED_CT
31389004|31389004|SNOMED_CT
123851003|Sputum specimen|SNOMED_CT
123851003|Sputum|SNOMED_CT
123851003|119334006|SNOMED_CT
123851003|Oral Swab|SNOMED_CT
123851003|418932006|SNOMED_CT
123851003|Saliva specimen|SNOMED_CT
123851003|Saliva|SNOMED_CT
123851003|258560004|SNOMED_CT
123851003|123851003|SNOMED_CT
39607008|Lower respiratory fluid sample|SNOMED_CT
39607008|lower respiratory tract aspirates|SNOMED_CT
39607008|309171007|SNOMED_CT
39607008|Bronchoalveolar lavage fluid sample|SNOMED_CT
39607008|Bronchoalveolar lavage fluid|SNOMED_CT
39607008|Bronchoalveolar lavage|SNOMED_CT
39607008|258607008|SNOMED_CT
39607008|39607008|SNOMED_CT

**Documentation**:

Translate inbound text to outbound SNOMED Codes

---

**Name**: Specimen_Type

**ReportStream Internal Name**: specimen_type

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 445297001

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
445297001|Swab of internal nose|SNOMED_CT
445297001|Nasal Swab|SNOMED_CT
445297001|Nasal|SNOMED_CT
445297001|Varied|SNOMED_CT
445297001|445297001|SNOMED_CT
258500001|Nasopharyngeal swab|SNOMED_CT
258500001|258500001|SNOMED_CT
871810001|Mid-turbinate nasal swab|SNOMED_CT
871810001|871810001|SNOMED_CT
697989009|Anterior nares swab|SNOMED_CT
697989009|Anterior nasal swab|SNOMED_CT
697989009|697989009|SNOMED_CT
258411007|Nasopharyngeal aspirate|SNOMED_CT
258411007|258411007|SNOMED_CT
258467004|Nasopharyngeal washings|SNOMED_CT
258467004|Nasopharyngeal wash|SNOMED_CT
258467004|258467004|SNOMED_CT
429931000124105|Nasal aspirate|SNOMED_CT
429931000124105|Nasal aspirate specimen|SNOMED_CT
429931000124105|429931000124105|SNOMED_CT
258529004|Throat swab|SNOMED_CT
258529004|Oropharyngeal Swab|SNOMED_CT
258529004|258529004|SNOMED_CT
418932006|Oral Swab|SNOMED_CT
418932006|418932006|SNOMED_CT
119334006|Sputum specimen|SNOMED_CT
119334006|Sputum|SNOMED_CT
119334006|119334006|SNOMED_CT
258560004|Saliva specimen|SNOMED_CT
258560004|Saliva|SNOMED_CT
258560004|258560004|SNOMED_CT
119364003|Serum specimen|SNOMED_CT
119364003|Serum|SNOMED_CT
119364003|119364003|SNOMED_CT
119361006|Plasma specimen|SNOMED_CT
119361006|Plasma|SNOMED_CT
119361006|119361006|SNOMED_CT
258580003|Whole blood sample|SNOMED_CT
258580003|Whole blood|SNOMED_CT
258580003|258580003|SNOMED_CT
122555007|Venous blood specimen|SNOMED_CT
122555007|Venous whole blood|SNOMED_CT
122555007|122555007|SNOMED_CT
119297000|Blood specimen|SNOMED_CT
119297000|119297000|SNOMED_CT
122554006|Capillary blood specimen|SNOMED_CT
122554006|fingerstick whole blood|SNOMED_CT
122554006|122554006|SNOMED_CT
440500007|Dried blood spot specimen|SNOMED_CT
440500007|Dried blood spot|SNOMED_CT
440500007|fingerstick blood dried blood spot|SNOMED_CT
440500007|440500007|SNOMED_CT
433801000124107|Nasopharyngeal and oropharyngeal swab|SNOMED_CT
433801000124107|Nasal and throat swab combination|SNOMED_CT
433801000124107|Nasal and throat swab|SNOMED_CT
433801000124107|433801000124107|SNOMED_CT
309171007|Lower respiratory fluid sample|SNOMED_CT
309171007|lower respiratory tract aspirates|SNOMED_CT
309171007|309171007|SNOMED_CT
258607008|Bronchoalveolar lavage fluid sample|SNOMED_CT
258607008|Bronchoalveolar lavage fluid|SNOMED_CT
258607008|Bronchoalveolar lavage|SNOMED_CT
258607008|258607008|SNOMED_CT

**Documentation**:

Translate inbound text to outbound SNOMED Codes

---

**Name**: TestName

**ReportStream Internal Name**: test_performed_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Performed LOINC Long Name

**Documentation**:

TestName populates multiple fields.  This instance populates test_performed_name.

---

**Name**: LabResult

**ReportStream Internal Name**: test_result

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
260385009|Negative|SNOMED_CT
260385009|Neg|SNOMED_CT
260385009|Negative *** High ***|SNOMED_CT
260385009|Negative *** Low ***|SNOMED_CT
260385009|260385009|SNOMED_CT
260415000|Not detected|SNOMED_CT
260415000|NDET|SNOMED_CT
260415000|260415000|SNOMED_CT
260373001|Detected|SNOMED_CT
260373001|DET|SNOMED_CT
260373001|260373001|SNOMED_CT
10828004|Positive|SNOMED_CT
10828004|Pos|SNOMED_CT
10828004|Positive (Abnormal)|SNOMED_CT
10828004|Positive (Alpha Abnormal)|SNOMED_CT
10828004|Positive *** High ***|SNOMED_CT
10828004|Positive  *** High ***|SNOMED_CT
10828004|Positive  *** Low ***|SNOMED_CT
10828004|Positive |SNOMED_CT
10828004|10828004|SNOMED_CT
720735008|Presumptive positive|SNOMED_CT
720735008|720735008|SNOMED_CT
419984006|Inconclusive|SNOMED_CT
419984006|Inconclusive Result|SNOMED_CT
419984006|419984006|SNOMED_CT
42425007|Equivocal|SNOMED_CT
42425007|42425007|SNOMED_CT
895231008|Not detected in pooled specimen|SNOMED_CT
895231008|895231008|SNOMED_CT
462371000124108|Detected in pooled specimen|SNOMED_CT
462371000124108|462371000124108|SNOMED_CT
455371000124106|Invalid result|SNOMED_CT
455371000124106|Invalid|SNOMED_CT
455371000124106|455371000124106|SNOMED_CT
125154007|Specimen unsatisfactory for evaluation|SNOMED_CT
125154007|125154007|SNOMED_CT
840539006|Disease caused by sever acute respiratory syndrome coronavirus 2 (disorder)|SNOMED_CT
840539006|840539006|SNOMED_CT
840544004|Suspected disease caused by severe acute respiratory coronavirus 2 (situation)|SNOMED_CT
840544004|840544004|SNOMED_CT
840546002|Exposure to severe acute respiratory syndrome coronavirus 2 (event)|SNOMED_CT
840546002|840546002|SNOMED_CT
840533007|Severe acute respiratory syndrome coronavirus 2 (organism)|SNOMED_CT
840533007|840533007|SNOMED_CT
840536004|Antigen of severe acute respiratory syndrome coronavirus 2 (substance)|SNOMED_CT
840536004|840536004|SNOMED_CT
840535000|Antibody to severe acute respiratory syndrome coronavirus 2 (substance)|SNOMED_CT
840535000|840535000|SNOMED_CT
840534001|Severe acute respiratory syndrome coronavirus 2 vaccination (procedure)|SNOMED_CT
840534001|840534001|SNOMED_CT
373121007|Test not done|SNOMED_CT
373121007|373121007|SNOMED_CT
82334004|Indeterminate|SNOMED_CT
82334004|82334004|SNOMED_CT

**Documentation**:

Translate multiple inbound Test Result values to RS values

---

**Name**: ResultDate

**ReportStream Internal Name**: test_result_date

**Type**: DATETIME

**PII**: No

**Format**: M/d/yyyy

**Cardinality**: [1..1]

**Documentation**:

ResultDate populates multiple fields.  This instance populates test_result_date.

---

**Name**: Fac_City

**ReportStream Internal Name**: testing_lab_city

**Type**: CITY

**PII**: No

**Cardinality**: [1..1]

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

**Cardinality**: [1..1]

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

**Cardinality**: [1..1]

**Documentation**:

DateColl populates multiple fields.  This instance populates testing_lab_specimen_received_datetime.

---

**Name**: Fac_State

**ReportStream Internal Name**: testing_lab_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

Fac_State populates multiple fields.  This instance populates testing_lab_state.

---

**Name**: Fac_Addr1

**ReportStream Internal Name**: testing_lab_street

**Type**: STREET

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

Fac_Addr1 populates multiple fields.  This instance populates testing_lab_street.

---

**Name**: Fac_Zip

**ReportStream Internal Name**: testing_lab_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

Fac_Zip populates multiple fields.  This instance populates testing_lab_zip_code.

---

**Name**: filler_clia

**ReportStream Internal Name**: filler_clia

**Type**: ID_CLIA

**PII**: No

**Cardinality**: [0..1]

---

**Name**: filler_name

**ReportStream Internal Name**: filler_name

**Type**: TEXT

**PII**: No

**HL7 Fields**

- [OBR-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.2)
- [ORC-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.2)

**Cardinality**: [1..1]

**Documentation**:

Facility populates multiple fields.  This instance populates filler_name.

---

**Name**: filler_order_id

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

**Name**: hospitalized

**ReportStream Internal Name**: hospitalized

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Default Value**: N

**LOINC Code**: 77974-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|Yes|HL7
N|No|HL7
UNK|Unknown|NULLFL

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

Code | Display | System
---- | ------- | ------
Y|Yes|HL7
N|No|HL7
UNK|Unknown|NULLFL

**Documentation**:

iPatientCare is an ambulatory EMR, so this field is defaulted to 'N'.

---

**Name**: patient_county

**ReportStream Internal Name**: patient_county

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: zip-code-data

**Table Column**: county

---

**Name**: patient_id_assigner

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

**Name**: patient_id_type

**ReportStream Internal Name**: patient_id_type

**Type**: TEXT

**PII**: No

**Default Value**: PI

**Cardinality**: [0..1]

---

**Name**: placer_order_id

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

**Name**: reporting_facility_clia

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

**Name**: test_result_status

**ReportStream Internal Name**: test_result_status

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Default Value**: F

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
C|Record coming over is a correction and thus replaces a final result|HL7
D|Deletes the OBX record|HL7
F|Final results; Can only be changed with a corrected result|HL7
I|Specimen in lab; results pending|HL7
N|Not asked; used to affirmatively document that the observation identified in the OBX was not sought when the universal service ID in OBR-4 implies that it would be sought.|HL7
O|Order detail description only (no result)|HL7
P|Preliminary results|HL7
R|Results entered -- not verified|HL7
S|Partial results|HL7
U|Results status change to final without retransmitting results already sent as ‘preliminary.’  E.g., radiology changes status from preliminary to final|HL7
W|Post original as wrong, e.g., transmitted for wrong patient|HL7
X|Results cannot be obtained for this observation|HL7

**Documentation**:

The test result status, which is different from the test result itself. Per the valueset, this indicates if
the test result is in some intermediate status, is a correction, or is the final result.


---

**Name**: test_type

**ReportStream Internal Name**: test_type

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: TestType

---

**Name**: testing_lab_id

**ReportStream Internal Name**: testing_lab_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

Typically this will be the same as the `testing_lab_clia`, but potentially could not be.

---
