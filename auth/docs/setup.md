# Running the Auth Microservice

## Prerequisites

A few secrets are required to run the Auth which are not committed to source. These values are
configured in Okta. We have to set up applications with properly scoped permissions to allow our
application to properly retrieve that data from Okta.

| Environment variable                                            | Value                           |
|-----------------------------------------------------------------|---------------------------------|
| OKTA_ADMIN_CLIENT_API_ENCODED_PRIVATE_KEY                       | Base 64 encoded private key pem |
| SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_CLIENT_SECRET | Shared secret                   |

Setup for `OKTA_ADMIN_CLIENT_API_ENCODED_PRIVATE_KEY`:
1. Sign into Okta Admin Portal
2. Go to Applications > Applications in the sidebar
3. Click "Create a new app integration" button
4. Select "API Services" radio button and click "Next"
5. Give it an appropriate name like "Authn Admin Integration"
6. Select "Public key / Private key" as the Client authentication method
7. Copy client ID and private key in PEM format somewhere 
8. Grant the application the `okta.apps.read` scope 
9. Grant the application the `Read-only Administrator role`
10. Set client id at `okta.adminClient.clientId` in application.yml 
11. Base 64 encode the private key PEM and set it in the environment variable `OKTA_ADMIN_CLIENT_API_ENCODED_PRIVATE_KEY`

Setup for `SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_CLIENT_SECRET`:
1. Sign into Okta Admin Portal
2. Go to Applications > Applications in the sidebar
3. Click "Create a new app integration" button
4. Select "API Services" radio button and click "Next"
5. Give it an appropriate name like "Authn Token Integration"
6. Select "Client secret" as the Client authentication method 
7. Copy client ID and client secret string somewhere 
8. Grant the application the `okta.apiTokens.read` scope 
9. Set client id at `spring.security.oauth2.resourceserver.opaquetoken.client-id` in application.yml 
10. Set secret in the environment variable `SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_CLIENT_SECRET`

## How to run application locally

```bash
# from project root
# start ReportStream and all dependent docker containers
./gradlew quickRun
# start submissions service
./ gradlew submissions:bootRun
# start auth service
./gradlew auth:bootRun
```

## Setup a Sender

- Sign in to Admin Okta
- Applications -> Application tab
- Click "Create App Integration"
- Select "API Services" and click next
- Name your sender
- Copy your client ID and client secret or private key locally to be used while calling the /token endpoint
- Add the user to the appropriate sender group
  - You can find this option on the small gear next to your newly created application
  - Ensure the group has the prefix DHSender_

## Submitting reports locally

- Retrieve an access token directly from Okta and ensure the JWT contains the "sender" scope
  - Make a well-formed request to https://reportstream.oktapreview.com/oauth2/default/v1/token to retrieve your access token
    - See [sample code](../src/scripts/get_client_access_token.py)
      - `authorization_server_id` defined in Security > API in Okta admin; must match spring.security.oauth2.resourceserver.opaquetoken.introspection-url
    - [See Okta documentation on that endpoint here](https://developer.okta.com/docs/guides/implement-oauth-for-okta-serviceapp/main/#get-an-access-token)
- Submit your report to http://localhost:9000/api/v1/reports
  - Note API is on port 9000 which is auth rather than directly to submissions on port 8880
  - See endpoint definition in [SubmissionController](../../submissions/src/main/kotlin/gov/cdc/prime/reportstream/submissions/controllers/SubmissionController.kt)
  - Required headers:
    - `Content-Type`: the content type of the report (must be "application/hl7-v2" or "application/fhir+ndjson")
    - `clientId`: the ID of the client submitting the report. Should represent org.senderName
    - `content-length`
    - `x-azure-clientip`
  - Content of report (HL7, FHIR) in body of post request 
  - Add the access token you retrieved from Okta as a `Bearer` token in the `Authorization` header
- Inspect the logs if you received a 401 or a 403. This indicates there is an issue with your access token.

## Notes on secrets

The Okta-Groups JWT signing key pair has a local dev value already set up appropriately in auth and 
downstream in submissions. New values _must_ be generated for deployed environments. You can look
at [KeyGenerationUtils](../src/test/kotlin/gov/cdc/prime/reportstream/auth/util/KeyGenerationUtils.kt) 
for scripts to run to generate new keys.

By Default, we are connecting to the Staging Okta. We cannot post connection secrets directly in this document so
you will have to ask someone for those values.
