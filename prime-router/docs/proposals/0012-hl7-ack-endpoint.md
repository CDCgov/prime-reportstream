# DRAFT PROPOSAL - HL7 ACK Messages

## Context and Motivation
As we continue to work in the space of sending data on to our public health department 
partners it would be valuable for us to be able to accept ACK and NACK messages from 
health departments that attempt to process our messages but for some reason are not able to.

An ACK or a NACK message is the outcome from a public health agency partner attempting to
process an HL7 message we have sent them. We typically receive them in one of two formats:
1) Email messages
2) Excel spreadsheets

Typically, we only receive NACK messages, meaning the primary focus is on building a system
that will accept those types of messages, but we should accept positive ACKs as well.

I would like us to set up a system that would allow our partners to send us these details
via either a spreadsheet or via an endpoint that will accept `HL7 ACK` messages.

## A Brief Detour Through HL7
HL7 is "structured" (as much as it can be viewed as being structured) around two main ideas:
* **Events** - These represent an event that has happened in health care for a patient, for example, 
when someone is admitted to a hospital. That event is something HL7 treats as a message.
* **Segments** - These are the individual chunks of information that are grouped into a message. Each
segment has an identifier code, and then multiple segments within it, and can contain a lot of data.

The only event we currently process is the `ORU_R01` event, which is the type of message 
for "unsolicited transmission of an observation message".

As noted above, an `ACK` message is another event that is used by a recipient of 
an HL7 message to acknowledge it was received, as well as return any errors that came up 
during the processing.

An example successful `ACK` message looks like this:
```text
MSH|^~\&|||||20210604165513.576+0100||ACK|108|P|2.5.1
MSA|AA|MESSAGE ID
MSH|^~\&|||||20210604165513.576+0100||ACK|108|P|2.5.1
MSA|AE|MESSAGE ID
ERR|^^207&ERROR&hl70357^^^^Missing Patient Phone Number
```

An example of an error `ACK` looks like this:
```text
MSH|^~\&|DCS|MYIIS|MYIIS||20150924162038- 0500||ACK^V04^ACK|465798|P|2.5.1|||NE|NE|||||Z23^CDCPHINVS
MSA|AE|313217
ERR||PID^1^11^5|999^Application error^HL70357|W|1^illogical date error^HL70533|||12345 is not a valid zip code in MYIIS
```

In addition, error segments could be repeated, which means that receiving systems 
could send all the errors they have for each message to us automatically.

As noted above, this does not mean we just receive error messages. We can also get updates from 
our receivers that they have accepted and ingested the messages. If we establish this 
kind of system we can then keep track of systems that aren't sending ACK or NACK and 
reach out and proactively solve problems. Right now we have no sense on if a message has 
been processed at all unless we hear from the STLTs directly.

Furthermore, we could build this into a system that lets us send corrections for messages 
and follow a full lifecycle for a message, where we send a message, there are problems, we 
send a correction, there is additional feedback, until we send them the final message, 
and we get an ACK response back.

It should also be noted that this concept is the same one that is used by FHIR to acknowledge 
receipt of messages and/or errors, so building this functionality in to our system will be 
used by our FHIR api as well.

## Goals
The intent of this proposal is to build an API endpoint in Azure that would allow receivers 
to automatically send us any errors they have processing messages. The endpoint would accept 
a valid ACK message, and if a message matching the ID they provide exists in our system, we 
will store the response and any associated errors in our database.

The end point will accept POST, with the ACK message in the header. The associated end 
point will require authentication to use.

In a future proposal we will build a UI on our website that will present the error 
messages and associated metadata so senders are able to see real-time automatic feedback 
from the receivers.

Finally, this will allow us to build out the functionality of the HL7 ingestion by including 
the ability to process a second message type on something very small and straightforward.

## Proposals
We will complete the following work for this proposal:

### ACK Tracking Database
The first piece of work will be a table in the database that will contain the tracking ID for
the message, the PHA sending us the message, and any error details provided. It should also
contain the status of the ACK (from the receiver), and the status of the ACK for our internal
processes.

### ACK API Endpoint
The next piece of work will be two API endpoints, one that accepts a JSON packet with the ACK
information in it, and another that will accept an `ACK` HL7 message and will then decompose the
ACK info out of it and save that to the database.

### ACK CLI Tools
The next piece of work will be a CLI tool built into `prime` that allows us to manually add
an ACK to the table after being logged in, via the API endpoint.

### ACK SFTP Site

## Next Steps
* Identify the different partners we could work with to test this out
* Identify what our responsibilities are around a NACK message
* Plan out what the website will look like for reporting our errors

## References
https://repository.immregistries.org/files/resources/5835adc2add61/guidance_for_hl7_acknowledgement_messages_to_support_interoperability_.pdf

## Discussion Points
* Can we flip this around and use with senders? 
* Can we pilot this with SimpleReport and one other sender?