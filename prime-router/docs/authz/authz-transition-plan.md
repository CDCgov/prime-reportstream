This document will detail the steps needed to successfully transition from the current RS auth to the auth microservice.

# Preparation #

* Implement profile groups (API > ReportStream API > Claims > sender_name)
* API changes for new auth flow
  * Need to decide if cutting over or maintaining both APIs - see analysis in Questions section 
* Should sender groups be stored as Okta groups or solely as profile attributes? 
  * Utilizing groups permits onboarding via Okta admin console, but the group memberships are redundant as it is only
    the profile attributes that are actively used during the authz process
  * Storing solely as profile attributes would require the sender management APIs to be built before transition to the
    auth service can occur
  * We can change this strategy at any time
* Create test workflow in staging representative of production environment
  * Update workflow documentation to create new sender
* Add auth/submissions endpoints to deployment and CI processes
  * auth and submissions projects currently ignored
  * end2end testing for endpoints should be created
    * Consider how developer environment setup needs to change 
  * Dependabot already hooked, but need to verify configuration and confirm what other integrations are needed (DevOps)
* Create Okta applications/APIs in production Okta (API, groups)
  * Consider how to run a test in production 
* Add integration/smoke tests to include end to end test of auth/submissions
  * New tests alongside existing tests for original report API until it is removed?
* Migrate internal functions utilizing deprecated authentication
  * `prime login` uses Okta user login flow; need application user flow to allow machine to machine authentication (CI
    runners can log in - the ultimate goal should be allowing smoke tests to run without needing a user web login)
  * Smoke tests against staging additionally rely on an Azure functions key and an authorization key stored in
    Environment variables
* Which Azure functions require authorization processes?


# Migration Strategy #

### Build Okta related endpoints and application profile update function ###
Microservices should be updated to provide the endpoints for Okta event hooks to push events to as well as the endpoint
for application users to acquire bearer tokens via Okta. The ability to update application user profiles should also be
built at this time, and hooked in to the Okta event hook API. We should also allow application user profile updates via 
CLI.

### Integrate auth and submissions microservices to CI processes ###
We should begin including the microservices projects in our continuous integration builds, set up the API endpoints in
the staging environment, and make sure the microservices are executed. This will also require setting up environment-
specific secrets for connecting with Okta and ensuring we have set up permanent application users for the microservices.

### Update end to end testing and development environments to include auth and submissions ###
We should build a new test based on `end2end_up` that performs submission through the microservices instead of the
functionapp. We will need to determine if this test should integrate with Okta or if Okta connections should be mocked
in the absence of an offline test container. We should also aim for the new smoke test to be performed by a GitHub
action rather than be executed from a developer machine; this will require storing secrets for this purpose.

### Create application users for senders and begin migration outreach ###
Senders are represented as application users. Creating the application user will produce a client ID and a private key
that the sender will use to authenticate to the microservices. Begin outreach to at least one sender to have them begin
using the microservices to submit instead of the functionapp.

### Add group and scope assignments to existing Okta users ###
Existing Okta users can be assigned to groups and scopes within Okta without affecting their current authentication.
Therefore, we should begin to make these changes in advance of altering the frontend to use the new claims structure.
For example, an existing user with organization scope `DHmd-phdAdmins` would be added to the `md-phd` group and assigned
the `admin` scope. New users created after beginning this effort will need to be onboarded with both the new and 
deprecated scopes.

### Update frontend claims authorization handling and API connections ###
The frontend is changed to process user authorization based on the claim structure outlined in the design. After this is
accomplished, the deprecated organization scopes can be retired. All references to the RS API are changed to be directed
to the auth microservice. 

### Retire API access to the functionapp ###
Once all senders and users have been provisioned in Okta using the updated structures we can remove network access to
the functionapp. All communication from senders or the frontend should occur through the auth microservice.

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

* Pros for maintaining separate endpoints:
  * Flexible timeline for transitioning senders; no hard cutoff date, don't have to migrate all at once
    * Coordinating a cutover date with external partners may not be feasible
  * Can designate a pilot partner before initiating a larger migration
    * Good idea to have a pilot partner even if not preserving separate endpoints for new API 

* Cons:
  * Transparent cutover not an option; new API must have different path
  * Will require implementation and tests for both APIs to be maintained simultaneously
  * Will likely require some level of reonboarding for all users of RS
  * Can both APIs coexist on the same listening port? Would this require the functionapp to act as a passthrough?


Should support for SMART on FHIR be considered?
* Further reading: https://www.okta.com/resources/whitepaper/smart-on-fhir-with-okta/
