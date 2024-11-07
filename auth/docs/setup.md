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
# start report stream and all dependant docker containers
./gradlew quickRun
# start submissions service
./ gradlew submissions:bootRun
# start auth service
./gradlew auth:bootRun
```

## Submitting reports locally

- Retrieve an access token directly from Okta and ensure the JWT contains the "sender" scope
- Submit your report to http://localhost:9000/api/v1/reports
  - Note the it's port 9000 which is auth rather than directly to 8880 which is submissions
  - See endpoint definition in [SubmissionController](../../submissions/src/main/kotlin/gov/cdc/prime/reportstream/submissions/controllers/SubmissionController.kt)
- Inspect the logs if you received a 401 or a 403. This indicates there is an issue with your access token.

## Notes on secrets

The Okta-Groups JWT signing key pair has a local dev value already set up appropriately in auth and 
downstream in submissions. New values _must_ be generated for deployed environments. You can look
at [KeyGenerationUtils](../src/test/kotlin/gov/cdc/prime/reportstream/auth/util/KeyGenerationUtils.kt) 
for scripts to run to generate new keys.

By Default, we are connecting to the Staging Okta. We cannot post connection secrets directly in this document so
you will have to ask someone for those values.
