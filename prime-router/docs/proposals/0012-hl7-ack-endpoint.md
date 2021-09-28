# DRAFT PROPOSAL - HL7 NACK Message Endpoint
## Context and Motivation
As we continue to work in the space of sending data on to our public health department partners it would be valuable for us to be able to accept ACK and NACK messages from health departments that attempt to process our messages but for some reason are not able to.

HL7 is structured around two main ideas:

* Events
* Segments

* The only event we currently process is the ORU_R01 event, which is the type of message for "unsolicited transmission of an observation message".

There are many other message types however, for many other types of events in health care, including the ACK.

An `ACK` message is used by a recipient of an HL7 message to acknowledge it was received, as well as return any errors that came up during the processing.

An example successful `ACK` message looks like this:

```text
MSH|^~\&|||||20210604165513.576+0100||ACK|108|P|2.5.1
MSA|AA|MESSAGE ID
MSH|^~\&|||||20210604165513.576+0100||ACK|108|P|2.5.1
MSA|AE|MESSAGE ID
ERR|^^207&ERROR&hl70357^^^^Missing Patient Phone Number
```

In addition, error segments could be repeated, which means that receiving systems could send all the errors they have for each message to us automatically.

This does not mean we just receive error messages. We can also get updates from our receivers that they have accepted and ingested the messages. If we establish this kind of system we can then keep track of systems that aren't sending ACK or NACK and reach out and proactively solve problems. Right now we have no sense on if a message has been processed at all unless we hear from the STLTs directly.

Furthermore, we could build this into a system that lets us send corrections for messages and follow a full lifecycle for a message, where we send a message, there are problems, we send a correction, there is additional feedback, until we send them the final message, and we get an ACK response back.

It should also be noted that this concept is the same one that is used by FHIR to acknowledge receipt of messages and/or errors, so building this functionality in to our system will be used by our FHIR api as well.

## Goals
The intent of this proposal is to build an API endpoint in Azure that would allow receivers to automatically send us any errors they have processing messages. The endpoint would accept a valid ACK message, and if a message matching the ID they provide exists in our system, we will store the response and any associated errors in our database.

The end point will accept POST, with the ACK message in the header. The associated end point will require authentication to use.

In a future proposal we will build a UI on our website that will present the error messages and associated metadata so senders are able to see real-time automatic feedback from the receivers.

Finally, this will allow us to build out the functionality of the HL7 ingestion by including the ability to process a second message type on something very small and straightforward.

## Next Steps
* Identify the different partners we could work with to test this out
* Identify what our responsibilities are around a NACK message
* Plan out what the website will look like for reporting our errors

## References
https://repository.immregistries.org/files/resources/5835adc2add61/guidance_for_hl7_acknowledgement_messages_to_support_interoperability_.pdf

## Discussion Points
Can we flip this around and use with senders? Can we pilot this with SimpleReport and one other sender?