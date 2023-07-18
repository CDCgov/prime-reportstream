# Datetime formating as part of receiver translation setting

## Context

In receiver translation setting, there is a useHighPrecisionHeaderDateTimeFormat flag that allows us to set the DateTime Percision format. When it sets true, the datetime will include milliseconds.  The DateTime Percision format can be archeived via the convertDateTimeToHL7 function in FhirPathUtils.kt evaluating the flag and return DateTime format according to the flag set to true or false.

## Problem

Currently, Universal Pipeline doesn’t have ability to configure the datetime format for receivers using the the useHighPrecisionHeaderDateTimeFormat flag as COVID pipeline.

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
•	We will pass the receiver object that contains all receiver setting to the the FHIRTranslation.kt:FhirToHl7Converter.convert method.  Inturn, it will call to the FhirPathUtils.kt:convertDateTimeToHL7.
•	To formate dateTime, convertDateTimeToHL7 will use the DateUtilities.kt:formatDateForReceiver function as COVID pipeline does.   All attributes needed to format datetime are in the receiver object.
