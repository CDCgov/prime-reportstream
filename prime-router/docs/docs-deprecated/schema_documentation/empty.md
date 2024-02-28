### Schema: empty
### Topic: covid-19
### Tracking Element: Message_ID (message_id)
### Base On: [covid-19](./covid-19.md)
### Extends: none
#### Description: Empty Schema.  For Testing error conditions.  Used by cli ./prime test

---

**Name**: blankField

**ReportStream Internal Name**: blank_field

**Type**: BLANK

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Message_ID

**ReportStream Internal Name**: message_id

**Type**: ID

**PII**: No

**Cardinality**: [1..1]

**Documentation**:
unique id to track the usage of the message
---

**Name**: Ordering_facility_county

**ReportStream Internal Name**: ordering_facility_county

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: Ordering_facilty_state

**ReportStream Internal Name**: ordering_facility_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:
The state of the facility which the test was ordered from
---

**Name**: processing_mode_code

**ReportStream Internal Name**: processing_mode_code

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Default Value**: P

**Cardinality**: [1..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
D|Debugging|HL7
P|Production|HL7
T|Training|HL7

**Documentation**:
P, D, or T for Production, Debugging, or Training
---

**Name**: sender_fullname

**ReportStream Internal Name**: sender_fullname

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

---

**Name**: sender_orgname

**ReportStream Internal Name**: sender_orgname

**Type**: TEXT

**PII**: No

**Cardinality**: [1..1]

---
