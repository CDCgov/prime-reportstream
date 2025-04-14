## FHIR to FHIR transforms

This document goes through the common FHIR to HL7 transforms that we currently use (see [FHIR to HL7 transforms](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/onboarding-users/fhir-to-hl7-transforms.md)) and comments on whether it is currently supported as a FHIR to FHIR transform. 


### Replace value in field - Default a static value into a single field or subfield
FHIR to HL7
```yaml
  - name: patient-city-default
    value: [ '"default"' ]
    hl7Spec: [ '"/PATIENT_RESULT/PATIENT/PID-11-3"' ]
```
FHIR to FHIR:
```yaml
  - name: patient-city-default
    bundleProperty: 'Bundle.entry.resource.ofType(Patient).address[0].city'
    value: ['"default"'] 
```

### Replace value in multiple fields/subfields
FHIR to HL7
```yaml
  - name: remove-hl7-fields
    value: [ '""' ]
    hl7Spec: [ 'MSH-19-1' , 'MSH-19-2' , 'MSH-19-3' , '%{ORC}-2-1' , '%{ORC}-2-2' , '%{ORC}-2-3' , '%{ORC}-2-4' , '%{ORC}-4-1' , '%{ORC}-4-2' , '%{ORC}-4-3' , '%{ORC}-4-4' , '%{OBR}-2-1' , '%{OBR}-2-2' , '%{OBR}-2-3' , '%{OBR}-2-4' ]
```
FHIR to FHIR:
Can't be done within a single rule, an option would be to implement a bundleProperty array.


### Modify value - Change the format of a value in a single field or subfield
FHIR to HL7
```yaml
- name: obx-value-dtm-dt
  condition: '%context.extension(%`rsext-obx-observation`).extension.where(url = "OBX.2").value = "DT"'
  value: [ '%resource.value.extension(%`rsext-hl7v2-date-time`).value.toString().replace("-","")' ]
  hl7Spec: [ '%{hl7OBXField}-5' ]
```
FHIR to FHIR:
Can use function:
```yaml
  - name: hidden-patient-first-name
    resource: 'Bundle.entry.resource.ofType(Patient)'
    bundleProperty: '%resource.name'
    action: SET  
    function: "deidentifyHumanName('##hidden##')"
```
or fhirpath:
```yaml
  - name: patient-last-name-replace
    condition: 'Bundle.entry.resource.ofType(Patient).name[0].family.empty().not()'
    bundleProperty: 'Bundle.entry.resource.ofType(Patient).name[0].family'
    value: ['Bundle.entry.resource.ofType(Patient).name[0].family.value.replace("W","A")']
```

### Modify value based on valueset - Correlate a set of values to another set of values
FHIR to HL7
```yaml
  - name: patient-sex
    resource: 'Bundle.entry.resource.ofType(Patient)'
    value: [ '%resource.gender' ]
    hl7Spec: [ '/PATIENT_RESULT/PATIENT/PID-8' ]
    valueSet:
      values:
        unknown: U
        female: F
        male: M
        other: O
```

FHIR to FHIR:
Can be done as long as we have the bundleProperty that maps to the hl7 field.
```yaml
  - name: patient-sex
    resource: 'Bundle.entry.resource.ofType(Patient)'
    bundleProperty: '%resource.gender'
    value: [ '%resource.gender' ]
    valueSet:
      values:
        unknown: U
        female: F
        male: M
        other: O
```

### Use customFhirFunction to correlate FHIR valuesets with HL7 valuesets
FHIR to HL7
```yaml
  - name: ak-patient-race-coding-system
    resource: 'Bundle.entry.resource.ofType(Patient).extension("http://ibm.com/fhir/cdm/StructureDefinition/local-race-cd").value.coding'
    condition: '%resource.code.exists()'
    value: [ '%resource.system.getCodingSystemMapping()' ]
    hl7Spec: [ '/PATIENT_RESULT/PATIENT/PID-10-3' ]
```

FHIR to FHIR:
Same as previous scenario; Can be done by using bundleProperty instead of hl7Spec.

### Override base mappings to prevent certain segments/fields from being mapped to outbound HL7
FHIR to HL7
```yaml
  - name: obx-equipment-instance-identifier
    condition: 'false'
```
FHIR to FHIR:
We need the bundle property that maps to the hl7 field. An option would be to implement a way to pass in the element name/id. That way we can easily translate the FHIR-to-HL7 transforms that we have already.
We can also use the DELETE action.   Documentation for ACTIONs on transforms would be helpful for this.
A rule like the following does not work currently: (also might be using it incorrectly)
```yaml
    - name: delete-obx-equipment-instance-identifier
      resource: '%resource.device.resolve().identifier.union(%resource.extension(%`rsext-obx-observation`).extension.where(url = "OBX.18").tail().value.resolve().identifier)'
      condition: '%resource.value.exists() or %resource.extension(%`rsext-assigning-authority`).exists()'
      action: DELETE
```

### Override base mappings for a specific datatype
FHIR to FHIR:
Same as above

### Segments in a specific ordering
FHIR to FHIR:
Can use the APPEND action like so:
```yaml
  - name: add-names-in-notes-to-patient
    resource: 'Bundle.entry.resource.ofType(ServiceRequest).note'
    bundleProperty: 'family'
    value: ['%resource.text']
    action: APPEND
    appendToProperty: 'Bundle.entry.resource.ofType(Patient).name'
```