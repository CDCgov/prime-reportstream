
### Schema:         empty
#### Description:   Empty Schema.  For Testing error conditions.  Used by cli ./prime test

---

**Name**: blankField

**Type**: BLANK

**PII**: No

**Cardinality**: [0..1]

---

**Name**: Message_ID

**Type**: ID

**PII**: No

**Cardinality**: [1..1]

**Documentation**:

unique id to track the usage of the message

---

**Name**: Ordering_facility_county

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: County

---

**Name**: Ordering_facilty_state

**Type**: TABLE

**PII**: No

**Cardinality**: [1..1]

**Table**: fips-county

**Table Column**: State

**Documentation**:

The state of the facility which the test was ordered from

---
