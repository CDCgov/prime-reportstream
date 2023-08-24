# Handling sending duplicate items

## Context

There is currently an ability to resend a report to a receiver, but it is only configured to support sending a report
that failed at the send step.  It works by simply adding a new send event to the queue with the report ID that have previously 
failed.

## Problem

There are other places in the pipeline where an error might occur (i.e. the translation step fails for a receiver) and those
failures are not supported by the existing resend function leaving the only recourse being to have the sender resubmit
the entire report.  This then results in a new problem where receivers can currently receive the same report twice.

## Solution

### Preventing a receiver from getting a duplicate

Regardless of the approach for actually triggering reports to get resent, the system should be rigorous enough to check
if the receiver has already gotten that item delivered.

#### Prior art

The legacy pipeline tracked items through the system in two ways:

- A unique tracking ID was extracted from the item
  - How that ID was extracted was determined based on the schema associated with the sender
- An item hash is calculated using the contents of the item (using all the columns as the input to a message digest)

Both of these are only partially implemented in the universal pipeline:

- The universal pipeline does not implement a tracking ID
- An item hash is calculated in the route and convert steps, but not in the translate or batch step

#### Using the item hash to avoid duplicates

By calculating an item hash at every step, the system can quickly detect whether an item has been sent to a receiver. This
would consist of primarily two changes:

- update the translate and batch steps to store an item hash when the item lineage row is generated
- update the batch step to filter the reports to be batched by examining the item lineage table to see if the item in the
report had already been batched
  - this new function will likely need to accept a maximum date in order to only consider the most recent reports sent to
  the receiver
  - the batch step should record when items are filtered because they have already been sent

There is existing [functionality](https://github.com/CDCgov/prime-reportstream/blob/9d11de15a24d7b8d58a50fec026168f33f857774/prime-router/src/main/kotlin/Report.kt#L1284) 
for calculating an item hash from an HL7 row so this and a similar method will need to be added for FHIR bundles.  Jackson
does implement `hashCode` for every object that extends `JsonNode`, but we'll need to investigate if that is sufficient
for our purposes.  Specifically, implementor will need to verify that identical but structurally different JSON generates
the same hash: `{name1: 'a', name2: 'b'}` vs `{name2: 'b', name1: 'a'}`

### Resending items that fail in a step another than send

This section is purposely left sparse as it's highly contingent on product requirements.

#### UI and custom functions

This solution would entail writing new admin functions and UI that can query for errors that have occurred in the pipeline
and then provide endpoints for triggering messages to be resent.

#### Take advantage of the azure `*-poison` queue

Events that are retried but keep failing are ultimately placed in a poison queue in azure.  A custom function could be
written that processes any items out of the queue once the underlying problem has been solved.

## Open questions

- Are there any situations where a receiver would want to receive the same item twice?
- Is there a current unmet need to resend items that failed earlier than the send step?