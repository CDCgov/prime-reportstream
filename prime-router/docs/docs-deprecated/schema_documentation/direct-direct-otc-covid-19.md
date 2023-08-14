### Schema: direct/direct-otc-covid-19
### Topic: covid-19
### Tracking Element: (message_id)
### Base On: [covid-19](./covid-19.md)
### Extends: none
#### Description: Direct Schema for OTC Results based on NIH At-Home specs

---

**Name**: testReportDate

**ReportStream Internal Name**: date_result_released

**Type**: DATETIME

**PII**: No

**Cardinality**: [1..1]

---

**Name**: testReportDate

**ReportStream Internal Name**: date_result_released_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: testId

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

**Name**: testOrderedDate

**ReportStream Internal Name**: order_test_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [1..1]

---

**Name**: patientCity

**ReportStream Internal Name**: ordering_facility_city

**Type**: CITY

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The city of the facility which the test was ordered from
---

**Name**: patientPhone

**ReportStream Internal Name**: ordering_facility_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The phone number of the facility which the test was ordered from
---

**Name**: patientState

**ReportStream Internal Name**: ordering_facility_state

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:
The state of the facility which the test was ordered from
---

**Name**: patientHomeAddress

**ReportStream Internal Name**: ordering_facility_street

**Type**: STREET

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The address of the facility which the test was ordered from
---

**Name**: patientHomeAddress2

**ReportStream Internal Name**: ordering_facility_street2

**Type**: STREET_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The secondary address of the facility which the test was ordered from
---

**Name**: patientZip

**ReportStream Internal Name**: ordering_facility_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The zip code of the facility which the test was ordered from
---

**Name**: orderingProviderCity

**ReportStream Internal Name**: ordering_provider_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The city of the provider
---

**Name**: orderingProviderFname

**ReportStream Internal Name**: ordering_provider_first_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.3)
- [ORC-12-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.3)

**Cardinality**: [1..1]

**Documentation**:
The first name of the provider who ordered the test
---

**Name**: orderingProviderFname

**ReportStream Internal Name**: ordering_provider_first_name_fromCSV

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: orderingProviderNpi

**ReportStream Internal Name**: ordering_provider_id

**Type**: ID_NPI

**PII**: No

**HL7 Fields**

- [OBR-16-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.1)
- [ORC-12-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.1)

**Cardinality**: [0..1]

**Documentation**:
The ordering provider’s National Provider Identifier
---

**Name**: orderingProviderLname

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

**Name**: orderingProviderPhone

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

**Name**: orderingProviderState

**ReportStream Internal Name**: ordering_provider_state_fromCSV

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: orderingProviderState

**ReportStream Internal Name**: ordering_provider_state_fromValueSet

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
AA|Armed Americas|LOCAL
AA|Armed Americas (except Canada)|LOCAL
AA|AA|LOCAL
AB|ALBERTA|LOCAL
AB|AB|LOCAL
AE|Armed Forces Europe, the Middle East, and Canada|LOCAL
AE|AE|LOCAL
AL|ALABAMA|LOCAL
AL|AL|LOCAL
AK|ALASKA|LOCAL
AK|AK|LOCAL
AP|Armed Forces Pacific|LOCAL
AP|AP|LOCAL
AS|AMERICAN SAMOA|LOCAL
AS|AS|LOCAL
AZ|ARIZONA|LOCAL
AZ|AZ|LOCAL
AR|ARKANSAS|LOCAL
AR|AR|LOCAL
BC|BRITISH COLUMBIA|LOCAL
BC|BC|LOCAL
CA|CALIFORNIA|LOCAL
CA|CA|LOCAL
CO|COLORADO|LOCAL
CO|CO|LOCAL
CT|CONNECTICUT|LOCAL
CT|CT|LOCAL
DE|DELAWARE|LOCAL
DE|DE|LOCAL
FL|FLORIDA|LOCAL
FL|FL|LOCAL
FM|FEDERAED STATES OF MICRONESIA|LOCAL
FM|FM|LOCAL
GA|GEORGIA|LOCAL
GA|GA|LOCAL
GU|GUAM|LOCAL
GU|GU|LOCAL
HI|HAWAII|LOCAL
HI|HI|LOCAL
ID|IDAHO|LOCAL
ID|ID|LOCAL
IL|ILLINOIS|LOCAL
IL|ILLINIOS|LOCAL
IL|IL|LOCAL
IN|INDIANA|LOCAL
IN|INDANIA|LOCAL
IN|IN|LOCAL
IA|IOWA|LOCAL
IA|IA|LOCAL
KS|KANSAS|LOCAL
KS|KS|LOCAL
KY|KENTUCKY|LOCAL
KY|KY|LOCAL
LA|LOUISIANA|LOCAL
LA|LA|LOCAL
MB|MANITOBA|LOCAL
MB|MB|LOCAL
ME|MAINE|LOCAL
ME|ME|LOCAL
MD|MARYLAND|LOCAL
MD|MD|LOCAL
MA|MASSACHUSETTS|LOCAL
MA|MA|LOCAL
MH|MARSHALL ISLANDS|LOCAL
MH|MH|LOCAL
MI|MICHIGAN|LOCAL
MI|MI|LOCAL
MN|MINNESOTA|LOCAL
MN|MN|LOCAL
MP|NORTHERN MARIANA ISLANDS|LOCAL
MP|NORTH MARIANA ISLANDS|LOCAL
MP|N MARIANA ISLANDS|LOCAL
MP|N. MARIANA ISLANDS|LOCAL
MP|MP|LOCAL
MS|MISSISSIPPI|LOCAL
MS|MS|LOCAL
MO|MISSOURI|LOCAL
MO|MO|LOCAL
MT|MONTANA|LOCAL
MT|MT|LOCAL
NC|NORTH CAROLINA|LOCAL
NC|N. CAROLINA|LOCAL
NC|N CAROLINA|LOCAL
NC|NC|LOCAL
NE|NEBRASKA|LOCAL
NE|NE|LOCAL
NF|NEWFOUNDLAND|LOCAL
NF|TERRE NEUVE|LOCAL
NF|NF|LOCAL
NV|NEVADA|LOCAL
NV|NV|LOCAL
NH|NEW HAMPSHIRE|LOCAL
NH|NH|LOCAL
NJ|NEW JERSEY|LOCAL
NJ|NJ|LOCAL
NM|NEW MEXICO|LOCAL
NM|NM|LOCAL
ND|NORTH DAKOTA|LOCAL
ND|N. DAKOTA|LOCAL
ND|N DAKOTA|LOCAL
ND|ND|LOCAL
NT|NORTHWEST TERRITORIES|LOCAL
NT|TERRITOIRES DU NORD-QUEST|LOCAL
NT|NT|LOCAL
NS|NOVA SCOTIA|LOCAL
NS|NOUVELLE ECOSSE|LOCAL
NS|NS|LOCAL
NU|NUNAVUT|LOCAL
NU|NU|LOCAL
NY|NEW YORK|LOCAL
NY|NY|LOCAL
OH|OHIO|LOCAL
OH|OH|LOCAL
OK|OKLAHOMA|LOCAL
OK|OK|LOCAL
ON|ONTARIO|LOCAL
ON|ON|LOCAL
OR|OREGON|LOCAL
OR|OR|LOCAL
PA|PENNSYLVANIA|LOCAL
PA|PA|LOCAL
PE|PRINCE EDWARD ISLAND|LOCAL
PE|LLE DU PRINCE EDWARD|LOCAL
PE|PE|LOCAL
PR|PUERTO RICO|LOCAL
PR|PR|LOCAL
PW|PALAU|LOCAL
PW|PW|LOCAL
RI|RHODE ISLAND|LOCAL
RI|RI|LOCAL
QC|QUEBEC|LOCAL
QC|QC|LOCAL
SC|SOUTH CAROLINA|LOCAL
SC|S. CAROLINA|LOCAL
SC|S CAROLINA|LOCAL
SC|SC|LOCAL
SD|SOUTH DAKOTA|LOCAL
SD|S. DAKOTA|LOCAL
SD|S DAKOTA|LOCAL
SD|SD|LOCAL
SK|SASKATCHEWAN|LOCAL
SK|SK|LOCAL
TN|TENNESSEE|LOCAL
TN|TN|LOCAL
TX|TEXAS|LOCAL
TX|TX|LOCAL
UT|UTAH|LOCAL
UT|UT|LOCAL
VT|VERMONT|LOCAL
VT|VT|LOCAL
VI|VIRGIN ISLANDS|LOCAL
VI|VI|LOCAL
VA|VIRGINIA|LOCAL
VA|VA|LOCAL
WA|WASHINGTON|LOCAL
WA|WA|LOCAL
WV|WEST VIRGINIA|LOCAL
WV|W VIRGINIA|LOCAL
WV|W. VIRGINIA|LOCAL
WV|WV|LOCAL
WI|WISCONSIN|LOCAL
WI|WI|LOCAL
WY|WYOMING|LOCAL
WY|WY|LOCAL
YT|YUKON TERRITORY|LOCAL
YT|TERRITOIRES DU YUKON|LOCAL
YT|YT|LOCAL

**Documentation**:
Translate multiple inbound State values to RS values
---

**Name**: orderingProviderAddress

**ReportStream Internal Name**: ordering_provider_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The street address of the provider
---

**Name**: orderingProviderAddress2

**ReportStream Internal Name**: ordering_provider_street2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The street second address of the provider
---

**Name**: orderingProviderZip

**ReportStream Internal Name**: ordering_provider_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The zip code of the provider
---

**Name**: performingFacility

**ReportStream Internal Name**: otc_flag

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
SA.OverTheCounter|00Z0000014|NULLFL
|00Z0000015|NULLFL

---

**Name**: patientCity

**ReportStream Internal Name**: patient_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's city
---

**Name**: patientCounty

**ReportStream Internal Name**: patient_county

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: patientDob

**ReportStream Internal Name**: patient_dob

**Type**: DATE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.

---

**Name**: patientEmail

**ReportStream Internal Name**: patient_email

**Type**: EMAIL

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patientEthnicity

**ReportStream Internal Name**: patient_ethnicity

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

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

**Name**: patientNameFirst

**ReportStream Internal Name**: patient_first_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's first name
---

**Name**: patientSex

**ReportStream Internal Name**: patient_gender

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

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

**Name**: patientUniqueId

**ReportStream Internal Name**: patient_id

**Type**: TEXT

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:
The ID for the patient within one of the reporting entities for this lab result. It could be the
the patient ID from the testing lab, the oder placer, the ordering provider, or even within the PRIME system itself.

---

**Name**: patientNameLast

**ReportStream Internal Name**: patient_last_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:
The patient's last name
---

**Name**: patientNameMiddle

**ReportStream Internal Name**: patient_middle_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patientPhone

**ReportStream Internal Name**: patient_phone_number

**Type**: TELEPHONE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's phone number with area code
---

**Name**: patientRace

**ReportStream Internal Name**: patient_race

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

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

**Name**: patientState

**ReportStream Internal Name**: patient_state_fromCSV

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: patientState

**ReportStream Internal Name**: patient_state_fromValueSet

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
AA|Armed Americas|LOCAL
AA|Armed Americas (except Canada)|LOCAL
AA|AA|LOCAL
AB|ALBERTA|LOCAL
AB|AB|LOCAL
AE|Armed Forces Europe, the Middle East, and Canada|LOCAL
AE|AE|LOCAL
AL|ALABAMA|LOCAL
AL|AL|LOCAL
AK|ALASKA|LOCAL
AK|AK|LOCAL
AP|Armed Forces Pacific|LOCAL
AP|AP|LOCAL
AS|AMERICAN SAMOA|LOCAL
AS|AS|LOCAL
AZ|ARIZONA|LOCAL
AZ|AZ|LOCAL
AR|ARKANSAS|LOCAL
AR|AR|LOCAL
BC|BRITISH COLUMBIA|LOCAL
BC|BC|LOCAL
CA|CALIFORNIA|LOCAL
CA|CA|LOCAL
CO|COLORADO|LOCAL
CO|CO|LOCAL
CT|CONNECTICUT|LOCAL
CT|CT|LOCAL
DE|DELAWARE|LOCAL
DE|DE|LOCAL
FL|FLORIDA|LOCAL
FL|FL|LOCAL
FM|FEDERAED STATES OF MICRONESIA|LOCAL
FM|FM|LOCAL
GA|GEORGIA|LOCAL
GA|GA|LOCAL
GU|GUAM|LOCAL
GU|GU|LOCAL
HI|HAWAII|LOCAL
HI|HI|LOCAL
ID|IDAHO|LOCAL
ID|ID|LOCAL
IL|ILLINOIS|LOCAL
IL|ILLINIOS|LOCAL
IL|IL|LOCAL
IN|INDIANA|LOCAL
IN|INDANIA|LOCAL
IN|IN|LOCAL
IA|IOWA|LOCAL
IA|IA|LOCAL
KS|KANSAS|LOCAL
KS|KS|LOCAL
KY|KENTUCKY|LOCAL
KY|KY|LOCAL
LA|LOUISIANA|LOCAL
LA|LA|LOCAL
MB|MANITOBA|LOCAL
MB|MB|LOCAL
ME|MAINE|LOCAL
ME|ME|LOCAL
MD|MARYLAND|LOCAL
MD|MD|LOCAL
MA|MASSACHUSETTS|LOCAL
MA|MA|LOCAL
MH|MARSHALL ISLANDS|LOCAL
MH|MH|LOCAL
MI|MICHIGAN|LOCAL
MI|MI|LOCAL
MN|MINNESOTA|LOCAL
MN|MN|LOCAL
MP|NORTHERN MARIANA ISLANDS|LOCAL
MP|NORTH MARIANA ISLANDS|LOCAL
MP|N MARIANA ISLANDS|LOCAL
MP|N. MARIANA ISLANDS|LOCAL
MP|MP|LOCAL
MS|MISSISSIPPI|LOCAL
MS|MS|LOCAL
MO|MISSOURI|LOCAL
MO|MO|LOCAL
MT|MONTANA|LOCAL
MT|MT|LOCAL
NC|NORTH CAROLINA|LOCAL
NC|N. CAROLINA|LOCAL
NC|N CAROLINA|LOCAL
NC|NC|LOCAL
NE|NEBRASKA|LOCAL
NE|NE|LOCAL
NF|NEWFOUNDLAND|LOCAL
NF|TERRE NEUVE|LOCAL
NF|NF|LOCAL
NV|NEVADA|LOCAL
NV|NV|LOCAL
NH|NEW HAMPSHIRE|LOCAL
NH|NH|LOCAL
NJ|NEW JERSEY|LOCAL
NJ|NJ|LOCAL
NM|NEW MEXICO|LOCAL
NM|NM|LOCAL
ND|NORTH DAKOTA|LOCAL
ND|N. DAKOTA|LOCAL
ND|N DAKOTA|LOCAL
ND|ND|LOCAL
NT|NORTHWEST TERRITORIES|LOCAL
NT|TERRITOIRES DU NORD-QUEST|LOCAL
NT|NT|LOCAL
NS|NOVA SCOTIA|LOCAL
NS|NOUVELLE ECOSSE|LOCAL
NS|NS|LOCAL
NU|NUNAVUT|LOCAL
NU|NU|LOCAL
NY|NEW YORK|LOCAL
NY|NY|LOCAL
OH|OHIO|LOCAL
OH|OH|LOCAL
OK|OKLAHOMA|LOCAL
OK|OK|LOCAL
ON|ONTARIO|LOCAL
ON|ON|LOCAL
OR|OREGON|LOCAL
OR|OR|LOCAL
PA|PENNSYLVANIA|LOCAL
PA|PA|LOCAL
PE|PRINCE EDWARD ISLAND|LOCAL
PE|LLE DU PRINCE EDWARD|LOCAL
PE|PE|LOCAL
PR|PUERTO RICO|LOCAL
PR|PR|LOCAL
PW|PALAU|LOCAL
PW|PW|LOCAL
RI|RHODE ISLAND|LOCAL
RI|RI|LOCAL
QC|QUEBEC|LOCAL
QC|QC|LOCAL
SC|SOUTH CAROLINA|LOCAL
SC|S. CAROLINA|LOCAL
SC|S CAROLINA|LOCAL
SC|SC|LOCAL
SD|SOUTH DAKOTA|LOCAL
SD|S. DAKOTA|LOCAL
SD|S DAKOTA|LOCAL
SD|SD|LOCAL
SK|SASKATCHEWAN|LOCAL
SK|SK|LOCAL
TN|TENNESSEE|LOCAL
TN|TN|LOCAL
TX|TEXAS|LOCAL
TX|TX|LOCAL
UT|UTAH|LOCAL
UT|UT|LOCAL
VT|VERMONT|LOCAL
VT|VT|LOCAL
VI|VIRGIN ISLANDS|LOCAL
VI|VI|LOCAL
VA|VIRGINIA|LOCAL
VA|VA|LOCAL
WA|WASHINGTON|LOCAL
WA|WA|LOCAL
WV|WEST VIRGINIA|LOCAL
WV|W VIRGINIA|LOCAL
WV|W. VIRGINIA|LOCAL
WV|WV|LOCAL
WI|WISCONSIN|LOCAL
WI|WI|LOCAL
WY|WYOMING|LOCAL
WY|WY|LOCAL
YT|YUKON TERRITORY|LOCAL
YT|TERRITOIRES DU YUKON|LOCAL
YT|YT|LOCAL

**Documentation**:
Translate multiple inbound State values to RS values
---

**Name**: patientHomeAddress

**ReportStream Internal Name**: patient_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's street address
---

**Name**: patientHomeAddress2

**ReportStream Internal Name**: patient_street2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's second address line
---

**Name**: patientZip

**ReportStream Internal Name**: patient_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
The patient's zip code
---

**Name**: correctedTestId

**ReportStream Internal Name**: previous_message_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
pointer/link to the unique id of a previously submitted result.  Usually blank. Or, if an item modifies/corrects a prior item, this field holds the message_id of the prior item.
---

**Name**: processingModeCode

**ReportStream Internal Name**: processing_mode_code

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: P

**Cardinality**: [1..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
D|Debugging|HL7
P|Production|HL7
T|Training|HL7
T|Training|HL7
T|Training|HL7
T|Training|HL7
P|Production|HL7
P|Production|HL7

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
T|TESTING|HL7
T|INACTIVE|HL7
T|T|HL7
P|ACTIVE|HL7
P|P|HL7

**Documentation**:
P, D, or T for Production, Debugging, or Training
---

**Name**: performingFacility

**ReportStream Internal Name**: reporting_facility_clia

**Type**: ID_CLIA

**PII**: No

**HL7 Fields**

- [MSH-4-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/MSH.4.2)
- [PID-3-4-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4.2)
- [PID-3-6-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.6.2)
- [SPM-2-1-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.1.3)
- [SPM-2-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2.2.3)

**Cardinality**: [1..1]

**Documentation**:
Expecting a CLIA number here.  eg, "10D1234567"
---

**Name**: specimenCollectedDate

**ReportStream Internal Name**: specimen_collection_date_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: specimenCollectedDate

**ReportStream Internal Name**: specimen_collection_date_time

**Type**: DATETIME

**PII**: No

**HL7 Fields**

- [OBR-7](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.7)
- [OBR-8](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.8)
- [OBX-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.14)
- [SPM-17-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.17.1)

**Cardinality**: [1..1]

**Documentation**:
The date which the specimen was collected. The default format is yyyyMMddHHmmsszz

---

**Name**: specimenSource

**ReportStream Internal Name**: specimen_source_site_code

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

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

**Name**: specimenSource

**ReportStream Internal Name**: specimen_type

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

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
433871000124101|Nasal washings|SNOMED_CT
433871000124101|433871000124101|SNOMED_CT
441620008|Sputum specimen obtained by sputum induction|SNOMED_CT
441620008|441620008|SNOMED_CT
441903006|Coughed sputum specimen|SNOMED_CT
441903006|441903006|SNOMED_CT
119336008|Specimen from trachea obtained by aspiration|SNOMED_CT
119336008|119336008|SNOMED_CT
258610001|Oral fluid specimen|SNOMED_CT
258610001|258610001|SNOMED_CT
119335007|Specimen obtained by bronchial aspiration|SNOMED_CT
119335007|119335007|SNOMED_CT
445447003|Exhaled air specimen|SNOMED_CT
445447003|445447003|SNOMED_CT

**Documentation**:
Translate inbound text to outbound SNOMED Codes
---

**Name**: symptomatic

**ReportStream Internal Name**: symptomatic_for_disease

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 95419-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
Y|YES|LOCAL
Y|Y|LOCAL
N|NO|LOCAL
N|N|LOCAL
UNK|Unknown|LOCAL
UNK|U|LOCAL
UNK|UNK|LOCAL
UNK|N/A|LOCAL
UNK|NA|LOCAL
UNK|NR|LOCAL
UNK|NP|LOCAL
UNK|maybe|LOCAL

**Documentation**:
Translate multiple inbound Y/N/U AOE values to RS values
---

**Name**: deviceIdentifier

**ReportStream Internal Name**: test_kit_name_id

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: Testkit Name ID

**Documentation**:
Must match LIVD column M, "Test Kit Name ID"
---

**Name**: testPerformed

**ReportStream Internal Name**: test_performed_code

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Performed LOINC Code

**Documentation**:
The LOINC code of the test performed. This is a standardized coded value describing the test
---

**Name**: testResult

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

**Name**: testResultDate

**ReportStream Internal Name**: test_result_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [1..1]

---

**Name**: performingFacility

**ReportStream Internal Name**: testing_lab_clia

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**HL7 Fields**

- [OBR-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.3)
- [OBR-3-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.3)
- [OBX-15-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.15.1)
- [OBX-23-10](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.23.10)
- [ORC-2-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.3)
- [ORC-3-3](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.3)

**Cardinality**: [1..1]

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
00Z0000014|00Z0000014|NULLFL
00Z0000015|00Z0000015|NULLFL

**Documentation**:
CLIA Number from the laboratory that sends the message to DOH

An example of the ID is 03D2159846

---

**Name**: performingFacility

**ReportStream Internal Name**: testing_lab_name

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**HL7 Fields**

- [OBR-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.2.2)
- [OBR-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.3.2)
- [OBX-15-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.15.2)
- [OBX-23-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBX.23.1)
- [ORC-2-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.2.2)
- [ORC-3-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.3.2)
- [PID-3-4-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/PID.3.4.1)

**Cardinality**: [1..1]

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
SA.OverTheCounter|00Z0000014|NULLFL
SA.Prescription|00Z0000015|NULLFL

**Documentation**:
The name of the laboratory which performed the test, can be the same as the sending facility name
---

**Name**: performingFacility

**ReportStream Internal Name**: testing_lab_street

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
11 Fake AtHome Test Street|00Z0000014|NULLFL
12 Fake AtHome Test Street|00Z0000015|NULLFL

**Documentation**:
The street address for the testing lab
---

**Name**: abnormal_flag

**ReportStream Internal Name**: abnormal_flag

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
A|Abnormal (applies to non-numeric results)|HL7
&#62;|Above absolute high-off instrument scale|HL7
H|Above high normal|HL7
HH|Above upper panic limits|HL7
AC|Anti-complementary substances present|HL7
<|Below absolute low-off instrument scale|HL7
L|Below low normal|HL7
LL|Below lower panic limits|HL7
B|Better--use when direction not relevant|HL7
TOX|Cytotoxic substance present|HL7
DET|Detected|HL7
IND|Indeterminate|HL7
I|Intermediate. Indicates for microbiology susceptibilities only.|HL7
MS|Moderately susceptible. Indicates for microbiology susceptibilities only.|HL7
NEG|Negative|HL7
null|No range defined, or normal ranges don't apply|HL7
NR|Non-reactive|HL7
N|Normal (applies to non-numeric results)|HL7
ND|Not Detected|HL7
POS|Positive|HL7
QCF|Quality Control Failure|HL7
RR|Reactive|HL7
R|Resistant. Indicates for microbiology susceptibilities only.|HL7
D|Significant change down|HL7
U|Significant change up|HL7
S|Susceptible. Indicates for microbiology susceptibilities only.|HL7
AA|Very abnormal (applies to non-numeric units, analogous to panic limits for numeric units)|HL7
VS|Very susceptible. Indicates for microbiology susceptibilities only.|HL7
WR|Weakly reactive|HL7
W|Worse--use when direction not relevant|HL7

**Documentation**:
This field is generated based on the normalcy status of the result. A = abnormal; N = normal
---

**Name**: accession_number_temp

**ReportStream Internal Name**: accession_number_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: clia_text

**ReportStream Internal Name**: clia_text

**Type**: TEXT

**PII**: No

**Default Value**: CLIA

**Cardinality**: [0..1]

---

**Name**: equipment_model_name

**ReportStream Internal Name**: equipment_model_name

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: Model

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

**Cardinality**: [0..1]

---

**Name**: iso_text

**ReportStream Internal Name**: iso_text

**Type**: TEXT

**PII**: No

**Default Value**: ISO

**Cardinality**: [0..1]

---

**Name**: message_id

**ReportStream Internal Name**: message_id

**Type**: ID

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
unique id to track the usage of the message
---

**Name**: ordering_facility_name

**ReportStream Internal Name**: ordering_facility_name

**Type**: TEXT

**PII**: No

**Default Value**: SA.OverTheCounter

**Cardinality**: [0..1]

**Documentation**:
The name of the facility which the test was ordered from
---

**Name**: ordering_provider_state

**ReportStream Internal Name**: ordering_provider_state

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:
The state of the provider
---

**Name**: patient_county_code

**ReportStream Internal Name**: patient_county_code

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

**Documentation**:
The FIPS code for the patient's county
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
The name of the assigner of the patient_id field. Typically we use the name of the ordering facility
---

**Name**: patient_id_type

**ReportStream Internal Name**: patient_id_type

**Type**: TEXT

**PII**: No

**Default Value**: PI

**Cardinality**: [0..1]

---

**Name**: patient_state

**ReportStream Internal Name**: patient_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:
The patient's state
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

**Name**: processing_mode_code_fromsettings

**ReportStream Internal Name**: processing_mode_code_fromsettings

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: reporting_facility_name

**ReportStream Internal Name**: reporting_facility_name

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
The reporting facility's name
---

**Name**: sender_id

**ReportStream Internal Name**: sender_id

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
ID name of org that is sending this data to ReportStream.  Suitable for provenance or chain of custody tracking.  Not to be confused with sending_application, in which ReportStream acts as the 'sender' to the downstream jurisdiction.
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

**Table Column**: Test Type

---

**Name**: testing_lab_city

**ReportStream Internal Name**: testing_lab_city

**Type**: CITY

**PII**: No

**Default Value**: Yakutat

**Cardinality**: [0..1]

**Documentation**:
The city of the testing lab
---

**Name**: testing_lab_county_code

**ReportStream Internal Name**: testing_lab_county_code

**Type**: TABLE

**PII**: No

**Default Value**: 02282

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: FIPS

**Documentation**:
The county code for the testing lab from the FIPS dataset. This is the standard code used in ELR reporting.

---

**Name**: testing_lab_id

**ReportStream Internal Name**: testing_lab_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Typically this will be the same as the `testing_lab_clia`, but potentially could not be.
---

**Name**: testing_lab_id_assigner

**ReportStream Internal Name**: testing_lab_id_assigner

**Type**: HD

**PII**: No

**Default Value**: CLIA&2.16.840.1.113883.4.7&ISO

**Cardinality**: [0..1]

**Documentation**:
This is the assigner of the CLIA for the testing lab. If the testing lab has a CLIA, this field will be filled in.
---

**Name**: testing_lab_specimen_id

**ReportStream Internal Name**: testing_lab_specimen_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The specimen-id from the testing lab
---

**Name**: testing_lab_state

**ReportStream Internal Name**: testing_lab_state

**Type**: TABLE

**PII**: No

**Default Value**: AK

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:
The state for the testing lab
---

**Name**: testing_lab_zip_code

**ReportStream Internal Name**: testing_lab_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Default Value**: 99689

**Cardinality**: [0..1]

**Documentation**:
The postal code for the testing lab
---
