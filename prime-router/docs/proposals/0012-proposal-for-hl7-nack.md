# DRAFT PROPOSAL - HL7 NACK Message Endpoint

## Context and Motivation
As we continue to work in the space of sending data on to our public health department partners
it would be valuable for us to be able to accept `NAK` messages from health departments that 
attempt to process our messages but for some reason are not able to.

HL7 is structured around two main ideas:
- Events
- Segments

The only event we currently process is the `ORU_R01` event, which is the type of message for
["unsolicited transmission of an observation message"](https://hl7-definition.caristix.com/v2/HL7v2.5.1/TriggerEvents/ORU_R01).

There are many other message types however, for many other types of events in health care, including the `ACK`.

An `ACK` message is used by a recipient of an HL7 message to acknowledge it was received, as well as 
return any errors that came up during the processing.

An example successful `ACK` message looks like this:

```text
MSH|^~\&|||||20210604165513.576+0100||ACK|108|P|2.5.1 
MSA|AA|MESSAGE ID
```

```text
MSH|^~\&|||||20210604165513.576+0100||ACK|108|P|2.5.1 
MSA|AE|MESSAGE ID 
ERR|^^207&ERROR&hl70357^^^^Missing Patient Phone Number
```

In addition, error segments could be repeated, which means that receiving systems could send all
the errors they have for each message to us automatically.

## Goals
The intent of this proposal is to build an API endpoint in Azure that would allow receivers to
automatically send us any errors they have processing messages. The endpoint would accept a valid
`ACK` message, and if a message matching the ID they provide exists in our system, we will store
the response and any associated errors in our database.

The end point will accept `POST`, with the `ACK` message in the header. The associated end point
will require authentication to use.

In a future proposal we will build a UI on our website that will present the error messages
and associated metadata so senders are able to see real-time automatic feedback from the
receivers.

## Conclusion

## Discussion Points