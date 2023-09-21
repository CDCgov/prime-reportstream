## Problem Statement
In HL7 Converter schemas and FHIR Transformer schemas, when a `resource` is specified within an element, fields like condition can no longer reference the original `resource` value, since `resource` will refer to the new value instead. Consider the following example:
```
# resource: <Path to organization>
- name: performing-organization-name-pracrole
  condition: '%resource.performer.resolve() is PractitionerRole'
  resource: '%resource.performer.resolve().organization.resolve()'
  schema: ./datatype/xon-organization
  constants:
    hl7OrgField: '%{hl7OBXField}-23'
```

The condition here, `%resource.performer.resolve() is PractitionerRole`, `%resource` will use the new `resource` value, `%resource.performer.resolve().organization.resolve()`, which will amount to `<Path to organization>.performer.resolve().organization.resolve()`. Then the condition will amount to `<Path to organization>.performer.resolve().organization.resolve().performer.resolve() is PractitionerRole` which is not what we want. We really want to just reference the original resource's `performer` not the new value of `resource` which is set to be passed into the referenced schema `xon-organization`. As a workaround for this, we add a separate variable to contain a reference to the organization, and use that in `condition` instead:
```
# resource: <Path to organization>
# organization: <Path to organization>
- name: performing-organization-name-pracrole
  condition: '%organization.performer.resolve() is PractitionerRole'
  resource: '%resource.performer.resolve().organization.resolve()'
  schema: ./datatype/xon-organization
  constants:
    hl7OrgField: '%{hl7OBXField}-23'
```

With this new variable, the condition can work more as expected. But in some cases, it's not possible to reliably set that extra variable to point to the same resource as `resource`. The problem with this kind of workaround is illustrated by this code from `order-observation.yml`:
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
```
We want to be able to access both the initial observation (`'%diagnostic.result[%resultIndex].resolve()'`) as well as 
the resource that is the result of this condition 
`%resource.result.resolve() | %service.supportingInfo.resolve().where(specimen.exists().not())`. 
However, to do this, we are needing to set the observation constant to the value of the 
outer resource. This is a problematic workaround because this piece of code isn't actually working how you might 
expect it to. Currently, if there are 5 items in`%resource.result.resolve()` and 2 items in 
`%service.supportingInfo.resolve().where(specimen.exists().not())`, the 
`resourceIndex` will go up to 7. The `observation` expression will be invalid when `resultIndex` is 6 or 7 since
there are only 5 items in `%diagnostic.result` and that is ultimately what resource will end up being set to.

We want to have a way to access that outer resource, the one initially defined, so that in places like these we can
reference the parent and some of its children without losing the value of the initial resource.

There are two ways we can go about this, and they are not mutually exclusive. 

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