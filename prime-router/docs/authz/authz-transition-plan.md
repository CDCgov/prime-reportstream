This document details the steps needed to successfully transition from the current RS auth to the auth microservice.

Migrating to microservices will be performed in two stages. During the intermediate (first) stage, all endpoints handled
by the functionapp will remain as they are while the microservices are prepared; all existing endpoints will remain
supported as well as the new endpoints provided by the auth microservice as it is developed. 
After we have completed architectural changes and successfully migrated all senders we will reach the final stage, where
all public communication is handled through the auth microservice and the functionapp will no longer be accessed publicly.

Please consult [this Lucidchart](http://lucidgov.app/lucidchart/1ad27194-a283-4a38-85e4-132d7e9cf5e3/edit?page=~G0RZ44nv8oX)
for a visualization of the intended connectivity strategy for each stage.

# Migration Strategy #

The migration process can be divided to the following tasks. For the most part these tasks can be worked on in any order
and could be worked on in parallel.

### Build Okta related endpoints and application profile update function ###
The auth microservice should be updated to provide the endpoints for Okta event hooks to push events to as well as the
endpoint for application users to acquire bearer tokens via Okta. The ability to update application user profiles should
also be built at this time, and hooked in to the Okta event hook API. We should also allow application user profile
updates via CLI.

### Build passthrough API for functionapp APIs via auth microservice ###
Calls to the APIs served via the functionapp (reports/waters) should be able to pass through the auth microservice so 
that it will no longer be necessary to expose the functionapp to the internet after migration tasks are completed. 
Requests should be passed verbatim, including the bearer token received from the requester. Direct public access of the 
functionapp endpoints shall remain supported until all senders are migrated to the AuthN service.

### Update end to end testing and development environments to include auth and submissions ###
We should build a new test based on `end2end_up` that performs submission through the microservices instead of the
functionapp. We should also aim for the new smoke test to be performed by a GitHub action rather than be executed from a
developer machine; this will require storing secrets for this purpose. Gradle tasks and Dockerfiles should be updated
so that auth and submissions are built and run in the development environment. Changes should be sufficiently documented
for DevSecOps' use.

### Integrate auth and submissions microservices to CI processes ###
We should begin including the microservices projects in our continuous integration builds, set up the API endpoints in
the staging and production environments, and make sure the microservices are executed. This will also require setting up
environment-specific secrets for connecting with Okta and ensuring we have set up permanent application users for the 
microservices. By the end of this work both staging and production Okta should have their final access applications; the 
names of the application and the method by which the secrets are passed to the microservices should be documented at 
this point. This work also requires any proof of concept code to be removed and any outstanding security concerns (e.g.
CORS) addressed.

### Create application users for senders and begin migration outreach ###
Senders are represented as application users. Creating the application user will produce a client ID and a private key
that the sender will use to authenticate to the microservices. Begin outreach to at least one sender to have them begin
using the microservices to submit instead of the functionapp.

### Add group and scope assignments to existing Okta users ###
Existing Okta users can be assigned to groups and scopes within Okta without affecting their current authentication.
Therefore, we should begin to make these changes in advance of altering the frontend to use the new claims structure.
For example, an existing user with organization scope `DHmd-phdAdmins` would be added to the `md-phd` group and assigned
the `admin` scope. New users created after beginning this effort will need to be onboarded with both the new and 
deprecated scopes. If there are a very large number of users we may want to consider building a script that retrieves 
the users via the Okta API then programmatically derives and applies the equivalent scopes.

### Update functionapp APIs to utilize new authorization and authentication ###
All APIs in the functionapp should be updated to use the updated authentication and authorization design. In the process
we should evaluate the authorization requirements for each endpoint. We should consider splitting this effort as there
are as many as 58 endpoints to be evaluated and updated. This work will need to be performed in a feature branch in
order to not introduce breaking changes before the frontend API connections are updated.

### Update frontend API connections ###
Once all Okta users and functionapp APIs have been updated as outlined above, all references to the RS API are changed
to be directed to the auth microservice. This involves updating the Okta OAuth API to the new bearer token format.

### Retire API access to the functionapp ###
Once all senders and users have been provisioned in Okta using the updated structures we can remove network access to
the functionapp. All communication from senders or the frontend should occur through the auth microservice. We should
also revisit integration and smoke tests at this point and ensure testing can be performed solely via calls to the auth
microservice.

### Implement sender setup API and frontend (tentative) ###
A proposed API for creating senders entirely within RS is included in the design. All configurations needed for senders
can be performed from the Okta admin console, so this is not strictly necessary for migration. If desired, the 
implementation of this API and the related frontend can occur in parallel to the other migration tasks.

# Dev notes #
These are miscellaneous dev notes that should be considered during the implementation process.

* Migrate all senders in staging
    * Can existing public/private keys be transferred to Okta, or will all senders have to be fully reonboarded?
* Implement sending profile attributes in claims (API > ReportStream API > Claims > create `appSubmit` claim)
* API changes for new auth flow
  * Which Azure functions require authorization processes?
  * Sender onboarded to use auth microservice will have significantly different authorization flow from original API -
    need strategy for how authorization decisions are made when passing API calls from microservices to functionapp
    * Could use some sort of translation function to bridge new authorization to old in order to support legacy
      endpoint before all senders are reonboarded
    * Alternative would be to handle all authorization on auth microservice and bypass authorization on functionapp
      end when auth microservice is the caller 
* Should sender groups be stored as Okta groups or solely as profile attributes?
    * Utilizing groups permits onboarding via Okta admin console, but the group memberships are redundant as it is only
      the profile attributes that are actively used during the authz process
      * Event hook strategy should keep group memberships and profile attributes in sync without much upkeep 
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
* Users: Process to migrate existing user authorizations
    * Map existing RS scopes to Okta scopes
    * How to restrict users to information related to their organization
* Frontend: We *could* hard cut APIs consumed by the frontend only - do not need to preserve current OktaAuthentication 
  * Do we need to verify token with introspect endpoint? Access token verifier utilizes JWKS keys to verify token is valid
  * Heavy lift is changing authorization - requires user scopes to be updated
  * The http request is still available to AuthenticatedClaims so the jwt could be re-extracted
  * Make claim strategy decision based on jwt content?
  * Document what authorization each endpoint requires
    * plan to split up work, there are 58 known endpoints
  * Senders not migrated are not affected as they are not currently using Okta auth
    * Senders that migrate to Okta auth will have different claims structure - endpoints called in a server to server
      context will require supporting both authorization schemes  

## Other Questions ##

A phased cutover during which both old and new endpoints are available was determined to be the only practical option,
as a hard cutover would require a level of coordination with external partners that is not feasible. Still, we needed to
consider what would be required to do this:

* Pros for maintaining separate endpoints:
  * Flexible timeline for transitioning senders; no hard cutoff date, don't have to migrate all at once
    * Coordinating a cutover date with external partners may not be feasible
  * Can designate a pilot partner before initiating a larger migration
    * Good idea to have a pilot partner even if not preserving separate endpoints for new API 

* Cons:
  * Transparent cutover not an option; new API must have different path
  * Will require implementation and tests for both APIs to be maintained simultaneously
  * Can both APIs coexist on the same listening port? Would this require the functionapp to act as a passthrough?


What is the extent to which handling of receivers needs to change, if at all?
* Receiver API keys are currently stored in the database
* REST Transport uses a self-contained OAuth implementation
* Complication is mostly in setting up web users and authorization for sender specific API calls

Should support for SMART on FHIR be considered?
* Further reading: https://www.okta.com/resources/whitepaper/smart-on-fhir-with-okta/
