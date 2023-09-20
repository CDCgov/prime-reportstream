## Problem Statement
The problem is a bit complex, so, to start let's look at an example from `order-observation.yml`:
```
- name: observation-result-with-aoe
  # Grab only the AOE observations from ServiceRequest.supportingInfo NOT associated with a specimen
  resource: '%resource.result.resolve() | %service.supportingInfo.resolve().where(specimen.exists().not())'
  schema: observation-result
  resourceIndex: resultIndex
  constants:
  hl7ObservationPath: '/PATIENT_RESULT/ORDER_OBSERVATION(%{orderIndex})/OBSERVATION(%{resultIndex})'
  hl7OBXField: '/PATIENT_RESULT/ORDER_OBSERVATION(%{orderIndex})/OBSERVATION(%{resultIndex})/OBX'
  observation: '%diagnostic.result[%resultIndex].resolve()'
condition should be: is the observation's performer a practitionerrole
%outerResource.performer.resolve() is PractitionerRole
```

This piece of code actually isn't working how you might expect it to. Currently if there are 5 items in 
`%resource.result.resolve()` and 2 items in `%service.supportingInfo.resolve().where(specimen.exists().not())` the 
index will be 7 but there will only actually be the first 5 items in the resource. We want to have a way to access 
that outer resource, the one initially defined, so that in places like these we can reference the parent and some of 
its children without losing the valyue of the initial resource.

There are two ways we can go about this and they are not mutually exclusive. 

## Proposals
### Outer Resource
This is, by far, the less difficult option. In the code, we would basically set a constant called `outerResource` to 
equal the resource being passed into the file. That way, there would automatically be access in each file to the outer 
resource. So in `FhirToHl7Converter`, we would basically create a constant that would be set to the focusResource. 

### .parent()
Initially, it was thought that this would get the outerResource, but calling `.parent()` sounds more like you are 
getting the resource above the current one. For instance, if resource was set to 
`%resource.organization.resolve().contact.telecom` calling `.parent()` on it would just get us 
`%resource.organization.resolve().contact` creating a way to transverse back up the tree. This is much more complicated
to implement. We would need to keep the element path in our memory, skipping over any resolves, and then evaluate the 
shortened path to get the intended resource.

## Conclusion
While we may want to eventually implement both, for now, the outer resource is simplest and achieves the intended goal 
which is to solve the issue of holding onto the value of the initial resource. 