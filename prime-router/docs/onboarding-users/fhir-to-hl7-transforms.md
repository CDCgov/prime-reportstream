This file documents the types of transforms currently being used by Engagement. This document does not recommend any particular method of acheiving the needed transformations or purport to show best practices. This is simply to document how things have been accomplished so far.

## Types of FHIR->HL7 transforms currently used

Default value in field - Default a static value into a single field or subfield
```yaml
  - name: ak-receiving-application
    value: [ '"AKDOH"' ]
    hl7Spec: [ 'MSH-5-1' ]
```

Removing single field - Remove any value in a single field or subfield
```yaml
  - name: ak-ordering-provider-id-number
    value: [ '""' ]
    hl7Spec: [ '/PATIENT_RESULT/ORDER_OBSERVATION/OBR-16-1' ]
```

Remove multiple fields - Remove all values from multiple fields or subfields
```yaml
  - name: remove-hl7-fields
    value: [ '""' ]
    hl7Spec: [ 'MSH-19-1' , 'MSH-19-2' , 'MSH-19-3' , '%{ORC}-2-1' , '%{ORC}-2-2' , '%{ORC}-2-3' , '%{ORC}-2-4' , '%{ORC}-4-1' , '%{ORC}-4-2' , '%{ORC}-4-3' , '%{ORC}-4-4' , '%{OBR}-2-1' , '%{OBR}-2-2' , '%{OBR}-2-3' , '%{OBR}-2-4' ]
```

Modify value - Change the format of a value in a single field or subfield
```yaml
- name: obx-value-dtm-dt
  condition: '%context.extension(%`rsext-obx-observation`).extension.where(url = "OBX.2").value = "DT"'
  value: [ '%resource.value.extension(%`rsext-hl7v2-date-time`).value.toString().replace("-","")' ]
  hl7Spec: [ '%{hl7OBXField}-5' ]
```

Modify value based on valueset - Correlate a set of values to another set of values
```yaml
  - name: ak-patient-ethnicity-identifier-code
    value:
        - 'Bundle.entry.resource.ofType(Patient).extension(%`rsext-ethnic-group`).value.coding[0].code'
    hl7Spec: [ '/PATIENT_RESULT/PATIENT/PID-22-1' ]
    valueSet:
        values:
            H: 2135-2
            N: 2186-5
```

Use customFhirFunction to correlate FHir valuesets with HL7 valuesets
```yaml
  - name: ak-patient-race-coding-system
    resource: 'Bundle.entry.resource.ofType(Patient).extension("http://ibm.com/fhir/cdm/StructureDefinition/local-race-cd").value.coding'
    condition: '%resource.code.exists()'
    value: [ '%resource.system.getCodingSystemMapping()' ]
    hl7Spec: [ '/PATIENT_RESULT/PATIENT/PID-10-3' ]
```

Override base mappings to prevent certain segments/fields from being mapped to outbound HL7
```yaml
  - name: obx-equipment-instance-identifier
    condition: 'false'
```

Override base mappings to prevent a field in specific datatype from being mapped to outbound hl7
```yaml
  - name: xtn-extension-value
    resource: '%resource.extension(%`rsext-xtn-contact-point`)'
    schema: classpath:/metadata/hl7_mapping/receivers/Common/remove-xtn-1/XTNExtension.yml
```
```yaml
elements:

  - name: xtn-2-telecom-use-code
    value: [ '%resource.extension("XTN.2").value' ]
    hl7Spec: [ '%{hl7TelecomPath}-2' ]

  - name: xtn-3-telecom-equipment-type
    value: [ '%resource.extension("XTN.3").value' ]
    hl7Spec: [ '%{hl7TelecomPath}-3' ]

  - name: xtn-4-communication-address
    value: [ '%resource.extension("XTN.4").value' ]
    hl7Spec: [ '%{hl7TelecomPath}-4' ]

  - name: xtn-7-local-number
    value: [ '%resource.extension("XTN.7").value' ]
    hl7Spec: [ '%{hl7TelecomPath}-7' ]

  - name: xtn-9-any-text
    value: [ '%resource.extension("XTN.9").value' ]
    hl7Spec: [ '%{hl7TelecomPath}-9' ]

  - name: xtn-12-unformatted-telephone-number
    value: [ '%resource.extension("XTN.12").value' ]
    hl7Spec: [ '%{hl7TelecomPath}-12' ]
```

Override base mappings to create segments/fields in a specific format/ordering
```yaml
  - name: ca-order-observations
    resource: 'Bundle.entry.resource.ofType(DiagnosticReport)'
    condition: '%resource.count() > 0'
    schema: classpath:/metadata/hl7_mapping/receivers/STLTs/CA/ca-order-observation.yml
    resourceIndex: orderIndex
```
```yaml
constants:
  hl7Order: '/PATIENT_RESULT/ORDER_OBSERVATION(%{orderIndex})'
  diagnostic: 'Bundle.entry.resource.ofType(DiagnosticReport)[%orderIndex]'
  service: 'Bundle.entry.resource.ofType(DiagnosticReport)[%orderIndex].basedOn.resolve()'
  specimen: 'Bundle.entry.resource.ofType(DiagnosticReport)[%orderIndex].specimen.resolve()'
elements:
  - name: ca-observation-result
    resource: '%resource.result.resolve()'
    schema: classpath:/metadata/hl7_mapping/receivers/STLTs/CA/ca-observation-result.yml
    resourceIndex: resultIndex
```
```yaml
constants:
  hl7ObservationPath: '/PATIENT_RESULT/ORDER_OBSERVATION(%{orderIndex})/OBSERVATION(%{resultIndex})'
  rsext: '"https://reportstream.cdc.gov/fhir/StructureDefinition/"'
  observation: '%diagnostic.result[%resultIndex].resolve()'
elements:

  # California requirement: Move all regular notes and AOE questions between last OBX segment and SPM
  #   as NTE segments
  # To do that we have to compare the Observation index against the Observation count
  # to make sure is the last OBX segment and only add notes and AOE questions to that segment
  - name: ca-order-observation-note
    resource: '%diagnostic.result.resolve().note.text.split(''\n'') | %service.note.text.split(''\n'') | %service.supportingInfo.resolve()'
    condition: >-
      (Bundle.entry.resource.ofType(Observation).note.exists() or %service.note.exists() or %service.supportingInfo.exists())
      and (%diagnostic.result.count() - 1) = %resultIndex
    schema: classpath:/metadata/hl7_mapping/receivers/STLTs/CA/ca-order-note.yml
    resourceIndex: noteIndex
    constants:
      hl7NotePath: '%{hl7ObservationPath}'
```
```yaml
elements:
  - name: aoe-note
    condition: '%resource is Observation'
    schema: classpath:/metadata/hl7_mapping/receivers/STLTs/CA/ca-aoe-note.yml

  - name: note
    condition: '%resource is string'
    schema: classpath:/metadata/hl7_mapping/datatypes/annotation/NTE.yml
    constants:
      noteDetails: '%resource.note'
```
```yaml
constants:
  caAoeNteFieldPath: '%{hl7NotePath}/NTE(%{noteIndex})'
  commentCondition: '%resource.code.coding.code.exists() and %resource.code.text.exists()'
elements:
  - name: aoe-note-id
    value: [ '%noteIndex + 1' ]
    hl7Spec: [ '%{caAoeNteFieldPath}-1' ]

  - name: aoe-note-source
    value:
      - '"L"'
    hl7Spec: [ '%{caAoeNteFieldPath}-2' ]

  - name: aoe-note-comment-datetime
    condition: '%resource.value.exists() and %resource.value is dateTime and %commentCondition'
    value: [ '%resource.code.coding.code + " " + %resource.code.text + ": " + %resource.value.toString()' ]
    hl7Spec: [ '%{caAoeNteFieldPath}-3' ]

  - name: aoe-note-comment-string
    condition: '%resource.value.exists() and %resource.value is string and %commentCondition'
    value: [ '%resource.code.coding.code + " " + %resource.code.text + ": " + %resource.value' ]
    hl7Spec: [ '%{caAoeNteFieldPath}-3' ]

  - name: aoe-note-comment-cwe
    condition: '%resource.value.exists() and %resource.value is CodeableConcept and %commentCondition'
    value: [ '%resource.code.coding.code 
    + " " + %resource.code.text 
    + ": " + %resource.value.coding.display 
    + " " + %resource.value.coding.code 
    + " " + %resource.value.coding.system.getCodingSystemMapping()' ]
    hl7Spec: [ '%{caAoeNteFieldPath}-3' ]
```
