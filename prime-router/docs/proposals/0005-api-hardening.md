# API Endpoint Hardening Proposal

## Context and Motivation

As a secure system, the Prime Data Hub's API end-points must be hardened against malicious attacks that manipulate the payload sent through the data hub. 
The API end-points should have a number of limits, checks and validations that prevents from an attacker who knows about the inner workings of the data hub (ie. read our source code) from information disclosure and service disruption. 

This epic is not about DDOS attacks which will be handled by the WAF epics. 

## Goals 

1. Enforce limits on report size overall size, and on individual data elements size
2. URL hashing in the of the data hubs reports end-point
3. Expiring URLs and URL hashing for download URLs
4. Prevent SQL and Javascript injection, by design.

## Detailed proposal for overall size limitation, and individual field size limitations

The system will define these overall limits:

- MAX_BYTES - a reasonable limit on the overall size of the payload. Azure limit is 100Meg.   **Suggest: 40Meg. (allows 4k per row)** 
- MAX_ITEMS - a limit on the number of Items (in a CSV, this is a limit on the number of rows + 1).  **Suggest: 10,000 Items (10,001 rows)**
- **Phase 2 work?**: (MAX_COLUMNS - for CSVs only, a limit on the number of columns input. **Suggest: 1000 columns**)
- MAX_ITEM_SIZE - byte limit on the size of one item.  This will be most useful in the future, when we read in hl7 - we'll want a way to reject inputs without having to get bogged down in detailed item parsing.   **Suggest:  200K, to match Redox max**
- MAX_ERRORS - Validation will fail immediately if the total number of Errors passes this threshold.  **Suggest: 100 errors**
- Note:  no limit on warnings.

These will be set globally, hardcoded, for now.  Could be set per schema in the future, with careful consideration of how that gets locked down.

### Proposed Order to Apply new "max" Validation Rules

1. MAX_BYTES test.  First check Content-Length header, then check actual bytes.  Return immediately on failure. (*? Can this be checked by firewall as well?*)
2. MAX_ITEMS and MAX_ITEM_SIZE tests, for CSVs, can be done prior to reading into Tablesaw format.  Return on failure.
3. For CSVs, data is now read into internal Tablesaw structure.  MAX_COLUMNS test is applied. Return immediately on first failure.
   At all times, if MAX_ERRORS is passed, return immediately.
4. First pass validation:  Individual field Max Bytes validation rules applied here, including truncation.  No other checking.
   At all times, if MAX_ERRORS is passed, return immediately.
5. Second pass validation: All other existing validation rules applied
   At all times, if MAX_ERRORS is passed, return immediately.

### Proposed Validation Rules per PRIME Type

Proposal is for very simple fixed maximums per type, with only a small number of length values used, all in bytes, so its very easy for customers to understand.
Only one new type is needed to support this, which I'm calling **BIGTEXT**.

|     Type        | Max Bytes | Other validation rules and notes
|-----------------|-----------|----------------------------------
| TEXT            | 256       | Example: ordering_facility_name might go over 64.  Also allows some room for UTF-8
| BIGTEXT         | 65536     | *New Proposed Type* Example: `comment field`, `test_method_description`, `remarks` (HL7 Limit is 64K on remarks)
| TEXT_OR_BLANK   | 256       |  Blank values are valid (not null) 
| NUMBER          | ?        | 
| DATE            | ?        |  
| DATETIME        | ?        |  
| DURATION        | ?        |  
| CODE            | ?        |  CODED with a HL7 SNOMED-CT, LONIC valueSet
| TABLE           | 4096      | (max is based on largest cell in LIVD table, which is 1133 bytes)
| TABLE_OR_BLANK  | 4096      |  
| EI              | ?        |  A HL7 Entity Identifier (4 parts)
| HD              | ?        |  ISO Hierarchic Designator 
| ID              | ?        |  Generic ID 
| ID_CLIA         | ?        |  CMS CLIA number (must follow CLIA format rules) 
| ID_DLN          | ?        |  
| ID_SSN          | ?        |  
| ID_NPI          | ?        |  
| STREET          | 256	      |  
| STREET_OR_BLANK | 256       |  
| CITY            | 256       |  
| POSTAL_CODE     | ?        |  
| PERSON_NAME     | 256       |  
| TELEPHONE       | ?        |  
| EMAIL           | 256       |  
| BLANK           | ?        |  


##  Questions on URL Hashing

- Should we allow for an optional digest on the payload, as way to confirm data is not corrupted in transit?

## Proposal for SQL injection protection


## Notes

### HL7 Notes on Size limitations

- From [www.hl7.eu](http://www.hl7.eu/HL7v2x/v282/std282/ch02.html) :  
```
HL7 sets no limits on the maximum size of HL7 messages. The Standard assumes that the communications environment can transport messages of any length that might be necessary. In practice, sites may agree to place some upper bound on the size of messages and may use the message continuation protocol, described later in this chapter, for messages that exceed the upper limit.

When values are truncated because they are too long (whether because some applicable specification limits the length of the item, or because the application is not able to store the full value), the value should be truncated at N-1, where N is the length limit, and the final character replaced with a special truncation character. This means that whenever that value is subsequently processed later, either by the system, a different system, or a human user, the fact that the value has been truncated has been preserved, and the information can be handled accordingly.

The truncation character is not fixed; applications may use any character. The truncation character used in the message is defined in MSH-2. The default truncation character in a message is # (23), because the character must come from the narrow range of allowed characters in an instance. The truncation character only represents truncation when it appears as the last character of a truncatable field.
```

HL7 does not have hard size limits.  [See here, for example](https://healthstandards.com/blog/2007/01/05/introduction-to-hl7-field-cardinality-and-field-lengths/)

### RedoxEngine Notes on Size Limitations

Redox has a 200K limit for files submitted via its API and 30Meg on files uploaded. Note that the 200k corresponds to a single Item max limit in our world, so that's actually quite large, equivalent to 1000 column CSV of 200chars each.

[Taken from here](https://developer.redoxengine.com/onboarding/)
```
The break point in which you would use the file upload endpoint vs. directly sending data to the API as a base64 encoded string or plain text string is currently 200KB. If you expect that the contents of your messages may occasionally exceed 200KB, you should use this method to upload the file to Redox.

There is currently a 30MB file size limit for all files uploaded to Redox. Attempting to load any larger files will return a 413 error response.
```
(Note:  I've also seen a 10Meg file size limit at Redox)

Character encoding:  Redox uses UTF-8.   However, [see here](https://www.redoxengine.com/blog/everything-you-wanted-to-know-about-character-encoding-in-hl7-and-redox/) for messy issues related to HL7.

## Guidance and Requirements, taken from the Zendesk tickets

#### Max size Test cases:
- What if the request body is 100x memory size, would all resources be consumed?
- What if there is a 200,000 row CSV being loaded?
- Write test framework to cover these and other test cases, that can be run against the azure functions, either locally or in test.

### Acceptance Criteria
- [X] Consideration of HL7 and Redox limits
- [ ] Allow individual custom schemas to set limits
- [X] truncate fields when converting
- [X] Consider whether to Error or Warn on truncation.

### Need for URL Signing and Expiration

To reduce our attack surface on our APIs, we are considering a scheme for URL signing, to prevent URL tampering in the report end-point by adding a URL hash that is checked on every request.

#### Implementation Options
- Consider what Frontdoor and Akamai can do with URL signing techniques.
- Verify with the CISO team about our implementation

#### URL Hashing Tasks

Use a FIPS 180-2 approved hash algorithm. https://csrc.nist.gov/publications/detail/fips/180/4/final
Put initialization materials into the secrets keyvault.
Rotation of initialization materials in secret keyvalut.
Develop a common modules and libraries for URL signing
Develop a conical form of every URL query parameters
add a hash= query parameter as required for every URL
check hash on entry


