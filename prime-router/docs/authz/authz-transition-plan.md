This document will detail the steps needed to successfully transition from the current RS auth to the auth microservice.

# Migration Strategy #

## Migration of senders ##
### Information required ###
* Can existing public keys be transferred to Okta, or will all senders have to be fully reonboarded
* Should support for SMART on FHIR be considered?
  * Further reading: https://www.okta.com/resources/whitepaper/smart-on-fhir-with-okta/

## Migration of website users ##
### Information required ###
* Process to migrate existing user authorizations
  * Map existing RS scopes to Okta scopes
  * How to restrict users to information related to their organization

## Verification ##
### Functional test of sender creation process ###
* Ensure all needed information is captured in new auth flow

## Other Questions ##
