# Convert/Translate
The convert step converts the incoming message into the RS FHIR bundle structure and adds any custom sender transforms to the message.

After determining the receivers for a message, the translate step will go through the receiver’s translation settings and check whether the receiver wants the message in HL7 or FHIR. If the receiver wants HL7, the UP can translate the message using the default receiver transforms or use a custom receiver transform.

Note: The Translate step will occur after the Route step (next section), but the same transform mechanisms that are used for the senders (in the Convert step) are used by the receivers (during the Translate step); therefore, the content has been consolidated.

## Differentiating Sender and Receiver Transforms
- Explain how we use the same transforms as the ones documented above, but they are run in different locations depending on if they are a sender or receiver transform.

## Transforms
When creating a transform, please reference [Changing/Updating Sender/Receiver Transforms](../getting-started/standard-operating-procedures/changing-transforms.md) for guidance
- HL7 Transforms
    - HL7 Translation Settings
    - How to use translation configuration features. Make sure to include that they are supported via the covid-19 schema (current version)
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
- FHIR Transforms
- CSV Transforms

## HL7 to FHIR translation process
Note: This part of the Translate step, which occurs after the Route step (next section)
