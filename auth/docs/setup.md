# Running the Auth Microservice

## Prerequisites

A few secrets are required to run the Auth which are not committed to source. These values are
configured in Okta.

| Environment variable | Value                           |
|----------------------|---------------------------------|
| OKTA_ADMIN_CLIENT_API_ENCODED_PRIVATE_KEY | Base 64 encoded private key pem |
| SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_CLIENT_SECRET | Base 64 encoded secret |

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
    - [See Okta documentation on that endpoint here](https://developer.okta.com/docs/guides/implement-oauth-for-okta-serviceapp/main/#get-an-access-token)
- Submit your report to http://localhost:9000/api/v1/reports
  - Note the it's port 9000 which is auth rather than directly to 8880 which is submissions
  - See endpoint definition in [SubmissionController](../../submissions/src/main/kotlin/gov/cdc/prime/reportstream/submissions/controllers/SubmissionController.kt)
  - Add the access token you retrieved from Okta as a `Bearer` token in the `Authorization` header
- Inspect the logs if you received a 401 or a 403. This indicates there is an issue with your access token.

## Notes on secrets

The Okta-Groups JWT signing key pair has a local dev value already set up appropriately in auth and 
downstream in submissions. New values _must_ be generated for deployed environments. You can look
at [KeyGenerationUtils](../src/test/kotlin/gov/cdc/prime/reportstream/auth/util/KeyGenerationUtils.kt) 
for scripts to run to generate new keys.

By Default, we are connecting to the Staging Okta. We cannot post connection secrets directly in this document so
you will have to ask someone for those values.
