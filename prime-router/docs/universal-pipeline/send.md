# Send

This is the final step in the pipeline and is responsible for actually delivering the batched report to a receivers
preferred location

## Configuration

The primary configuration for this step is a transport type for a receiver.  There are many transport options available, each with their own configuration options:

- AS2 (currently unused) [protocol](https://en.wikipedia.org/wiki/AS2) for transporting business data. Requires credentials for the receiver to have been stored in the vault
  - `receiverUrl`:  The URL to send the report
  - `receiverId`: The configured ID for where the report is sent to
  - `senderId`: The configured ID for the report stream application
  - `senderEmail`: The email address of the sender to include in the request, default is "reportstream@cdc.gov"
  - `mimeType`: The mimetype to send with the report, default is "application/hl7-v2"
  - `contentDescription`: the description to send with report
- BlobStore used to move items between different azure storage accounts and containers within the CDC azure
  - `storageName`: the name of the azure storage account
  - `containerName`: the name of the azure container in that storage acount
- Email used to send a report an email address
  - `addresses`: list of email addresses to send report to
  - `from`: who the email should be from, default "qtv1@cdc.gov"
- GAENT (Google/Apple Exposure Notification) send the report to the Google or Apple notification API
  - `apiUrl`: the url to make the POST to
  - `uuidFormat`: the format of the UUID that the APIs use to deduplicate messages
  - `uuidIV`: HMAC initialize vector, required if the format is `WA_NOTIFY`
- Null does not send the report anywhere
- Rest sends the report to a configured rest endpoint, credentials are stored in the vault and can be configured as username/password, API key or client_id/secret
  - `reportUrl`: the URL to POST the report to
  - `authTokenUrl`: the URL to get the OAuth token from
  - `authType`: the kind of authentication, when set to `two-legged` will generate a JWT 
  - `tlsKeyStore`: the name of the credential manager to use
  - `parameters`: map of parameters to be included
  - `headers`: map of headres to be included
- SFTP send the report over a file transport to a configured SFTP server, credential must be stored in the vault
  - `host`: the url for the SFTP server
  - `port`: the port the SFTP server runs on
  - `filePath`: what path on the SFTP server the report should be placed in
  - `credentialName`: the name of the credential that should be used for authenticating the request
- Soap (only works for PA) send the report to a SOAP endpoint, credential must be configured in the vault
  - `endpoint`: the URL to send the SOAP post to
  - `soapAction`: which SOAP action to invoke
  - `credentialName`: the name of the credential to find in the vault
  - `namespace`: the optional namespace in the SOAP object

Example for an SFTP configuration (most common):
```yaml
transport:
  type: SFTP
  host: sftp
  port: 22
  filePath: ./upload
  credentialName: DEFAULT-SFTP
```

## How it works

This step works by listening for messages to get added to the send queue and then looking up the transport that is configured
for the receiver.  Each transport has its own internal logic for creating the report and sending it out, but they mostly all follow
the similar pattern of lookup the credentials for that transport/receiver, create an external file name, send the contents of the report.

**Some important notes on the transports:**
- The AS2 transport is unused
- The SOAP transport is hardcoded to only work with the state of Pennsylvania
- The Email transport is incomplete and does not work
- The Rest Transport uses the KTOR library under the hood
- The BlobStore transport only supports azure as the backing blob store and should only be used to move blobs internally in the CDC

### Retries

All the transports are configured to catch any thrown exception and return a retry token of all items.  When a retry token is returned, the SendFunction
looks to see if it should re-queue the event for another attempt at getting sent.  The SendFunction will attempt to send the report the 5 times 
using an exponential back off with a random amount of time appended; this is done in order to prevent a huge slew of send events all being 
triggered at the same time in the event of a large outage.

Under the hood, the retry functionality takes advantage of the `visibilityTimeout` parameter of azure queue messages that keeps a message
invisible for a configured amount of time.

## Code entry points

- [SendFunction](https://github.com/CDCgov/prime-reportstream/blob/6f28db462ae9623d46486a45e8ce0b356e92dd05/prime-router/src/main/kotlin/azure/SendFunction.kt#L56)
- [ITransport](https://github.com/CDCgov/prime-reportstream/blob/6f28db462ae9623d46486a45e8ce0b356e92dd05/prime-router/src/main/kotlin/transport/ITransport.kt#L9)
- [TransportType](https://github.com/CDCgov/prime-reportstream/blob/6f28db462ae9623d46486a45e8ce0b356e92dd05/prime-router/src/main/kotlin/TransportType.kt#L22)

## Examples


Retry mechanisms (in case of send failures)

- This is already included within the [Getting Started > Common Failures & Fixes > Message Fails to Send](https://docs.google.com/document/d/1Xr2defgjUjarZgyROqOqM1Cezx_hp6S_Gd3Ue8bik64/edit#heading=h.3ahv06k8tiv9) section. Make sure to link to that content to prevent writing it twice.   