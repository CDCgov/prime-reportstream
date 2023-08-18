# Receive
`Receive` is the first step a message goes through when entering the Universal Pipeline (UP). Despite its name, this step
actually occurs in the `ReportFunction.kt` file. 

## How it works
There are two calls that will kick off this function:
```
POST /api/reports
POST /api/waters
```

`\reports` - old API, does not really have security other than checking that the sender exists. 
Still widely used.
`\waters` - new API, uses [FHIR auth security](#fhir-auth-security)

The parameters are the same for both endpoints. 
Expected Parameters:
- `client` - the sender name
- `topic` - the topic that the message is applicable to
  Optional Parameters:
- `allowDuplicate` - indicates if you want us to send messages that we flag as duplicates
- `option` - currently can pass the following values:
  - `None` - normal workflow
  - `ValidatePayload` -  validate the payload, but do not process. return 200 on OK or 400 on fail
  - `CheckConnections` - health check, returns 200
  - `SkipSend` - validate and route but do not send reports. Data is kept in the hub.
  - `SendImmediately` - send the reports immediately, skip batching and timing
- `payloadname` - the message will be associated with the payloadName with the submission as part of its history/lineage
tracking, so that users can map submissions back to original filenames.
- `default` - helps set some defaults for validation
- `routeTo` - allows the sender to specify the receiver(s)
- `format` - format of the message

Currently, the UP can receive data in either HL7 or FHIR. We are planning to support CSV in the future.

### FHIR Auth Security
This is used in the `\waters` endpoint. It retrieves the access token from the request then it will attempt to 
authenticate with Okta. If that does not work then it will try Server2Server auth. If that does not work it will deem 
you Unauthorized. 

### Async/Sync
UP pipeline is always asynchronous, but this branching logic is here since the code is shared with the legacy pipeline.

### Validation
Currently, we do very basic validation in the `receive` step, like ensuring that it is a type of message we can process 
and breaking each message out into a separate message if it is a batch message. We also check for duplicates and, if 
they are not allowed via the `allowDuplicate` param, reject the message. We check that the sender is valid and enabled.

The last thing it does it upload the message to the blob store and triggers the convert step via the Azure queue. 

