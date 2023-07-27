# API Endpoint Hardening Proposal

## Context and Motivation

As a secure system, the Prime Data Hub's API end-points must be hardened against malicious attacks that manipulate the payload sent through the data hub. 
The API end-points should have a number of limits, checks and validations that prevents from an attacker who knows about the inner workings of the data hub (ie. read our source code) from information disclosure and service disruption. 

This epic is not about DDoS attacks which will be handled by the WAF epics. 

## Goals 

1. Enforce limits on report size overall size, and on individual data elements size
2. Prevent SQL and Javascript injection, by design.

## Detailed proposal for overall size limitation, and individual field size limitations

The system will define these overall limits:

- MAX_BYTES - a reasonable limit on the overall size of the payload. (Azure limit is 100Meg.)   **Suggest: 40Meg. (allows 4k per row)** 
- MAX_ITEMS - a limit on the number of Items (in a CSV, this is a limit on the number of rows + 1).  **Suggest: 10,000 Items (10,001 rows)**
- **Phase 2 work?**: (MAX_COLUMNS - for CSVs only, a limit on the number of columns input. **Suggest: 1000 columns**)
- MAX_ITEM_SIZE - byte limit on the size of one item.  This is most useful for reading in hl7 - we'll want a way to reject inputs without having to get bogged down in detailed item parsing.   **Suggest:  200K, to match Redox max**
- MAX_ERRORS - Validation will fail immediately if the total number of Errors passes this threshold.  **Suggest: 100 errors**
- Note:  no limit on warnings, since warnings by definition should not cause failures.

These limits will be set globally, hardcoded, for now.  Could be set per schema in the future, with careful consideration of how that gets locked down.

The limits will apply to both CSV and HL7, when appropriate.

### Proposed Order to Apply new "max" Validation Rules

The goal is to minimize memory and cpu impact by failing as early as possible.

1. MAX_BYTES test.  First check Content-Length header, then check actual bytes.  Return immediately on failure. (*? Can this be checked by firewall as well?*)
2. MAX_ITEMS and MAX_ITEM_SIZE tests, for CSVs, can be done prior to reading into Tablesaw format.  Return on failure.
3. For CSVs, data is now read into internal Tablesaw structure.  MAX_COLUMNS test is applied. Return immediately on failure.
4. Individual field validation validation rules applied here, including Max Bytes.  No other checking.
   At all times, if MAX_ERRORS is passed, return immediately.
5. Second pass validation: All other existing validation rules applied
   At all times, if MAX_ERRORS is passed, return immediately.

### Proposed Validation Rules per PRIME Type

Proposal is for very simple fixed maximums per type, with only a small number of length values used, all in bytes, so its very easy for customers to understand.
Only one new type is needed to support this, which I'm calling **BIGTEXT**.

Note: other validations occur, but are not covered here (yet).

|     Type        | Max Bytes |  Action on max bytes failure | Notes
|-----------------|-----------|------------------------------|---
| TEXT            | 256       | Truncate, continue| Example: ordering_facility_name might go over 64.  Also allows some room for UTF-8
| TEXT_OR_BLANK   | 256       | Truncate, continue|  Blank values are valid (not null)
| BIGTEXT         | 65536     | Truncate, continue| **New Proposed Type** Example: `comment field`, `test_method_description`, `remarks` (HL7 Limit is 64K on remarks)
| BIGTEXT_OR_BLANK| 65536     | Truncate, continue| **New Proposed Type**   Blank values are valid (not null)
| NUMBER          | 16?       | Fail, always error|
| DATE            | 16?       | Fail, always error|
| DATETIME        | 16?       | Fail, always error|
| DURATION        | 16?       | Null-out, continue| Not used as of this writing.
| CODE            | 16?       | Fail, always error| CODED with a HL7 SNOMED-CT, LONIC valueSet
| TABLE           | 4096      | Fail, always error| (max is based on largest cell in LIVD table, which is 1133 bytes)
| TABLE_OR_BLANK  | 4096      | Fail, always error| 
| EI              | 32?       | Fail, always error| A HL7 Entity Identifier (4 parts)
| HD              | 32?       | Fail, always error|  ISO Hierarchic Designator 
| ID              | 32?       | Fail, always error| none Generic ID 
| ID_CLIA         | 64?       | Fail, always error| CMS CLIA number (must follow CLIA format rules) 
| ID_DLN(DriveLic)| 64        | Fail, always error| 
| ID_SSN          | 11        | Fail, always error| 
| ID_NPI          | ?         | Fail, always error| 
| STREET          | 256	      | Truncate, continue|
| STREET_OR_BLANK | 256       | Truncate, continue|
| CITY            | 256       | Truncate, continue|
| POSTAL_CODE     | 32?       | Truncate, continue|
| PERSON_NAME     | 256       | Truncate, continue|
| TELEPHONE       | 20?       | Truncate, continue| 
| EMAIL           | 256       | Truncate, continue|
| BLANK           | 1?        | Truncate, continue|

## Proposal for SQL injection protection

- Review code and look for weaknesses.

## Appendix / Notes

### HL7 Notes on Size limitations

- From [www.hl7.eu](http://www.hl7.eu/HL7v2x/v282/std282/ch02.html) :  
```
HL7 sets no limits on the maximum size of HL7 messages. The Standard assumes that the communications environment can transport messages of any length that might be necessary. In practice, sites may agree to place some upper bound on the size of messages and may use the message continuation protocol, described later in this chapter, for messages that exceed the upper limit.

When values are truncated because they are too long (whether because some applicable specification limits the length of the item, or because the application is not able to store the full value), the value should be truncated at N-1, where N is the length limit, and the final character replaced with a special truncation character. This means that whenever that value is subsequently processed later, either by the system, a different system, or a human user, the fact that the value has been truncated has been preserved, and the information can be handled accordingly.

The truncation character is not fixed; applications may use any character. The truncation character used in the message is defined in MSH-2. The default truncation character in a message is # (23), because the character must come from the narrow range of allowed characters in an instance. The truncation character only represents truncation when it appears as the last character of a truncatable field.
```

HL7 does not have hard size limits.  [See here, for example](https://healthstandards.com/blog/2007/01/05/introduction-to-hl7-field-cardinality-and-field-lengths/)

## Guidance and Requirements, taken from the Zendesk tickets

#### Max size Test cases:
- What if the request body is 100x memory size, would all resources be consumed?
- What if there is a 200,000 row CSV being loaded?
- Write test framework to cover these and other test cases, that can be run against the azure functions, either locally or in test.

### Acceptance Criteria
- [X] Consideration of HL7 limits
- [ ] Allow individual custom schemas to set limits
- [X] truncate fields when converting
- [X] Consider whether to Error or Warn on truncation.

Above proposal does not meet the 'individual custom schema' criteria.

