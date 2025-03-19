This document will detail the steps needed to successfully transition from the current RS auth to the auth microservice.

# Preparation #

* Implement profile groups (API > ReportStream API > Claims > sender_name)
* API changes for new auth flow
  * Need to decide if cutting over or maintaining both APIs - see analysis in Questions section 
* Create test workflow in staging representative of production environment
  * Update workflow documentation to create new sender
* Create Okta applications/APIs in production Okta (API, groups)
  * Consider how to run a test in production 
* Add integration/smoke tests to include end to end test of auth/submissions
  * New tests alongside existing tests for original report API until it is removed?

# Migration Strategy #

## Migration of senders and website users ##
* Migrate all senders in staging
  * Can existing public keys be transferred to Okta, or will all senders have to be fully reonboarded?
  * Do we intend to run both auth systems simultaneously?
* Users: Process to migrate existing user authorizations
    * Map existing RS scopes to Okta scopes
    * How to restrict users to information related to their organization
* Do sender and user migrations need to be performed simultaneously?

## Other Questions ##

Do we need to maintain both auth systems simultaneously?

* Pros:
  * Flexible timeline for transitioning senders; don't have to migrate all at once
  * Can designate a pilot partner

* Cons:
  * Transparent cutover not an option; new API must have different path
  * Will require tests for both APIs to be maintained simultaneously
  * Can both APIs coexist on the same listening port? Would this require the functionapp to act as a passthrough?


Should support for SMART on FHIR be considered?
* Further reading: https://www.okta.com/resources/whitepaper/smart-on-fhir-with-okta/
