# Proposal for handling missing timezones in messages

## Background

ReportStream is currently facing an ongoing issue regarding missing timezones in date times sent by some senders. 
Certain STLTs require the conversion of all datetime values to their respective local timezones. In cases where 
timezones are absent, ReportStream defaults the datetime to UTC and then converts it to the receiver's local timezone.
This can be an issue since ReportStream is essentially making up data and could be reporting incorrect date/time(s).
To tackle this issue, there are alternative approaches to handling missing timezones instead of resorting to UTC 
as the default. We also propose implementing a logging mechanism to notify the STLT when such discrepancies occur.

## Possible Implementations

We should encourage Senders to include timezones for date times whenever they're missing from messages and issue 
a warning message whenever this information is absent, ensuring they are aware of the issue.

In cases where a sender cannot provide a timezone for a datetime, ReportStream will need to determine the most
appropriate timezone for the Sender. This can be achieved through the following methods:

* Request the sender to specify a default timezone for their date times, which could be added as a sender transform.
* Retrieve the timezone by looking at the Patient's address. This could be unreliable since the patient's location may not be the same as the timezone the data is reported in.
* Check if timezone information is available in other date times in the message.
* Always default to UTC as we are currently doing
* Check with STLTs whether they want us to reject messages when the sender can't provide a timezone

Each time ReportStream appends a timezone to a datetime, a note must be logged on the FHIR bundle.  A possible place to
map this under could be in `ServiceRequest.note`

### Adding Missing Timezone through Sender Transforms
To support adding missing timezones to date times through sender transforms. We could add two new custom FHIR path functions:
* `isMissingTimezone` Checks if a date time is missing timezone data
* `addTimezone` Adds timezone data to a dateTime

To create a note on the Bundle, we could either create a new property `addMissingTimezoneNote` that when is set to true adds
a note to the FHIR bundle. Another way to create the note is to make it part of the `addTimezone` function.


``` yaml
- name: sr-observation-datetime
  resource: 'Bundle.entry.resource.ofType(Observation)'
  condition: '%resource.effective.isMissingTimezone()'
  addMissingTimezoneNote: true
  bundleProperty: '%resource.effective[x]'
  value: [ '%resource.effective[x].addTimezone("CST")' ]
```

The note's contents should detail the field that was lacking a timezone, along with both the new and original values.
We will have to check with STLTs if they will need this additional data, or would want these notes suppress. We will have 
to have a way to easily identify these notes in order to suppress them if necessary.



## Extra considerations

What other changes should we let STLTs know about changes we're doing to their messages.

* Turn unwanted conditions into notes
* Turn AOEs into notes


