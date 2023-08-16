# Receive
`Receive` is the first step a message goes through when entering the Universal Pipeline (UP). Despite its name, this step
actually occurs in the `ReportFunction.kt` file. 

There are two calls that will kick off this function:
```
POST /api/reports
POST /api/waters
```

`\reports` - old API, does not really have security other than checking that the sender exists. 
Still widely used.

`\waters` - new API, uses FHIR auth security

Currently, the UP can receive data in either HL7 or FHIR. We are planning to support CSV in the future.

## Validation
One of the most important things the `receive` step does is validate the message.
There are big plans for validation in the works (see the [design doc](../design/design/validation/design.md)). Currently 
though, we do very basic validation, like ensuring that it is a type of message we can process. Moving forward, we 
want to validate messages against what are called an implementation guide. These specify a standard for a message type and 
are available for both HL7 messages and FHIR messages. For example, `RADx Mars` is an implementation guide for HL7 V2 and 
`US Core` is an implementation guide for FHIR.


