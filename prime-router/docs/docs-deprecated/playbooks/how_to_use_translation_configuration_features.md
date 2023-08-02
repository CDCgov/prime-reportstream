# How to Use ReportStream's Translation Configuration Features

Translation features provide the ability to configure a receiver's HL7 test data. For example, a receiver may request to have a particular field blank, or truncate a field to HL7 specs. Other features allow for the replacement of values or change in output formats such as time-and-date, no AOE, and converting diamerics to ascii.

### Our Configuration Library

Below are the configuration features currently used in ReportStream (RS). These features live in the TranslatorConfiguration.kt file. 

- useTestProcessingMode(depricated, do not use)
- useBatchHeaders
- receivingApplicationName
- receivingApplicationOID
- receivingFacilityName
- receivingFacilityOID
- messageProfileId
- replaceValue
- replaceValueAwithB
- reportingFacilityName
- reportingFacilityId
- reportingFacilityIdType
- suppressQstForAoe
- suppressHl7Fields
- suppressAoe
- defaultAoeToUnknown
- replaceUnicodeWithAscii
- useBlankInsteadOfUnknown
- truncateHDNamespaceIds
- truncateHl7Fields
- usePid14ForPatientEmail
- convertTimestampToDateTime
- cliaForOutOfStateTesting
- cliaForSender
- phoneNumberFormatting
- suppressNonNPI
- processingModeCode
- replaceDiiWithOid
- applyOTCDefault
- useOrderingFacilityName
- valueSetOverrides
- nameFormat
- receivingOrganization
- stripInvalidCharsRegex
- useHighPrecisionHeaderDateTimeFormat

### A Practical Example

```
    translation:
      type: HL7
      truncateHl7Fields: OBX-23-1
      suppressHl7Fields: OBX-18-1, OBX-18-2, OBX-18-3, OBX-18-4, OBX-15-3
      replaceValue:
        MSH-3-1: CDC PRIME - Atlanta,
```

#### Explanation

- **truncateHl7Fields**: This will truncate any hl7 field to their hl7 spec. In this case, it will truncate OBX-23-1 to 50 characters.
- **suppressHl7Fields**: This feature suppresses  all fields listed in the string. All listed fields will be blank in the output hl7 file.
- **replaceValue**: The replaceValue feature will replace the value of the hl7 field with the specified value. In the example above, the output hl7 file will have `CDC PRIME - Atlanta,` in MSH-3-1.

Note: Some features take multiple parameters and some can only take one. The ones that are Boolean for example, can only take `true` or `false`.


### Configurations

- **replaceUnicodeWithAscii**: Boolean type, can only take true of false. This feature replaces diacritics into their ascii representation. It checks the whole hl7 message, and it replaces any unicode diacritc that it finds.
Example:
  - ```
    translation:
      replaceUnicodeWithAscii: true 
    ```
  - `ÀÁÂÃÄÅ, ÈÉÊË, Î, Ô, Ù, Ç` would be replaced with `AAAAAA, EEEE, I, O, U, C`