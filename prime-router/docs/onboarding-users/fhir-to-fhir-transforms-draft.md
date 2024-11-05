## FHIR to FHIR transforms

This document goes through the common FHIR to HL7 transforms that we currently use (see [FHIR to HL7 transforms](https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/onboarding-users/fhir-to-hl7-transforms.md)) and comments on whether it is currently supported as a FHIR to FHIR transform. 

### default value in field
```
  - name: patient-city-default
    condition: 'Bundle.entry.resource.ofType(Patient).address[0].city.empty().not()'
    bundleProperty: 'Bundle.entry.resource.ofType(Patient).address[0].city'
    value: ['"default"'] 
```

### replace value
```
  - name: patient-last-name-replace
    condition: 'Bundle.entry.resource.ofType(Patient).name[0].family.empty().not()'
    bundleProperty: 'Bundle.entry.resource.ofType(Patient).name[0].family'
    value: ['Bundle.entry.resource.ofType(Patient).name[0].family.value.replace("W","A")']
```
### replace multiple values

can't be done within a single rule, maybe need bundleProperty array?


### change value format
can use function (example below) or fhirpath (like in patient-last-name-replace example above)
```
  - name: hidden-patient-first-name
    resource: 'Bundle.entry.resource.ofType(Patient)'
    bundleProperty: '%resource.name'
    action: SET  
    function: "deidentifyHumanName('##hidden##')"
```

### modify value based on valueset
can be done as long as we have the bundleProperty that maps to the hl7 field

### fhir valueset to hl7 valueset using function
same as above

### override base mappings
We need the bundle property that maps to the hl7 field. Can we just pass in the element name/id? That way we can easily translate the transforms that we have already.
We can also use the DELETE action.   Documentation for ACTIONs on transforms would be helpful for this.
a rule like 
```  
    - name: obx-equipment-instance-identifier
      condition: 'false'
```
would be
```
    - name: delete-obx-equipment-instance-identifier
      resource: '%resource.device.resolve().identifier.union(%resource.extension(%`rsext-obx-observation`).extension.where(url = "OBX.18").tail().value.resolve().identifier)'
      condition: '%resource.value.exists() or %resource.extension(%`rsext-assigning-authority`).exists()'
      action: DELETE
```
this doesnt work currently

### override base mappings for a specific datatype
same as above

### segments in a specific ordering
can use the APPEND action
```
  - name: add-names-in-notes-to-patient
    resource: 'Bundle.entry.resource.ofType(ServiceRequest).note'
    bundleProperty: 'family'
    value: ['%resource.text']
    action: APPEND
    appendToProperty: 'Bundle.entry.resource.ofType(Patient).name'
```
