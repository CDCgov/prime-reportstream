
### Schema:         empty
### Topic:          covid-19
### Tracking Element: Message_ID (message_id)
### Base On: [covid-19](./covid-19.md)
### Extends: [none](./none.md)
#### Description:   Empty Schema.  For Testing error conditions.  Used by cli ./prime test

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
