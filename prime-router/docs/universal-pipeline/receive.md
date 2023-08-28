# Universal Pipeline Receive Step
`Receive` is the first step a message goes through when entering the Universal Pipeline (UP). Despite its name, this step
actually occurs in the `ReportFunction.kt` file. 

## How it works
There are two calls that will kick off this function, see the Swagger doc for specifics: 
http://localhost:8080/swagger-ui.html. Currently, the UP can receive data in either HL7 or FHIR. We are planning to 
support CSV in the future.

### FHIR Auth Security
This is used in the `\waters` endpoint. It retrieves the access token from the request then it will attempt to 
authenticate with Okta. If that does not work then it will try Server2Server auth. If that does not work it will deem 
you Unauthorized.

### Validation
Currently, we do very basic validation in the `receive` step, like ensuring that it is a type of message we can process.
We also check for duplicates and, if they are not allowed via the `allowDuplicate` param, reject the message. 
We check that the sender is valid and enabled.

The last thing it does is upload the message to the blob store and triggers the convert step via the Azure queue. 

