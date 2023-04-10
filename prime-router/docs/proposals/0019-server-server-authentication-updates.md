## Background

See the initial proposal for [authentication](./0001-authentication.md)

## Goals

* Update the server to server auth logic to match the FIHR auth spec
* Update it to support ETOR handling a more complicated auth flows

## Proposal

Update the server to server authentication flow to match the SMART on FHIR spec (referenced below) and associate key pairs
with the organization rather than the sender.

The key changes to how the implementation currently works are:

* Keys are added to the organization rather than the sender
* The kid for each key must be unique within the set of keys associated with that scope
* All the applicable (per the spec) keys are tried while checking a JWS 

### Naming conventions

* kid must not be any unique value, recommendation would be to use the `{orgName}.{unique identifier}` 
for example: `healthy-labs.michael-kalish`
* The `iss` `sub`, `client_id` values are all the same and match the organization's name 

### Upgrade path

* Add support for adding keys to organization
* Update the implementation to match the proposal on `kid`
* Migrate keys from senders to organization
* Clean up any old code that used senders for storing keys


## References
[1] FHIR Bulk Data Access http://hl7.org/fhir/uv/bulkdata/authorization/index.html
