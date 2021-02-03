
### Schema:         nd/nd-covid-19
#### Description:   ND COVID-19

---

**Name**: sending_application

**Type**: HD

**HL7 Field**: MSH-3

**Cardinality**: [0..1]

---

**Name**: sending_facility

**Type**: HD

**HL7 Field**: MSH-4

**Cardinality**: [0..1]

---

**Name**: reporting_facility_name

**Type**: TEXT

**HL7 Field**: MSH-4-1

**Cardinality**: [0..1]

---

**Name**: reporting_facility_clia

**Type**: ID_CLIA

**HL7 Field**: MSH-4-2

**Cardinality**: [0..1]

---

**Name**: receiving_application

**Type**: HD

**HL7 Field**: MSH-5

**Cardinality**: [0..1]

---

**Name**: receiving_facility

**Type**: HD

**HL7 Field**: MSH-6

**Cardinality**: [0..1]

---

**Name**: patient_last_name

**Type**: PERSON_NAME

**HL7 Field**: PID-5-1

**Cardinality**: [1..1]

**Documentation**:

The patient's last name

---

**Name**: patient_first_name

**Type**: PERSON_NAME

**HL7 Field**: PID-5-2

**Cardinality**: [0..1]

**Documentation**:

The patient's first name

---

**Name**: patient_middle_name

**Type**: PERSON_NAME

**HL7 Field**: PID-5-3

**Cardinality**: [0..1]

---

**Name**: patient_dob

**Type**: DATE

**HL7 Field**: PID-7

**Cardinality**: [0..1]

**Documentation**:

The patient's date of birth. Default format is yyyyMMdd.

Other states may choose to define their own formats.


---

**Name**: patient_gender

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

**Name**: patient_race

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

**Name**: patient_city

**Type**: CITY

**HL7 Field**: PID-11-3

**Cardinality**: [0..1]

**Documentation**:

The patient's city

---

**Name**: patient_state

**Type**: TABLE

**HL7 Field**: PID-11-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The patient's state

---

**Name**: patient_zip_code

**Type**: POSTAL_CODE

**HL7 Field**: PID-11-5

**Cardinality**: [0..1]

**Documentation**:

The patient's zip code

---

**Name**: patient_phone_number

**Type**: TELEPHONE

**HL7 Field**: PID-13

**Cardinality**: [0..1]

**Documentation**:

The patient's phone number with area code

---

**Name**: ordering_facility_name

**Type**: TEXT

**HL7 Field**: ORC-21-1

**Cardinality**: [0..1]

**Documentation**:

The name of the facility which the test was ordered from

---

**Name**: ordering_facility_street

**Type**: STREET

**HL7 Field**: ORC-22-1

**Cardinality**: [0..1]

**Documentation**:

The address of the facility which the test was ordered from

---

**Name**: ordering_facility_street2

**Type**: STREET_OR_BLANK

**HL7 Field**: ORC-22-2

**Cardinality**: [0..1]

**Documentation**:

The secondary address of the facility which the test was ordered from

---

**Name**: ordering_facility_city

**Type**: CITY

**HL7 Field**: ORC-22-3

**Cardinality**: [0..1]

**Documentation**:

The city of the facility which the test was ordered from

---

**Name**: ordering_facility_state

**Type**: TABLE

**HL7 Field**: ORC-22-4

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---

**Name**: ordering_facility_zip_code

**Type**: POSTAL_CODE

**HL7 Field**: ORC-22-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the facility which the test was ordered from

---

**Name**: ordering_provider_street

**Type**: STREET

**HL7 Field**: ORC-24-1

**Cardinality**: [0..1]

**Documentation**:

The street address of the provider

---

**Name**: ordering_provider_street2

**Type**: STREET_OR_BLANK

**HL7 Field**: ORC-24-2

**Cardinality**: [0..1]

**Documentation**:

The street second address of the provider

---

**Name**: ordering_provider_city

**Type**: CITY

**HL7 Field**: ORC-24-3

**Cardinality**: [0..1]

**Documentation**:

The city of the provider

---

**Name**: ordering_provider_state

**Type**: TABLE

**HL7 Field**: ORC-24-4

**Cardinality**: [0..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the provider

---

**Name**: ordering_provider_zip_code

**Type**: POSTAL_CODE

**HL7 Field**: ORC-24-5

**Cardinality**: [0..1]

**Documentation**:

The zip code of the provider

---

**Name**: filler_order_id

**Type**: ID

**Cardinality**: [0..1]

---

**Name**: ordering_provider_first_name

**Type**: PERSON_NAME

**HL7 Field**: ORC-12-3

**Cardinality**: [0..1]

**Documentation**:

The first name of the provider who ordered the test

---

**Name**: ordering_provider_last_name

**Type**: PERSON_NAME

**Cardinality**: [0..1]

**Documentation**:

The last name of provider who ordered the test

---

**Name**: ordering_provider_id

**Type**: ID_NPI

**HL7 Field**: ORC-12-1

**Cardinality**: [0..1]

**Documentation**:

The ordering provider’s National Provider Identifier

---

**Name**: ordering_provider_id_authority

**Type**: HD

**HL7 Field**: ORC-12-9

**Cardinality**: [0..1]

---

**Name**: ordering_provider_id_authority_type

**Type**: TEXT

**HL7 Field**: ORC-12-13

**Cardinality**: [0..1]

---

**Name**: ordering_provider_middle_name

**Type**: PERSON_NAME

**HL7 Field**: ORC-12-4

**Cardinality**: [0..1]

---

**Name**: ordering_provider_middle_initial

**Type**: PERSON_NAME

**HL7 Field**: ORC-12-4

**Cardinality**: [0..1]

---

**Name**: test_result_status

**Type**: CODE

**HL7 Field**: OBX-11-1

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
A|Some, but not all, results available
C|Corrected, final
F|Final results
I|No results available; specimen received, procedure incomplete
M|Corrected, not final
N|Procedure completed, results pending
O|Order received; specimen not yet received
P|Preliminary
R|Results stored; not yet verified
S|No results available; procedure scheduled, but not done
X|No results available; Order canceled
Y|No order on record for this test
Z|No record of this patient

---

**Name**: value_type

**Type**: CODE

**HL7 Field**: OBX-2

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
AD|Address
CE|Coded Entry
CF|Coded Element With Formatted Values
CK|Composite ID With Check Digit
CN|Composite ID And Name
CP|Composite Price
CX|Extended Composite ID With Check Digit
DT|Date
ED|Encapsulated Data
FT|Formatted Text (Display)
MO|Money
NM|Numeric
PN|Person Name
RP|Reference Pointer
SN|Structured Numeric
ST|String Data
TM|Time
TN|Telephone Number
TS|Time Stamp (Date & Time)
TX|Text Data (Display)
XAD|Extended Address
XCN|Extended Composite Name and Number For Persons
XON|Extended Composite Name and Number For Organizations
XPN|Extended Person Name
XTN|Extended Telecommunications Number

---

**Name**: test_result_sub_id

**Type**: ID

**HL7 Field**: OBX-4

**Cardinality**: [0..1]

---

**Name**: test_result

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

**Name**: observation_result_status

**Type**: CODE

**HL7 Field**: OBX-11

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
C|Record coming over is a correction and thus replaces a final result
D|Deletes the OBX record
F|Final results; Can only be changed with a corrected result
I|Specimen in lab; results pending
N|Not asked; used to affirmatively document that the observation identified in the OBX was not sought when the universal service ID in OBR-4 implies that it would be sought.
O|Order detail description only (no result)
P|Preliminary results
R|Results entered -- not verified
S|Partial results
U|Results status change to final without retransmitting results already sent as ‘preliminary.’  E.g., radiology changes status from preliminary to final
W|Post original as wrong, e.g., transmitted for wrong patient
X|Results cannot be obtained for this observation

---

**Name**: specimen_id

**Type**: EI

**HL7 Field**: SPM-2

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2](https://hl7-definition.caristix.com/v2/HL7v2.5.1/Fields/SPM.2) 

---

**Name**: specimen_type

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

**Name**: specimen_collection_method

**Type**: CODE

**HL7 Field**: SPM-7

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
ANP|Plates, Anaerobic
BAP|Plates, Blood Agar
BCAE|Blood Culture, Aerobic Bottle
BCAN|Blood Culture, Anaerobic Bottle
BCPD|Blood Culture, Pediatric Bottle
BIO|Biopsy
CAP|Capillary Specimen
CATH|Catheterized
CVP|Line, CVP
EPLA|Environmental, Plate
ESWA|Environmental, Swab
FNA|Aspiration, Fine Needle
KOFFP|Plate, Cough
LNA|Line, Arterial
LNV|Line, Venous
MARTL|Martin-Lewis Agar
ML11|Mod. Martin-Lewis Agar
MLP|Plate, Martin-Lewis
NYP|Plate, New York City
PACE|Pace, Gen-Probe
PIN|Pinworm Prep
PNA|Aterial puncture
PRIME|Pump Prime
PUMP|Pump Specimen
QC5|Quality Control For Micro
SCLP|Scalp, Fetal Vein
SCRAPS|Scrapings
SHA|Shaving
SWA|Swab
SWD|Swab, Dacron tipped
TMAN|Transport Media, Anaerobic
TMCH|Transport Media, Chalamydia
TMM4|Transport Media, M4
TMMY|Transport Media, Mycoplasma
TMOT|Transport Media
TMP|Plate, Thayer-Martin
TMPV|Transport Media, PVA
TMSC|Transport Media, Stool Culture
TMUP|Transport Media, Ureaplasma
TMVI|Transport Media, Viral
VENIP|Venipuncture
WOOD|Swab, Wooden Shaft

---

**Name**: specimen_source_site_code

**Type**: CODE

**HL7 Field**: SPM-8

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
119297000|Blood specimen (specimen)
71836000|Nasopharyngeal structure (body structure)
45206002|Nasal structure (body structure)

---

**Name**: specimen_description

**Type**: TEXT

**HL7 Field**: SPM-14

**Cardinality**: [0..1]


**Reference URL**:
[https://hl7-definition.caristix.com/v2/HL7v2.8/Fields/SPM.14](https://hl7-definition.caristix.com/v2/HL7v2.8/Fields/SPM.14) 

---

**Name**: specimen_collection_date_time

**Type**: DATETIME

**HL7 Field**: SPM-17-1

**Cardinality**: [0..1]

**Documentation**:

The date which the specimen was collected. The default format is yyyyMMddHHmmsszz


---

**Name**: specimen_received_date_time

**Type**: DATETIME

**HL7 Field**: SPM-18

**Cardinality**: [0..1]

**Documentation**:

Date and time the specimen was received. Default format is yyyyMMddHHmmsszz


---

**Name**: comment_source

**Type**: CODE

**HL7 Field**: NTE-2

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
L|Ancillary (filler) department is source of comment
O|Other system is source of comment
P|Orderer (placer) is source of comment

---

**Name**: comment

**Type**: TEXT

**HL7 Field**: NTE-3

**Cardinality**: [0..1]

---

**Name**: comment_type

**Type**: CODE

**HL7 Field**: NTE-4

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
1R|Primary Reason
2R|Secondary Reason
AI|Ancillary Instructions
DR|Duplicate/Interaction Reason
GI|General Instructions
GR|General Reason
PI|Patient Instructions
RE|Remark

---
