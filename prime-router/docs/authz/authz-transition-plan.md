This document will detail the steps needed to successfully transition from the current RS auth to the auth microservice.

# Preparation #

* Implement profile groups (API > ReportStream API > Claims > sender_name)
* API changes for new auth flow
  * Need to decide if cutting over or maintaining both APIs - see analysis in Questions section 
* Consider design for when auth service updates sender profiles (must be done via API)
* Create test workflow in staging representative of production environment
  * Update workflow documentation to create new sender
* Add auth/submissions endpoints to deployment and CI processes
  * auth and submissions projects currently ignored
  * end2end testing for endpoints should be created
    * Consider how developer environment setup needs to change 
* Create Okta applications/APIs in production Okta (API, groups)
  * Consider how to run a test in production 
* Add integration/smoke tests to include end to end test of auth/submissions
  * New tests alongside existing tests for original report API until it is removed?
* Migrate internal functions utilizing deprecated authentication
  * `prime login` uses Okta user login flow; need application user flow to allow machine to machine authentication (CI runners can log in)
  * Smoke tests against staging additionally rely on an Azure functions key and an authorization key stored in Environment variables


# Migration Strategy #

## Migration of senders and website users ##
* Migrate all senders in staging
  * Can existing public/private keys be transferred to Okta, or will all senders have to be fully reonboarded?
  * Do we intend to run both auth systems simultaneously?
    * Document differences between RS auth and proposed Okta auth (endpoints used, etc.) 
* Users: Process to migrate existing user authorizations
    * Map existing RS scopes to Okta scopes
    * How to restrict users to information related to their organization
* Do sender and user migrations need to be performed simultaneously?

## Other Questions ##

Should we maintain old and new auth/report endpoints simultaneously?

* Pros:
  * Flexible timeline for transitioning senders; no hard cutoff date, don't have to migrate all at once
  * Can designate a pilot partner before initiating a larger migration
    * Good idea to have a pilot partner even if not preserving separate endpoints for new API 

* Cons:
  * Transparent cutover not an option; new API must have different path
  * Will require tests for both APIs to be maintained simultaneously
  * Will likely require some level of reonboarding for all users of RS
  * Can both APIs coexist on the same listening port? Would this require the functionapp to act as a passthrough?


Should support for SMART on FHIR be considered?
* Further reading: https://www.okta.com/resources/whitepaper/smart-on-fhir-with-okta/
