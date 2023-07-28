# Datetime formating as part of receiver translation setting

## Context

In the receiver translation settings, there is a useHighPrecisionHeaderDateTimeFormat flag that allows us to set the DateTime Precision format. When its set to true, the datetime will include milliseconds.  The DateTime Precision format can be achieved via the convertDateTimeToHL7 function in FhirPathUtils.kt evaluating the flag and returning the DateTime format according to the flag set to true or false.

## Problem

Currently, Universal Pipeline doesn’t have ability to configure the datetime format for receivers using the useHighPrecisionHeaderDateTimeFormat flag as the COVID pipeline.

## Goal

Implement the feature to Universal Pipeline to allow us to configure datetime and work the same way as of COVID pipline receiver configutation.

## Receiver setting

```yaml
- name: lt-pdh
  description: LT Department of Health
  jurisdiction: STATE
  stateCode: LT
  receivers:
    - name: elr
      topic: covid-19
      jurisdictionalFilter: [ "matches(ordering_facility_state, LT)" ]
      translation:
        type: HL7
        useBatchHeaders: true
        receivingApplicationName: LT-PDH
        receivingApplicationOID:
        receivingFacilityName: LT-PDH
        receivingFacilityOID:
        convertDateTimesToReceiverLocalTime: false
        useHighPrecisionHeaderDateTimeFormat: true
  externalName: null
  timeZone: null
  dateTimeFormat: "OFFSET"
```

## Implementation
•	We will pass the receiver object that contains all the receiver settings to the FHIRTranslation.kt:FhirToHl7Converter.convert method.  In turn, it will pass the reciver object in CustomerContext object to the processSchema function. And, it will propagate down to FhirPathUtils.kt:convertDateTimeToHL7.
•	To format dateTime, convertDateTimeToHL7 will use the DateUtilities.kt:formatDateForReceiver function as the COVID pipeline does.   All attributes needed to format datetime are in the receiver object.
