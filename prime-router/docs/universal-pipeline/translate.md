# Translate
After the [route step](route.md) has determined the receivers for a message, the translate step will determine based on each receiver’s translation settings what format the message should be sent in. If the receiver wants HL7v2, the UP can translate the message using the default receiver transforms or use a custom receiver transform. If the receiver wants FHIR, since the message at this point is already in the FHIR format, we can either send it to them as it is, or we can optionally apply a `FhirTransformer`, similarly to what is used for sender-specific changes in the [convert step](convert.md), but this time as a receiver transform.

## Transforms

### HL7v2 Transforms
HL7v2 receivers can utilize receiver transforms as well as some other organization settings to customize the process of going from FHIR to HL7v2. Unlike `FhirTransformer`, which operates on FHIR-to-FHIR conversion, this scenario makes use of the `FhirToHl7Converter`, which operates (as the name suggests) on FHIR-to-HL7v2 conversion.

#### FhirToHl7Converter configuration
[section all about configuration files and the different settings that are possible, link to separate page if necessary]

#### Other HL7v2 receiver settings 
[be sure to investigate support in Universal Pipeline vs Legacy (COVID) Pipeline]
- useTestProcessingMode
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
- convertPositiveDateTimeOffsetToNegative
- stripInvalidCharsRegex
- convertDateTimesToReceiverLocalTime
- useHighPrecisionHeaderDateTimeFormat

### FHIR Transforms
Since the message at this point is already in the FHIR format, when receivers want the message in FHIR format, no format translation needs to occur, however, the receiver may still want to apply some receiver-specific transforms to the FHIR bundle. This is done by applying a `FhirTransformer`, similarly to what is used for sender-specific changes in the [convert step](convert.md), but this time as a receiver transform. For details on the FhirTransformer settings, see [this section](convert.md#fhirtransformer-configurations).

#### Other FHIR receiver settings

### CSV Transforms
CSV receivers are not yet supported in the Universal Pipeline.

### Deidentification
[section for settings for deidentification, might be able to remove this if it fits into the above sections instead]