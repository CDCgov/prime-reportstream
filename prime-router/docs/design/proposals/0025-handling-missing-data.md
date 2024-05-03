# Proposal for handling missing timezones in messages

## Background

ReportStream is currently facing an ongoing issue regarding missing timezones in date times sent by some senders. 
Certain STLTs require the conversion of all datetime values to their respective local timezones. In cases where 
timezones are absent, ReportStream defaults the datetime to UTC and then converts it to the receiver's local timezone.
This can be an issue since ReportStream is essentially making up data and could be reporting incorrect data.
To tackle this issue, there are alternative approaches to handling missing timezones instead of resorting to UTC 
as the default. We also propose implementing a logging mechanism to notify the STLT when such discrepancies occur.

## Possible Implementations

We should encourage Senders to include timezones for date times whenever they're missing from messages and issue 
a warning message whenever this information is absent, ensuring they are aware of the issue.

In cases where a sender cannot provide a timezone for a datetime, ReportStream will need to determine the most appropriate timezone for the Sender. This can be achieved through the following methods:

* Request the sender to specify a default timezone for their date times, which could be added as a sender transform.
* Retrieve the timezone by looking at the Patient's address. We would need to add logic for this.
* Check if timezone information is available in other date times in the message.
* Always default to UTC as we are currently doing

Each time ReportStream appends a timezone to a datetime, a note must be logged on the FHIR bundle.  A possible place to
map this under could be in `ServiceRequest.note`

The note's contents should detail the field that was lacking a timezone, along with both the new and original values.

## Extra considerations

What other changes should we let STLTs know about changes we're doing to their messages.

* Turn unwanted conditions into notes
* Turn AOEs into notes


