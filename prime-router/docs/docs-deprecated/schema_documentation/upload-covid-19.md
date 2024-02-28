### Schema: upload-covid-19
### Topic: covid-19
### Tracking Element: (message_id)
### Base On: [covid-19](./covid-19.md)
### Extends: none
#### Description: Schema for CSV Upload Tool

---

**Name**: comment

**ReportStream Internal Name**: comment

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: date_result_released

**ReportStream Internal Name**: date_result_released

**Type**: DATETIME

**PII**: No

**Cardinality**: [1..1]

---

**Name**: date_result_released

**ReportStream Internal Name**: date_result_released_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: employed_in_healthcare

**ReportStream Internal Name**: employed_in_healthcare

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 95418-0

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

**Name**: equipment_model_id

**ReportStream Internal Name**: equipment_model_id

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: Equipment UID

---

**Name**: equipment_model_name

**ReportStream Internal Name**: equipment_model_name

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: Model

---

**Name**: testing_lab_clia

**ReportStream Internal Name**: filler_clia

**Type**: ID_CLIA

**PII**: No

**Cardinality**: [0..1]

---

**Name**: accession_number

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

**Name**: first_test

**ReportStream Internal Name**: first_test

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 95417-2

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

**Name**: hospitalized

**ReportStream Internal Name**: hospitalized

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 77974-4

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

**Name**: icu

**ReportStream Internal Name**: icu

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 95420-6

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

**Name**: illness_onset_date

**ReportStream Internal Name**: illness_onset_date

**Type**: DATE

**PII**: No

**Format**: yyyyMMdd

**Default Value**: 

**LOINC Code**: 65222-2

**Cardinality**: [0..1]

---

**Name**: order_test_date

**ReportStream Internal Name**: order_test_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [1..1]

---

**Name**: ordering_facility_city

**ReportStream Internal Name**: ordering_facility_city

**Type**: CITY

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
The city of the facility which the test was ordered from
---

**Name**: ordering_facility_name

**ReportStream Internal Name**: ordering_facility_name

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
The name of the facility which the test was ordered from
---

**Name**: ordering_facility_name

**ReportStream Internal Name**: ordering_facility_name_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordering_facility_phone_number

**ReportStream Internal Name**: ordering_facility_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
The phone number of the facility which the test was ordered from
---

**Name**: ordering_facility_state

**ReportStream Internal Name**: ordering_facility_state_fromCSV

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordering_facility_state

**ReportStream Internal Name**: ordering_facility_state_fromValueSet

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

**Name**: ordering_facility_street

**ReportStream Internal Name**: ordering_facility_street

**Type**: STREET

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
The address of the facility which the test was ordered from
---

**Name**: ordering_facility_street2

**ReportStream Internal Name**: ordering_facility_street2

**Type**: STREET_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The secondary address of the facility which the test was ordered from
---

**Name**: ordering_facility_street

**ReportStream Internal Name**: ordering_facility_street_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordering_facility_zip_code

**ReportStream Internal Name**: ordering_facility_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
The zip code of the facility which the test was ordered from
---

**Name**: ordering_provider_city

**ReportStream Internal Name**: ordering_provider_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:
The city of the provider
---

**Name**: ordering_provider_first_name

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

**Name**: ordering_provider_id

**ReportStream Internal Name**: ordering_provider_id

**Type**: ID_NPI

**PII**: No

**HL7 Fields**

- [OBR-16-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.1)
- [ORC-12-1](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.1)

**Cardinality**: [1..1]

**Documentation**:
The ordering provider’s National Provider Identifier
---

**Name**: ordering_provider_last_name

**ReportStream Internal Name**: ordering_provider_last_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.2)
- [ORC-12-2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.2)

**Cardinality**: [1..1]

**Documentation**:
The last name of provider who ordered the test
---

**Name**: ordering_provider_middle_name

**ReportStream Internal Name**: ordering_provider_middle_name

**Type**: PERSON_NAME

**PII**: No

**HL7 Fields**

- [OBR-16-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.16.4)
- [ORC-12-4](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.12.4)

**Cardinality**: [0..1]

---

**Name**: ordering_provider_phone_number

**ReportStream Internal Name**: ordering_provider_phone_number

**Type**: TELEPHONE

**PII**: Yes

**HL7 Fields**

- [OBR-17](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/OBR.17)
- [ORC-14](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/ORC.14)

**Cardinality**: [1..1]

**Documentation**:
The phone number of the provider
---

**Name**: ordering_provider_state

**ReportStream Internal Name**: ordering_provider_state_fromCSV

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordering_provider_state

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

**Name**: ordering_provider_street

**ReportStream Internal Name**: ordering_provider_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:
The street address of the provider
---

**Name**: ordering_provider_street2

**ReportStream Internal Name**: ordering_provider_street2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The street second address of the provider
---

**Name**: ordering_provider_zip_code

**ReportStream Internal Name**: ordering_provider_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
The zip code of the provider
---

**Name**: patient_city

**ReportStream Internal Name**: patient_city

**Type**: CITY

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:
The patient's city
---

**Name**: patient_county

**ReportStream Internal Name**: patient_county

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: patient_dob

**ReportStream Internal Name**: patient_dob

**Type**: DATE

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:
The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.

---

**Name**: patient_email

**ReportStream Internal Name**: patient_email

**Type**: EMAIL

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_ethnicity

**ReportStream Internal Name**: patient_ethnicity

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

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

**Name**: patient_first_name

**ReportStream Internal Name**: patient_first_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:
The patient's first name
---

**Name**: patient_gender

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

**Name**: patient_id

**ReportStream Internal Name**: patient_id

**Type**: TEXT

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The ID for the patient within one of the reporting entities for this lab result. It could be the
the patient ID from the testing lab, the oder placer, the ordering provider, or even within the PRIME system itself.

---

**Name**: ordering_facility_name

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

**Name**: patient_last_name

**ReportStream Internal Name**: patient_last_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:
The patient's last name
---

**Name**: patient_middle_name

**ReportStream Internal Name**: patient_middle_name

**Type**: PERSON_NAME

**PII**: Yes

**Cardinality**: [0..1]

---

**Name**: patient_phone_number

**ReportStream Internal Name**: patient_phone_number

**Type**: TELEPHONE

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's phone number with area code
---

**Name**: patient_preferred_language

**ReportStream Internal Name**: patient_preferred_language

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
abk|Abkhazian|ISO
abk|Abkhaz|ISO
abk|abk|ISO
ace|Achinese|ISO
ace|ace|ISO
ach|Acoli|ISO
ach|ach|ISO
ada|Adangme|ISO
ada|ada|ISO
ady|Adyghe; Adygei|ISO
ady|Adyghe|ISO
ady|Adygei|ISO
ady|ady|ISO
aar|Afar|ISO
aar|aar|ISO
afh|Afrihili|ISO
afh|afh|ISO
afr|Afrikaans|ISO
afr|afr|ISO
afa|Afro-Asiatic (Other)|ISO
afa|Afro-Asiatic|ISO
afa|afa|ISO
ain|Ainu|ISO
ain|ain|ISO
aka|Akan|ISO
aka|aka|ISO
akk|Akkadian|ISO
akk|akk|ISO
alb|Albanian|ISO
alb|alb|ISO
ale|Aleut|ISO
ale|ale|ISO
alg|Algonquian languages|ISO
alg|Algonquian|ISO
alg|alg|ISO
tut|Altaic (Other)|ISO
tut|Altaic|ISO
tut|tut|ISO
amh|Amharic|ISO
amh|amh|ISO
anp|Angika|ISO
anp|anp|ISO
apa|Apache languages|ISO
apa|Apache|ISO
apa|apa|ISO
ara|Arabic|ISO
ara|ara|ISO
arg|Aragonese|ISO
arg|arg|ISO
arp|Arapaho|ISO
arp|arp|ISO
arw|Arawak|ISO
arw|arw|ISO
arm|Armenian|ISO
arm|arm|ISO
rup|Aromanian; Arumanian; Macedo-Romanian|ISO
rup|Aromanian|ISO
rup|Arumanian|ISO
rup|Macedo-Romanian|ISO
rup|rup|ISO
art|Artificial (Other)|ISO
art|art|ISO
asm|Assamese|ISO
asm|asm|ISO
aii|Assyrian|ISO
aii|aii|ISO
ast|Asturian; Bable; Leonese; Asturleonese|ISO
ast|Asturian|ISO
ast|Bable|ISO
ast|Leonese|ISO
ast|Asturleonese|ISO
ast|ast|ISO
ath|Athapascan languages|ISO
ath|Athapascan|ISO
ath|ath|ISO
aus|Australian languages|ISO
aus|Australian|ISO
aus|aus|ISO
map|Austronesian (Other)|ISO
map|Austronesian|ISO
map|map|ISO
ava|Avaric|ISO
ava|ava|ISO
ave|Avestan|ISO
ave|ave|ISO
awa|Awadhi|ISO
awa|awa|ISO
aym|Aymara|ISO
aym|aym|ISO
aze|Azerbaijani|ISO
aze|aze|ISO
ban|Balinese|ISO
ban|ban|ISO
bat|Baltic (Other)|ISO
bat|Baltic|ISO
bat|bat|ISO
bal|Baluchi|ISO
bal|bal|ISO
bam|Bambara|ISO
bam|bam|ISO
bai|Bamileke languages|ISO
bai|Bamileke|ISO
bai|bai|ISO
bad|Banda languages|ISO
bad|Banda|ISO
bad|bad|ISO
bnt|Bantu (Other)|ISO
bnt|Bantu|ISO
bnt|bnt|ISO
bas|Basa|ISO
bas|bas|ISO
bak|Bashkir|ISO
bak|bak|ISO
baq|Basque|ISO
baq|baq|ISO
btk|Batak languages|ISO
btk|Batak|ISO
btk|btk|ISO
bej|Beja; Bedawiyet|ISO
bej|Beja|ISO
bej|Bedawiyet|ISO
bej|bej|ISO
bel|Belarusian|ISO
bel|bel|ISO
bem|Bemba|ISO
bem|bem|ISO
ben|Bengali|ISO
ben|ben|ISO
ber|Berber (Other)|ISO
ber|Berber|ISO
ber|ber|ISO
bho|Bhojpuri|ISO
bho|bho|ISO
bih|Bihari|ISO
bih|bih|ISO
bik|Bikol|ISO
bik|bik|ISO
bin|Bini; Edo|ISO
bin|Bini|ISO
bin|Edo|ISO
bin|bin|ISO
bis|Bislama|ISO
bis|bis|ISO
byn|Blin; Bilin|ISO
byn|Blin|ISO
byn|Bilin|ISO
byn|byn|ISO
zbl|Blissymbols; Blissymbolics; Bliss|ISO
zbl|Blissymbols|ISO
zbl|Blissymbolics|ISO
zbl|Bliss|ISO
zbl|zbl|ISO
nob|Norwegian Bokmål|ISO
nob|nob|ISO
bos|Bosnian|ISO
bos|bos|ISO
bra|Braj|ISO
bra|bra|ISO
bre|Breton|ISO
bre|bre|ISO
bug|Buginese|ISO
bug|bug|ISO
bul|Bulgarian|ISO
bul|bul|ISO
bua|Buriat|ISO
bua|bua|ISO
bur|Burmese|ISO
bur|bur|ISO
cad|Caddo|ISO
cad|cad|ISO
yue|Cantonese|ISO
yue|Cantonese; Chinese|ISO
yue|yue|ISO
cat|Catalan; Valencian|ISO
cat|Catalan|ISO
cat|Valencian|ISO
cat|cat|ISO
cau|Caucasian (Other)|ISO
cau|Caucasian|ISO
cau|cau|ISO
ceb|Cebuano|ISO
ceb|ceb|ISO
cel|Celtic (Other)|ISO
cel|Celtic|ISO
cel|cel|ISO
cai|Central American Indian (Other)|ISO
cai|Central American Indian|ISO
cai|cai|ISO
khm|Central Khmer|ISO
khm|khm|ISO
chg|Chagatai|ISO
chg|chg|ISO
cmc|Chamic languages|ISO
cmc|Chamic|ISO
cmc|cmc|ISO
cha|Chamorro|ISO
cha|cha|ISO
che|Chechen|ISO
che|che|ISO
chr|Cherokee|ISO
chr|chr|ISO
chy|Cheyenne|ISO
chy|chy|ISO
chb|Chibcha|ISO
chb|chb|ISO
nya|Chichewa; Chewa; Nyanja|ISO
nya|Chichewa|ISO
nya|Chewa|ISO
nya|Nyanja|ISO
nya|nya|ISO
chi|Chinese|ISO
chi|chi|ISO
chn|Chinook jargon|ISO
chn|Chinook|ISO
chn|chn|ISO
chp|Chipewyan; Dene Suline|ISO
chp|Chipewyan|ISO
chp|Dene Suline|ISO
chp|chp|ISO
cho|Choctaw|ISO
cho|cho|ISO
chk|Chuukese|ISO
chk|chk|ISO
chv|Chuvash|ISO
chv|chv|ISO
nwc|Classical Newari; Old Newari; Classical Nepal Bhasa|ISO
nwc|Classical Newari|ISO
nwc|Old Newari|ISO
nwc|Classical Nepal Bhasa|ISO
nwc|nwc|ISO
syc|Classical Syriac|ISO
syc|syc|ISO
cop|Coptic|ISO
cop|cop|ISO
cor|Cornish|ISO
cor|cor|ISO
cos|Corsican|ISO
cos|cos|ISO
cre|Cree|ISO
cre|cre|ISO
mus|Creek|ISO
mus|mus|ISO
crp|Creoles and pidgins (Other)|ISO
crp|crp|ISO
cpe|Creoles and pidgins English based (Other)|ISO
cpe|cpe|ISO
cpf|Creoles and pidgins French-based (Other)|ISO
cpf|cpf|ISO
cpp|Creoles and pidgins Portuguese-based (Other)|ISO
cpp|cpp|ISO
crh|Crimean Tatar; Crimean Turkish|ISO
crh|Crimean Tatar|ISO
crh|Crimean Turkish|ISO
crh|crh|ISO
hrv|Croatian|ISO
hrv|hrv|ISO
cus|Cushitic (Other)|ISO
cus|Cushitic|ISO
cus|cus|ISO
cze|Czech|ISO
cze|cze|ISO
dak|Dakota|ISO
dak|dak|ISO
dan|Danish|ISO
dan|dan|ISO
dar|Dargwa|ISO
dar|dar|ISO
prs|Dari|ISO
prs|prs|ISO
del|Delaware|ISO
del|del|ISO
din|Dinka|ISO
din|din|ISO
div|Divehi; Dhivehi; Maldivian|ISO
div|Divehi|ISO
div|Dhivehi|ISO
div|Maldivian|ISO
div|div|ISO
doi|Dogri|ISO
doi|doi|ISO
dgr|Dogrib|ISO
dgr|dgr|ISO
dra|Dravidian (Other)|ISO
dra|Dravidian|ISO
dra|dra|ISO
dua|Duala|ISO
dua|dua|ISO
dut|Dutch; Flemish|ISO
dut|Dutch|ISO
dut|Flemish|ISO
dut|dut|ISO
dyu|Dyula|ISO
dyu|dyu|ISO
dzo|Dzongkha|ISO
dzo|dzo|ISO
frs|Eastern Frisian|ISO
frs|frs|ISO
efi|Efik|ISO
efi|efi|ISO
eka|Ekajuk|ISO
eka|eka|ISO
elx|Elamite|ISO
elx|elx|ISO
eng|English|ISO
eng|eng|ISO
myv|Erzya|ISO
myv|myv|ISO
epo|Esperanto|ISO
epo|epo|ISO
est|Estonian|ISO
est|est|ISO
ewe|Ewe|ISO
ewo|Ewondo|ISO
ewo|ewo|ISO
fan|Fang|ISO
fan|fan|ISO
fat|Fanti|ISO
fat|fat|ISO
fao|Faroese|ISO
fao|fao|ISO
fij|Fijian|ISO
fij|fij|ISO
fil|Filipino; Pilipino|ISO
fil|Filipino|ISO
fil|Pilipino|ISO
fil|fil|ISO
fin|Finnish|ISO
fin|fin|ISO
fiu|Finno-Ugrian (Other)|ISO
fiu|Finno-Ugrian|ISO
fiu|fiu|ISO
fon|Fon|ISO
fre|French|ISO
fre|fre|ISO
fry|Frisian|ISO
fry|fry|ISO
fur|Friulian|ISO
fur|fur|ISO
ful|Fulah|ISO
ful|ful|ISO
gaa|Ga|ISO
gaa|gaa|ISO
gla|Gaelic; Scottish Gaelic|ISO
gla|Gaelic|ISO
gla|Scottish Gaelic|ISO
gla|gla|ISO
car|Galibi Carib|ISO
car|car|ISO
glg|Galician|ISO
glg|glg|ISO
lug|Ganda|ISO
lug|lug|ISO
gay|Gayo|ISO
gay|gay|ISO
gba|Gbaya|ISO
gba|gba|ISO
gez|Geez|ISO
gez|gez|ISO
geo|Georgian|ISO
geo|geo|ISO
ger|German|ISO
ger|ger|ISO
gem|Germanic (Other)|ISO
gem|Germanic|ISO
gem|gem|ISO
gil|Gilbertese|ISO
gil|gil|ISO
gon|Gondi|ISO
gon|gon|ISO
gor|Gorontalo|ISO
gor|gor|ISO
got|Gothic|ISO
got|got|ISO
grb|Grebo|ISO
grb|grb|ISO
gre|Greek Modern (1453-)|ISO
gre|Greek|ISO
gre|gre|ISO
grn|Guarani|ISO
grn|grn|ISO
guj|Gujarati|ISO
guj|guj|ISO
gwi|Gwich'in|ISO
gwi|gwi|ISO
hai|Haida|ISO
hai|hai|ISO
hat|Haitian; Haitian Creole|ISO
hat|Haitian|ISO
hat|Haitian Creole|ISO
hat|hat|ISO
hau|Hausa|ISO
hau|hau|ISO
haw|Hawaiian|ISO
haw|haw|ISO
heb|Hebrew|ISO
heb|heb|ISO
her|Herero|ISO
her|her|ISO
hil|Hiligaynon|ISO
hil|hil|ISO
him|Himachali|ISO
him|him|ISO
hin|Hindi|ISO
hin|hin|ISO
hmo|Hiri Motu|ISO
hmo|hmo|ISO
hit|Hittite|ISO
hit|hit|ISO
hmn|Hmong|ISO
hmn|hmn|ISO
hun|Hungarian|ISO
hun|hun|ISO
hup|Hupa|ISO
hup|hup|ISO
iba|Iban|ISO
iba|iba|ISO
ice|Icelandic|ISO
ice|ice|ISO
ido|Ido|ISO
ibo|Igbo|ISO
ibo|ibo|ISO
ijo|Ijo languages|ISO
ijo|Ijo|ISO
ilo|Iloko|ISO
ilo|ilo|ISO
smn|Inari Sami|ISO
smn|smn|ISO
inc|Indic (Other)|ISO
inc|Indic|ISO
inc|inc|ISO
ine|Indo-European (Other)|ISO
ine|Indo-European|ISO
ine|ine|ISO
ind|Indonesian|ISO
ind|ind|ISO
inh|Ingush|ISO
inh|inh|ISO
ikt|Inuinnaqtun|ISO
ikt|ikt|ISO
ina|Interlingua (International Auxiliary Language Association)|ISO
ina|Interlingua|ISO
ina|ina|ISO
ile|Interlingue; Occidental|ISO
ile|Interlingue|ISO
ile|Occidental|ISO
ile|ile|ISO
iku|Inuktitut|ISO
iku|iku|ISO
ipk|Inupiaq|ISO
ipk|ipk|ISO
ira|Iranian (Other)|ISO
ira|Iranian|ISO
ira|ira|ISO
gle|Irish|ISO
gle|gle|ISO
iro|Iroquoian languages|ISO
iro|Iroquoian|ISO
iro|iro|ISO
ita|Italian|ISO
ita|ita|ISO
jpn|Japanese|ISO
jpn|jpn|ISO
jav|Javanese|ISO
jav|jav|ISO
cjy|Jinyu Chinese|ISO
cjy|cjy|ISO
jrb|Judeo-Arabic|ISO
jrb|jrb|ISO
jpr|Judeo-Persian|ISO
jpr|jpr|ISO
kbd|Kabardian|ISO
kbd|kbd|ISO
kab|Kabyle|ISO
kab|kab|ISO
kac|Kachin; Jingpho|ISO
kac|Kachin|ISO
kac|Jingpho|ISO
kal|Kalaallisut; Greenlandic|ISO
kal|Kalaallisut|ISO
kal|Greenlandic|ISO
kal|kal|ISO
xal|Kalmyk; Oirat|ISO
xal|Kalmyk|ISO
xal|Oirat|ISO
xal|xal|ISO
kam|Kamba|ISO
kam|kam|ISO
kan|Kannada|ISO
kan|kan|ISO
kau|Kanuri|ISO
kau|kau|ISO
krc|Karachay-Balkar|ISO
krc|krc|ISO
kaa|Kara-Kalpak|ISO
kaa|kaa|ISO
krl|Karelian|ISO
krl|krl|ISO
kar|Karen languages|ISO
kar|Karen|ISO
kar|kar|ISO
kas|Kashmiri|ISO
kas|kas|ISO
csb|Kashubian|ISO
csb|csb|ISO
kaw|Kawi|ISO
kaw|kaw|ISO
kaz|Kazakh|ISO
kaz|kaz|ISO
kha|Khasi|ISO
kha|kha|ISO
khi|Khoisan (Other)|ISO
khi|Khoisan|ISO
khi|khi|ISO
khm|Khmer|ISO
khm|khm|ISO
kho|Khotanese|ISO
kho|kho|ISO
kik|Kikuyu; Gikuyu|ISO
kik|Kikuyu|ISO
kik|Gikuyu|ISO
kik|kik|ISO
kmb|Kimbundu|ISO
kin|Kinyarwanda|ISO
kin|kin|ISO
kir|Kirghiz; Kyrgyz|ISO
kir|Kyrgyz|ISO
kir|Kirghiz|ISO
kir|kir|ISO
tlh|Klingon; tlhIngan-Hol|ISO
tlh|Klingon|ISO
tlh|tlhIngan-Hol|ISO
tlh|tlh|ISO
kom|Komi|ISO
kom|kom|ISO
kon|Kongo|ISO
kon|kon|ISO
kok|Konkani|ISO
kok|kok|ISO
kor|Korean|ISO
kor|kor|ISO
kos|Kosraean|ISO
kos|kos|ISO
kpe|Kpelle|ISO
kpe|kpe|ISO
kro|Kru languages|ISO
kro|kro|ISO
kua|Kuanyama; Kwanyama|ISO
kua|Kuanyama|ISO
kua|Kwanyama|ISO
kua|kua|ISO
kum|Kumyk|ISO
kum|kum|ISO
kur|Kurdish|ISO
kur|kur|ISO
kru|Kurukh|ISO
kru|kru|ISO
kut|Kutenai|ISO
kut|kut|ISO
lad|Ladino|ISO
lad|lad|ISO
lah|Lahnda|ISO
lah|lah|ISO
lam|Lamba|ISO
lam|lam|ISO
day|Land Dayak languages|ISO
day|Land Dayak|ISO
day|day|ISO
lao|Lao|ISO
lat|Latin|ISO
lat|lat|ISO
lav|Latvian|ISO
lav|lav|ISO
lez|Lezghian|ISO
lez|lez|ISO
lim|Limburgan; Limburger; Limburgish|ISO
lim|Limburgan|ISO
lim|Limburger|ISO
lim|Limburgish|ISO
lim|lim|ISO
lin|Lingala|ISO
lin|lin|ISO
lit|Lithuanian|ISO
lit|lit|ISO
jbo|Lojban|ISO
jbo|jbo|ISO
nds|Low German; Low Saxon; German|ISO
nds|Low German|ISO
nds|Low Saxon|ISO
nds|nds|ISO
dsb|Lower Sorbian|ISO
dsb|dsb|ISO
loz|Lozi|ISO
loz|loz|ISO
lub|Luba-Katanga|ISO
lub|lub|ISO
lua|Luba-Lulua|ISO
lua|lua|ISO
lui|Luiseno|ISO
lui|lui|ISO
smj|Lule Sami|ISO
smj|smj|ISO
lun|Lunda|ISO
lun|lun|ISO
luo|Luo (Kenya and Tanzania)|ISO
luo|luo|ISO
lus|Lushai|ISO
lus|lus|ISO
ltz|Luxembourgish; Letzeburgesch|ISO
ltz|Luxembourgish|ISO
ltz|Letzeburgesch|ISO
ltz|ltz|ISO
mac|Macedonian|ISO
mac|mac|ISO
mad|Madurese|ISO
mad|mad|ISO
mag|Magahi|ISO
mag|mag|ISO
mai|Maithili|ISO
mai|mai|ISO
mak|Makasar|ISO
mak|mak|ISO
mlg|Malagasy|ISO
mlg|mlg|ISO
may|Malay|ISO
may|may|ISO
mal|Malayalam|ISO
mal|mal|ISO
mlt|Maltese|ISO
mlt|mlt|ISO
cmn|Mandarin|ISO
cmn|Mandarin Chinese|ISO
cmn|cmn|ISO
mnc|Manchu|ISO
mnc|mnc|ISO
mdr|Mandar|ISO
mdr|mdr|ISO
man|Mandingo|ISO
man|man|ISO
mni|Manipuri|ISO
mni|mni|ISO
mno|Manobo languages|ISO
mno|Manobo|ISO
mno|mno|ISO
glv|Manx|ISO
glv|glv|ISO
mao|Maori|ISO
mao|mao|ISO
arn|Mapudungun; Mapuche|ISO
arn|Mapudungun|ISO
arn|Mapuche|ISO
arn|arn|ISO
mar|Marathi|ISO
mar|mar|ISO
chm|Mari|ISO
chm|chm|ISO
mah|Marshallese|ISO
mah|mah|ISO
mwr|Marwari|ISO
mwr|mwr|ISO
mas|Masai|ISO
mas|mas|ISO
myn|Mayan languages|ISO
myn|Mayan|ISO
myn|myn|ISO
men|Mende|ISO
men|men|ISO
mic|Mi'kmaq; Micmac|ISO
mic|Mi'kmaq|ISO
mic|Micmac|ISO
mic|mic|ISO
min|Minangkabau|ISO
min|min|ISO
mwl|Mirandese|ISO
mwl|mwl|ISO
moh|Mohawk|ISO
moh|moh|ISO
mdf|Moksha|ISO
mdf|mdf|ISO
mol|Moldavian; Moldovan|ISO
mol|Moldovan|ISO
mol|Moldavian|ISO
mol|mol|ISO
lol|Mongo|ISO
lol|lol|ISO
mon|Mongolian|ISO
mon|mon|ISO
mkh|Mon-Khmer (Other)|ISO
mkh|Mon-Khmer|ISO
mkh|mkh|ISO
cnr|Montenegrin|ISO
cnr|cnr|ISO
mos|Mossi|ISO
mos|mos|ISO
mul|Multiple languages|ISO
mun|Munda languages|ISO
mun|Munda|ISO
mun|mun|ISO
nah|Nahuatl languages|ISO
nah|Nahuatl|ISO
nah|nah|ISO
nau|Nauru|ISO
nau|nau|ISO
nav|Navajo; Navaho|ISO
nav|Navajo|ISO
nav|Navaho|ISO
nav|nav|ISO
nde|Ndebele|ISO
nde|nde|ISO
nde|Ndebele North; North Ndebele|ISO
nde|Ndebele North|ISO
nde|North Ndebele|ISO
nde|nde|ISO
nbl|Ndebele South; South Ndebele|ISO
nbl|Ndebele South|ISO
nbl|South Ndebele|ISO
nbl|nbl|ISO
ndo|Ndonga|ISO
ndo|ndo|ISO
nap|Neapolitan|ISO
nap|nap|ISO
new|Nepal Bhasa; Newari|ISO
new|Nepal Bhasa|ISO
new|Newari|ISO
new|new|ISO
nep|Nepali|ISO
nep|nep|ISO
nia|Nias|ISO
nia|nia|ISO
nic|Niger-Kordofanian (Other)|ISO
nic|Niger-Kordofanian|ISO
nic|nic|ISO
ssa|Nilo-Saharan (Other)|ISO
ssa|Nilo-Saharan|ISO
ssa|ssa|ISO
niu|Niuean|ISO
niu|niu|ISO
nqo|N'Ko|ISO
nqo|nqo|ISO
zxx|No linguistic content; Not applicable|ISO
zxx|No linguistic content|ISO
zxx|zxx|ISO
nog|Nogai|ISO
nog|nog|ISO
nai|North American Indian|ISO
nai|nai|ISO
frr|Northern Frisian|ISO
frr|frr|ISO
sme|Northern Sami|ISO
sme|sme|ISO
nor|Norwegian|ISO
nor|nor|ISO
nno|Norwegian Nynorsk; Nynorsk Norwegian|ISO
nno|Norwegian Nynorsk|ISO
nno|Nynorsk Norwegian|ISO
nno|Nynorsk|ISO
nno|nno|ISO
nub|Nubian languages|ISO
nub|Nubian|ISO
nub|nub|ISO
nym|Nyamwezi|ISO
nym|nym|ISO
nyn|Nyankole|ISO
nyn|nyn|ISO
nyo|Nyoro|ISO
nyo|nyo|ISO
nzi|Nzima|ISO
nzi|nzi|ISO
oci|Occitan (post 1500); Provençal|ISO
oci|Occitan (post 1500)|ISO
oci|Occitan|ISO
oci|Provençal|ISO
oci|oci|ISO
oji|Ojibwa|ISO
oji|oji|ISO
ori|Oriya|ISO
ori|ori|ISO
orm|Oromo|ISO
orm|orm|ISO
osa|Osage|ISO
osa|osa|ISO
oss|Ossetian; Ossetic|ISO
oss|Ossetian|ISO
oss|Ossetic|ISO
oss|oss|ISO
oto|Otomian languages|ISO
oto|Otomian|ISO
oto|oto|ISO
pal|Pahlavi|ISO
pal|pal|ISO
pau|Palauan|ISO
pau|pau|ISO
pli|Pali|ISO
pli|pli|ISO
pam|Pampanga; Kapampangan|ISO
pam|Pampanga|ISO
pam|Kapampangan|ISO
pam|pam|ISO
pag|Pangasinan|ISO
pag|pag|ISO
pan|Panjabi|ISO
pan|pan|ISO
pap|Papiamento|ISO
pap|pap|ISO
paa|Papuan (Other)|ISO
paa|Papuan|ISO
paa|paa|ISO
pus|Pushto; Pashto|ISO
pus|Pashto|ISO
pus|Pushto|ISO
pus|pus|ISO
pst|Central Pashto|ISO
pst|pst|ISO
pbt|Southern Pashto|ISO
pbt|pbt|ISO
nso|Pedi; Sepedi; Northern Sotho|ISO
nso|Pedi|ISO
nso|Sepedi|ISO
nso|Northern Sotho|ISO
nso|nso|ISO
per|Persian|ISO
per|per|ISO
phi|Philippine (Other)|ISO
phi|Philippine|ISO
phi|phi|ISO
phn|Phoenician|ISO
phn|phn|ISO
pon|Pohnpeian|ISO
pon|pon|ISO
pol|Polish|ISO
pol|pol|ISO
por|Portuguese|ISO
por|por|ISO
pra|Prakrit languages|ISO
pra|Prakrit|ISO
pra|pra|ISO
pan|Punjabi|ISO
pan|pan|ISO
que|Quechua|ISO
que|que|ISO
raj|Rajasthani|ISO
raj|raj|ISO
rap|Rapanui|ISO
rap|rap|ISO
rar|Rarotongan; Cook Islands Maori|ISO
rar|Rarotongan|ISO
rar|Cook Islands Maori|ISO
rar|rar|ISO
qaa-qtz|Reserved for local use|ISO
qaa-qtz|qaa-qtz|ISO
roa|Romance (Other)|ISO
roa|Romance|ISO
roa|roa|ISO
rum|Romanian|ISO
rum|rum|ISO
roh|Romansh|ISO
roh|roh|ISO
rom|Romany|ISO
rom|rom|ISO
run|Rundi|ISO
run|run|ISO
rus|Russian|ISO
rus|rus|ISO
sal|Salishan languages|ISO
sal|Salishan|ISO
sal|sal|ISO
sam|Samaritan Aramaic|ISO
sam|sam|ISO
smi|Sami languages (Other)|ISO
smi|Sami (Other)|ISO
smi|Sami|ISO
smi|smi|ISO
smo|Samoan|ISO
smo|smo|ISO
sad|Sandawe|ISO
sad|sad|ISO
sag|Sango|ISO
sag|sag|ISO
san|Sanskrit|ISO
san|san|ISO
sat|Santali|ISO
sat|sat|ISO
srd|Sardinian|ISO
srd|srd|ISO
sas|Sasak|ISO
sas|sas|ISO
sco|Scots|ISO
sco|sco|ISO
sel|Selkup|ISO
sel|sel|ISO
sem|Semitic (Other)|ISO
sem|Semitic|ISO
sem|sem|ISO
srp|Serbian|ISO
srp|srp|ISO
srr|Serer|ISO
srr|srr|ISO
shn|Shan|ISO
shn|shn|ISO
sna|Shona|ISO
sna|sna|ISO
iii|Sichuan Yi; Nuosu|ISO
iii|Sichuan Yi|ISO
iii|Nuosu|ISO
iii|iii|ISO
scn|Sicilian|ISO
scn|scn|ISO
sid|Sidamo|ISO
sid|sid|ISO
sgn|Sign Languages|ISO
sgn|sgn|ISO
bla|Siksika|ISO
bla|bla|ISO
snd|Sindhi|ISO
snd|snd|ISO
sin|Sinhala; Sinhalese|ISO
sin|Sinhala|ISO
sin|Sinhalese|ISO
sin|sin|ISO
sit|Sino-Tibetan (Other)|ISO
sit|Sino-Tibetan|ISO
sit|sit|ISO
sio|Siouan languages|ISO
sio|Siouan|ISO
sio|sio|ISO
sms|Skolt Sami|ISO
sms|sms|ISO
den|Slave (Athapascan)|ISO
den|den|ISO
sla|Slavic (Other)|ISO
sla|Slavic|ISO
sla|sla|ISO
slo|Slovak|ISO
slo|slo|ISO
slv|Slovenian|ISO
slv|Slovene|ISO
slv|slv|ISO
sog|Sogdian|ISO
sog|sog|ISO
som|Somali|ISO
som|som|ISO
son|Songhai languages|ISO
son|Songhai|ISO
son|son|ISO
snk|Soninke|ISO
snk|snk|ISO
wen|Sorbian languages|ISO
wen|Sorbian|ISO
wen|wen|ISO
sot|Sotho Southern|ISO
sot|sot|ISO
sai|South American Indian (Other)|ISO
sai|South American Indian|ISO
sai|sai|ISO
alt|Southern Altai|ISO
alt|alt|ISO
sma|Southern Sami|ISO
sma|sma|ISO
spa|Spanish; Castilian|ISO
spa|Spanish|ISO
spa|Castilian|ISO
spa|spa|ISO
srn|Sranan Tongo|ISO
srn|srn|ISO
suk|Sukuma|ISO
suk|suk|ISO
sux|Sumerian|ISO
sux|sux|ISO
sun|Sundanese|ISO
sun|sun|ISO
sus|Susu|ISO
sus|sus|ISO
swa|Swahili|ISO
swa|swa|ISO
ssw|Swati|ISO
ssw|ssw|ISO
swe|Swedish|ISO
swe|swe|ISO
gsw|Swiss German|ISO
gsw|Alemannic|ISO
gsw|Alsatian|ISO
gsw|gsw|ISO
syr|Syriac|ISO
syr|syr|ISO
tgl|Tagalog|ISO
tgl|tgl|ISO
tah|Tahitian|ISO
tah|tah|ISO
tai|Tai (Other)|ISO
tai|tai|ISO
tgk|Tajik|ISO
tgk|tgk|ISO
tmh|Tamashek|ISO
tmh|tmh|ISO
tam|Tamil|ISO
tam|tam|ISO
tat|Tatar|ISO
tat|tat|ISO
tel|Telugu|ISO
tel|tel|ISO
ter|Tereno|ISO
ter|ter|ISO
tet|Tetum|ISO
tet|tet|ISO
tha|Thai|ISO
tha|tha|ISO
tib|Tibetan|ISO
tib|tib|ISO
tig|Tigre|ISO
tig|tig|ISO
tir|Tigrinya|ISO
tir|tir|ISO
tem|Timne|ISO
tem|tem|ISO
tiv|Tiv|ISO
tli|Tlingit|ISO
tli|tli|ISO
tpi|Tok Pisin|ISO
tpi|tpi|ISO
tkl|Tokelau|ISO
tkl|tkl|ISO
tog|Tonga (Nyasa)|ISO
tog|tog|ISO
ton|Tonga (Tonga Islands)|ISO
ton|ton|ISO
tsi|Tsimshian|ISO
tsi|tsi|ISO
tso|Tsonga|ISO
tso|tso|ISO
tsn|Tswana|ISO
tsn|tsn|ISO
tum|Tumbuka|ISO
tum|tum|ISO
tup|Tupi languages|ISO
tup|Tupi|ISO
tup|tup|ISO
tur|Turkish|ISO
tur|tur|ISO
tuk|Turkmen|ISO
tuk|tuk|ISO
tvl|Tuvalu|ISO
tvl|tvl|ISO
tyv|Tuvinian|ISO
tyv|tyv|ISO
twi|Twi|ISO
udm|Udmurt|ISO
udm|udm|ISO
uga|Ugaritic|ISO
uga|uga|ISO
uig|Uighur|ISO
uig|Uyghur|ISO
uig|uig|ISO
ukr|Ukrainian|ISO
ukr|ukr|ISO
umb|Umbundu|ISO
umb|umb|ISO
mis|Uncoded languages|ISO
mis|mis|ISO
und|Undetermined|ISO
und|und|ISO
hsb|Upper Sorbian|ISO
hsb|hsb|ISO
urd|Urdu|ISO
urd|urd|ISO
uzb|Uzbek|ISO
uzb|uzb|ISO
vai|Vai|ISO
ven|Venda|ISO
ven|ven|ISO
vie|Vietnamese|ISO
vie|vie|ISO
vol|Volapük|ISO
vol|vol|ISO
vot|Votic|ISO
vot|vot|ISO
wak|Wakashan languages|ISO
wak|Wakashan|ISO
wak|wak|ISO
wal|Walamo|ISO
wal|wal|ISO
wln|Walloon|ISO
wln|wln|ISO
war|Waray|ISO
war|war|ISO
was|Washo|ISO
was|was|ISO
wel|Welsh|ISO
wel|wel|ISO
fry|Western Frisian|ISO
fry|fry|ISO
wol|Wolof|ISO
wol|wol|ISO
xho|Xhosa|ISO
xho|xho|ISO
sah|Yakut|ISO
sah|sah|ISO
yao|Yao|ISO
yap|Yapese|ISO
yap|yap|ISO
yid|Yiddish|ISO
yid|yid|ISO
yor|Yoruba|ISO
yor|yor|ISO
ypk|Yupik languages|ISO
ypk|Yupik|ISO
ypk|ypk|ISO
znd|Zande languages|ISO
znd|Zande|ISO
znd|znd|ISO
zap|Zapotec|ISO
zap|zap|ISO
zza|Zaza; Dimili; Dimli; Kirdki; Kirmanjki; Zazaki|ISO
zza|Zaza|ISO
zza|Dimili|ISO
zza|Dimli|ISO
zza|Kirdki|ISO
zza|Kirmanjki|ISO
zza|Zazaki|ISO
zza|zza|ISO
zen|Zenaga|ISO
zen|zen|ISO
zha|Zhuang; Chuang|ISO
zha|Chuang|ISO
zha|zha|ISO
zul|Zulu|ISO
zul|zul|ISO
zun|Zuni|ISO
zun|zun|ISO

**Documentation**:
Translate multiple inbound Language values to the ISO-639 codes
---

**Name**: patient_race

**ReportStream Internal Name**: patient_race

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [1..1]

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

**Name**: patient_state

**ReportStream Internal Name**: patient_state_fromCSV

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: patient_state

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

**Name**: patient_street

**ReportStream Internal Name**: patient_street

**Type**: STREET

**PII**: Yes

**Cardinality**: [1..1]

**Documentation**:
The patient's street address
---

**Name**: patient_street2

**ReportStream Internal Name**: patient_street2

**Type**: STREET_OR_BLANK

**PII**: Yes

**Cardinality**: [0..1]

**Documentation**:
The patient's second address line
---

**Name**: patient_zip_code

**ReportStream Internal Name**: patient_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
The patient's zip code
---

**Name**: pregnant

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

**Cardinality**: [1..1]

**Documentation**:
The reporting facility's CLIA
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

**Cardinality**: [1..1]

**Documentation**:
The reporting facility's name
---

**Name**: residence_type

**ReportStream Internal Name**: residence_type

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 75617-1

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
22232009|Hospital|SNOMED_CT
22232009|22232009|SNOMED_CT
2081004|Hospital Ship|SNOMED_CT
2081004|2081004|SNOMED_CT
32074000|Long Term Care Hospital|SNOMED_CT
32074000|32074000|SNOMED_CT
224929004|Secure Hospital|SNOMED_CT
224929004|224929004|SNOMED_CT
42665001|Nursing Home|SNOMED_CT
42665001|42665001|SNOMED_CT
30629002|Retirement Home|SNOMED_CT
30629002|30629002|SNOMED_CT
74056004|Orphanage|SNOMED_CT
74056004|74056004|SNOMED_CT
722173008|Prison-based Care Site|SNOMED_CT
722173008|Prison Based Care Site|SNOMED_CT
722173008|722173008|SNOMED_CT
20078004|Substance Abuse Treatment Center|SNOMED_CT
20078004|20078004|SNOMED_CT
257573002|Boarding House|SNOMED_CT
257573002|257573002|SNOMED_CT
224683003|Military Accommodation|SNOMED_CT
224683003|224683003|SNOMED_CT
284546000|Hospice|SNOMED_CT
284546000|284546000|SNOMED_CT
257628001|Hostel|SNOMED_CT
257628001|257628001|SNOMED_CT
310207003|Sheltered Housing|SNOMED_CT
310207003|310207003|SNOMED_CT
57656006|Penal Institution|SNOMED_CT
57656006|Prison|SNOMED_CT
57656006|Correctional Facility|SNOMED_CT
57656006|Jail|SNOMED_CT
57656006|County Jail|SNOMED_CT
57656006|City Jail|SNOMED_CT
57656006|57656006|SNOMED_CT
285113009|Religious Institutional Residence|SNOMED_CT
285113009|285113009|SNOMED_CT
285141008|Work (environment)|SNOMED_CT
285141008|Work|SNOMED_CT
285141008|285141008|SNOMED_CT
32911000|Homeless|SNOMED_CT

**Documentation**:
Translate multiple inbound values into Residence Type SNOMED codes.
---

**Name**: resident_congregate_setting

**ReportStream Internal Name**: resident_congregate_setting

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: 

**LOINC Code**: 95421-4

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

**Name**: specimen_collection_date

**ReportStream Internal Name**: specimen_collection_date_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: specimen_collection_date

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

**Name**: specimen_type

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

**Name**: specimen_type

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

**Name**: symptomatic_for_disease

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

**Name**: test_kit_name_id

**ReportStream Internal Name**: test_kit_name_id

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]


**Reference URL**:
[https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification](https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification) 
                
**Table**: LIVD-SARS-CoV-2

**Table Column**: Testkit Name ID

**Documentation**:
Follows guidence for OBX-17 as defined in the HL7 Confluence page
---

**Name**: test_performed_code

**ReportStream Internal Name**: test_performed_code

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Performed LOINC Code

**Documentation**:
The LOINC code of the test performed. This is a standardized coded value describing the test
---

**Name**: test_performed_name

**ReportStream Internal Name**: test_performed_name

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Performed LOINC Long Name

**Documentation**:
The LOINC description of the test performed as related to the LOINC code.
---

**Name**: test_result

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

**Name**: test_result_date

**ReportStream Internal Name**: test_result_date

**Type**: DATETIME

**PII**: No

**Cardinality**: [1..1]

---

**Name**: test_result_status

**ReportStream Internal Name**: test_result_status

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Default Value**: F

**Cardinality**: [1..1]

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
F|Final results; Can only be changed with a corrected result|HL7
F|Final results; Can only be changed with a corrected result|HL7
C|Record coming over is a correction and thus replaces a final result|HL7
C|Record coming over is a correction and thus replaces a final result|HL7

**Alt Value Sets**

Code | Display | System
---- | ------- | ------
F|F|HL7
F|f|HL7
C|C|HL7
C|c|HL7

**Documentation**:
The test result status, which is different from the test result itself. Per the valueset, this indicates if
the test result is in some intermediate status, is a correction, or is the final result.

---

**Name**: testing_lab_city

**ReportStream Internal Name**: testing_lab_city

**Type**: CITY

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
The city of the testing lab
---

**Name**: testing_lab_clia

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

**Name**: testing_lab_clia

**ReportStream Internal Name**: testing_lab_id

**Type**: ID

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Typically this will be the same as the `testing_lab_clia`, but potentially could not be.
---

**Name**: testing_lab_name

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
The name of the laboratory which performed the test, can be the same as the sending facility name
---

**Name**: testing_lab_phone_number

**ReportStream Internal Name**: testing_lab_phone_number

**Type**: TELEPHONE

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
The phone number of the testing lab
---

**Name**: testing_lab_specimen_received_date

**ReportStream Internal Name**: testing_lab_specimen_received_date_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: testing_lab_specimen_received_date

**ReportStream Internal Name**: testing_lab_specimen_received_datetime

**Type**: DATETIME

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
The received date time for the specimen. This field is very important to many states for their HL7,
but for most of our senders, the received date time is the same as the collected date time. Unfortunately,
setting them to the same time breaks many validation rules. Most ELR systems apparently look for them to
be offset, so this field takes the `specimen_collection_date_time` field and offsets it by a small amount.

---

**Name**: testing_lab_state

**ReportStream Internal Name**: testing_lab_state_fromCSV

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: testing_lab_state

**ReportStream Internal Name**: testing_lab_state_fromValueSet

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

**Name**: testing_lab_street

**ReportStream Internal Name**: testing_lab_street

**Type**: STREET

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
The street address for the testing lab
---

**Name**: testing_lab_street2

**ReportStream Internal Name**: testing_lab_street2

**Type**: STREET_OR_BLANK

**PII**: No

**Cardinality**: [0..1]

**Documentation**:
Street 2 field for the testing lab
---

**Name**: testing_lab_zip_code

**ReportStream Internal Name**: testing_lab_zip_code

**Type**: POSTAL_CODE

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
The postal code for the testing lab
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

**Name**: message_id

**ReportStream Internal Name**: message_id

**Type**: ID

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
unique id to track the usage of the message
---

**Name**: ordering_facility_state

**ReportStream Internal Name**: ordering_facility_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:
The state of the facility which the test was ordered from
---

**Name**: ordering_facility_state_temp

**ReportStream Internal Name**: ordering_facility_state_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---

**Name**: ordering_provider_state

**ReportStream Internal Name**: ordering_provider_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

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

**Name**: patient_id_temp

**ReportStream Internal Name**: patient_id_temp

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

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

**Name**: sender_id

**ReportStream Internal Name**: sender_id

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
ID name of org that is sending this data to ReportStream.  Suitable for provenance or chain of custody tracking.  Not to be confused with sending_application, in which ReportStream acts as the 'sender' to the downstream jurisdiction.
---

**Name**: test_type

**ReportStream Internal Name**: test_type

**Type**: TABLE

**PII**: No

**Cardinality**: [0..1]

**Table**: LIVD-SARS-CoV-2

**Table Column**: Test Type

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

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:
The state for the testing lab
---
