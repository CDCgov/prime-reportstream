## Background

Note:  Click here for the [FHIR Auth Implementation Plan](#initial-implementation-plan)

The PRIME ReportStream1] aims to work with multiple senders or third-parties clients that send reports to the Data-Hub. Each of these clients will need to authenticate with the Data-Hub API. This paper outlines a proposal on how these clients should provide credentials to the Data-Hub.

Today, in healthcare and public health, the most common form of authentication between two computers, also known as service to service authentication, is either a shared secret or a form of TLS mutual authentication. An SFTP service authenticated by username and password is an example of the shared secret method. The API secret key is another form of a shared secret. PHIN MS is an example of an authentication mechanism built on TLS mutual authentication.

The sharing of usernames and passwords or secret keys is problematic from a security standpoint. Passwords are frequently compromised because the act of sharing a secret makes them vulnerable. A good security practice is to put a short time limit on the validity of a secret. This fix leads to operational problems as someone has to be involved when a secret expires and when a new secret is shared.

At first glance, mutual TLS may be a more secure alternative to a shared secret form of authentication. However, mutual TLS still suffers from the same operational issues as a shared secret. Since a private key may still leak, a good security practice is to rotate certificates yearly. Like refreshing a shared secret, this certificate rolling process requires coordination between both parties. If not done correctly, it can lead to service outages. The certificate change process is cumbersome when planned, but if it has to be done on a rapid basis (for example, to mitigate leaks of the private key), it is hard to avoid service disruption.

## Goals

Fortunately, the problems that I just outlined are well known. There are available computer authentication standards that address these issues. As we think about the Data-Hub and its possible place in the public health infrastructure, the hub could try to join those trying to advance how public health authorities communicate. My goals are:
- Align with developing standards in healthcare interoperability
- No sharing of secrets and keys between organizations
- Ability to roll keys without service disruption and coordination
- Support multiple clients per organization
- The ability to scale to 100s of third parties.

##Proposal

The basic proposal has two parts. First, use JSON Web Tokens (JWT) with a private key signature (aka Private Key JWT) [2] for the Data-Hub authentication token. This standard avoids the shared secrets problems of usernames and passwords or secret keys by using public-key cryptography mechanisms. Second, provide the client's public keys in a JSON Web Key Set (JWKS) that the client's website hosts. The Data-Hub would store the URL to the client's JWKS. Hosting the JWKS on the client's website allows the client to change their public and private keys without coordination with the data-hub.

The inspiration to use the combination of Private Key JWT and hosted JWKS came from the FHIR Bulk Data Access standard. The guide explains the details of how to combine these two web standards. Although the FHIR authorization standard lists two options for public key exchange, the Data-Hub will strongly encourage registering a JWKS URL because that will lead to lower maintenance costs. Here's a sequence diagram from the FHIR guide that illustrates the authentication sequence.

![Authentication Flow](../assets/backend-service-authorization-diagram.png "Logo Title Text 1")

In this proposal, I'm encouraging that we adopt JWKS URLs because it has lower operational costs in the long term. Healthcare interoperability will work better if it adopts this standard. In the short-term, it involves more setup work. I realize that it may be something better suited as an option.

Note:  an example implementation is the [Data at the Point of Care project](https://github.com/CMSgov/dpc-app/tree/master/dpc-api/src/main/java/gov/cms/dpc/api/auth)

## Initial Implementation Plan

We are working on implementing FHIR-style Authentication for our api/waters endpoint.   Our human users will auth using Okta, so FHIR auth is only needed for server-to-server connections.

### A little background information:

- In ReportStream, an `Organization` is an external entity that can send and/or receive data to/from ReportStream.  SimpleReport, and the Florida Public Health Department, are examples of `Organization`s.   Each `Organization` has any number of `Sender` and `Receiver` configurations.
- The submitted unit of work from a `Sender` is a `Report`, which is a set of medical data, eg, covid test results for one or more patients.  We receive it via the content payload of a POST to our `api/waters` endpoint.
- A `Sender` is a configuration within an `Organization` that sends `Report`s in one specific schema in one specific format.  For example, the SimpleReport `Organization` has a `Sender` called 'default' that sends in the `primedatainput/pdi-covid-19` schema in the `CSV` format.
- Initially, the FHIR auth project isn't concerned with `Receiver`s, but for completeness:  A `Receiver` is a configuration within an Organization that receives data in a specific schema in a specific format, and with specific timing, transport mechanism, and with specific filters based on jurisdiction.

For this initial project, we are implementing FHIR style authentication for a `Sender` at an `Organization` to submit `Reports`.

### Three Steps

To make FHIR auth work, there are three main interactions that have to happen between ReportStream, and `Sender`s, following the FHIR Auth style.  Implementation details for these steps are found below.
1. PreAuth / Setup
2. Signature Verification
3. Actual API usage.

Note: We may also implement additional protection mechanisms (eg, whitelisting of Sender IPs) at the Azure / network / firewall level.  These are not covered here.

### Token Naming Conventions

Here is our token naming conventions, to avoid confusion:

- "Okta token" is created by Okta, and is used to access the settings API.  Okta is not directly involved in the FHIR auth at all.   This API is used by us to store the Sender's public key in our database.  While this public key does not need to be managed as a secret, the ability to change it must be carefully controlled.
- "Sender token":  is the JWT, signed by the Sender using their private key, to do initial authentication.   
- "ReportStream token" is the token provided in response to an authenticated Sender token, that is then used by the Sender to submit data.  The ReportStream token will just be a signed JWT, signed with a private key, in our vault.   A timer will be built into the token.  Thus FHIR auth only requires one additional secret to be stored in our vault.

How will we implement each step?

Logging note:  we will never log token or public/private key values.

### Step One:  Setup: Sender Pre-authorization Step.   We need to pre-register information about the sender.

During the on-boarding of a new Organization, a PRIME team member will set up a new organization in our system via our `settings` API.  In addition, a new Group will be created in Okta for that Organization.  Obviously authorization to do this work will be using Okta token auth, which is already working, and outside the scope of this doc.

Settings are stored internal to ReportStream in a Postgres database, in the SETTING table, denormalized as a JSON structure per each Organization, Sender, and Reciever.

To support FHIR Auth, we will add an add auth info to the existing Sender class, APISender, and the yml in the Settings table.  No database changes needed.  No API or CLI changes needed.
The new auth info will include:
- The Sender's JSON Web Key Set (JWK Set)
- (for a Phase 2:  Sender's Public Key URL)
- Note: we will use the Sender's fullName (aka orgName.senderName) as the unique client_id, per the FHIR spec.

We'll need to write a document for our Senders, explaining how to generate a public/private key, and how to keep the private key secure.   Or better, we can write a web tool that immediately stores the public key on our side, and gives the customer the private key.  (Google APIs have a nice setup like this.)  We'll need a process for rotating keys, if the Sender is not using the Public Key URL.   Since we have relatively few senders, I think there's not a high priority on automating that right now.  We will need to track contact information of the person at the Sender who is responsible for the keypair.  This may not be the same person who handles the actual test data/results.

The sender can give us multiple keys in the JWK Set

In this initial release, we'll require submission to the settings API as properly formatted JWK Set.   However, in the future, we may want to just accept a base64 encoded public key submission, and do the conversion to a JWK as a convenience.

### Step Two: Signature Verification Step: Sender sends signed JWT.  If valid, response is the ReportStream token.

We'll need to document for our Senders how to generate the 'Sender Token', the signed JWT they send to us, generated using their private key.

The Sender Token will be sent as a standard "Authorization: Bearer <token>" header. 

We'll support both RS384 and ES384 web signature algorithms.

(Phase Two Note):  The sender can also send the URL with the signed JWT, as part of the request.  Per the FHIR protocol, we must check that against the URL originally provided at the time of PreAuth.  If it differs, we reject.

(Phase Two Note): How will we handle cache-control headers? Ans:  Per FHIR protocol, we should follow timeline in any cache-control request from the sender.

We will 
1. lookup and validate the client ID
2  validate the JWT signature against the public key we have stored in the settings table for that client_id, Key ID (`kid`).  We'll use the [JJWT libraries](https://github.com/jwtk/jjwt) to do JWT verification. (?)
2. check that the JWT has not been previously encountered within the max JWT lifetime (5 minutes)

We will then pull our ReportStream shared secret from our vault, and use it to create and sign a ReportStream token, for use by the Sender.   FHIR calls for a 5 minute end of life.

### Step Three: Actual API Usage :  Sender uses the token to submit data to `api/waters`

The current 'FUNCTION' level AuthorizationLevel will be changed to 'ANONYMOUS'. (See https://docs.microsoft.com/en-us/azure/azure-functions/functions-bindings-http-webhook-trigger?tabs=csharp#secure-an-http-endpoint-in-production)

The ReportFunction will check the ReportStream token upon entry to the function code.  That is, we will not implement a 'gateway' function in front or api/waters.   (And, as mentioned earlier, no dependency on Okta).  This check will simply be a validation of the signed JWT.  This will require pulling the ReportStream shared secret from the vault.  TBD whether this is a performance issue, or whether we'll have to make adjustments because of known rare connection failures to the vault.

The ReportStream Token will be sent as a standard "Authorization: Bearer <token>" header. 

ReportFunction will then validate the certificate, and allow or deny access.  If the time has expired, access will be denied.  UNAUTHORIZED Http Status will be returned.  Note:  No JSON will be returned in these cases (?)

For our initial release, there is only one scope associated with the token - the only scope is the ability to upload reports into the api/waters endpoint.

Note: there are no refresh tokens with servert-to-server auths.   After 5 mins, sender must go back to step 2.


## References
[1] Prime Data Hub https://github.com/cdcgov/prime-data-hub

[2] FHIR Bulk Data Access http://hl7.org/fhir/uv/bulkdata/authorization/index.html
