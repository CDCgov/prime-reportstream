# Batch Step

This step will batch all the reports that have been translated within a configured window and send them all in one report to the receiver. 
This report can contain items from multiple senders for a single topic.


## Configuration

This step supports different configuration options on a **per receiver** basis and are configured as part of onboarding
a new receiver:

- `operation`: whether to perform batching 
  - `MERGE`: merge multiple reports before delivering
  - `NONE`: do not perform batching and send each report individually
- `numberPerDay`: how many times a day to perform the batch operations and generate a report, must be between 0 and 3600; setting it to 1440 will
correspond to once a minute and 288 to every five minutes
- `initialTime`: used to determine when the first batch for the day should be sent
- `timezone`: what timezone to use when determining when batches should be sent
- `maxReportCount`: the maximum number of items to include in a delivered report
- `whenEmpty`: whether to send a report even if it contains no items; defaults to not sending an empty report
  - `action`: whether to send an empty report, options:
    - `SEND`
    - `NONE` (default)
  - `onlyOncePerDay`: whether to send an empty report for every time a batch would be sent or just once
- `useBatching`/`useBatchHeaders`: this is configured in the translation setting for the receiver, the two different properties
are for FHIR or HL7 batch formats respectively

**Note: in order for batching to take place both `useBatching` must be true and the timing `operation` must be `MERGE`**

#### Example configuration for a receiver

```yaml
...
timing:
  operation: MERGE
  numberPerDay: 1440
  initialTime: 00:00
translation: !<FHIR>
  useBatching: true
  format: FHIR
...
```

## How it works

The batch step is handled by two different azure functions:

- `BatchDeciderFunction` runs on a cron trigger every minute to look for receivers that need to have reports batched
- `BatchFunction` merges several reports into a single file and queues that report to be sent

### `BatchDeciderFunction`

This function runs on a cron trigger every minute (the smallest unit at which batches can be triggered) and performs the 
following work on each invocation for every receiver with a configured `timing`:

- Checks if the receiver should have received a report in the last 60 seconds
- Determines how many reports need to be batched by fetching all the `Task` records where the `next_action` is `batch`
- Calculates how many batch messages should get added to the queue by dividing the number of reports by the configured `maxReportCount`
- Dispatches those messages to the queue

### `BatchFunction`

This function runs on a queue trigger (queue name `batch`) and is responsible for performing the work of merging many reports
into one.  It takes advantage of a SQL feature, `SKIP LOCKED` in order to handle multiple batch events for the same receiver
being processed in tandem (see example section for more details).

For each message that gets processed, the function:

- Queries the `Task` table for all records where `next_action` is `batch` applying the `SKIP LOCKED` modifier to only grab records
that are not already being processed by another invocation applying a limit corresponding the `maxReportCount`
- Each report associated with the `Task` record is then downloaded and then all of them are merged into a single file
    - For FHIR receivers, the bundles are each appended on a new line following the `ndjson` format
    - For HL7 receivers, the reports are concatenated into a HL7 batch message
- After generating and uploading new report blob, a `send` event is added to the queue to be processed

### Retries

The retry capability of this step works by looking far enough back in the task table such that each task has at a minimum three
chances to be picked up by the batch step.  The look back window is calculated based on how often that receiver is configured
to receive a batch and then is used by the `BatchDeciderFunction` when it determines how many messages to add to the queue
and by the `BatchFunction` when it queries the task table for all the reports that need to be batched.  In both cases, the following
filter is applied:

```sql
task.next_action_at >= {BACKSTOPTIME}
```

For example, if a receiver is configured to receive a file once per day, when looking for tasks that need batching
we'll calculate a look back window of three days.

This behavior serves a secondary purpose of preventing the system from processing old tasks in the system.

**Note: the look back window includes a three-hour padding which means for more frequent schedules (i.e. batch once a minute) a larger window will be examined.  
This padding is in place to handle extended periods of downtime.**

## Code entry points

- [BatchDeciderFunction](https://github.com/CDCgov/prime-reportstream/blob/ec1f33db50fabdfc02f6d07cce34f28951121dd5/prime-router/src/main/kotlin/azure/BatchDeciderFunction.kt#L20)
- [BatchFunction](https://github.com/CDCgov/prime-reportstream/blob/ec1f33db50fabdfc02f6d07cce34f28951121dd5/prime-router/src/main/kotlin/azure/BatchFunction.kt#L29)
- [Timing](https://github.com/CDCgov/prime-reportstream/blob/ec1f33db50fabdfc02f6d07cce34f28951121dd5/prime-router/src/main/kotlin/Receiver.kt#L160): batch configuration data class
- Bundling
  - [FHIR](https://github.com/CDCgov/prime-reportstream/blob/ec1f33db50fabdfc02f6d07cce34f28951121dd5/prime-router/src/main/kotlin/fhirengine/utils/FHIRBundleHelpers.kt#L295)
  - [HL7](https://github.com/CDCgov/prime-reportstream/blob/ec1f33db50fabdfc02f6d07cce34f28951121dd5/prime-router/src/main/kotlin/fhirengine/utils/HL7MessageHelpers.kt#L32)

## Examples

### Multiple batches at once:

A receiver is configured to receive a batch every five minutes and only expects two items in each report and there were
six reports that were translated in the last 4 minutes.

The batch decider function will run and look for any task where the `next_action` is `batch` that was added in the last three
hours and 15 minutes; this corresponds to the 3-hour padding plus the three retries (3 * 5 minutes).  It will find the 6 
tasks corresponding to the reports that were translated and then add three messages to the queue by dividing the number of reports
by the max items configured for that receiver (6 / 2).

The exact number of concurrent invocations of the batch function will vary based on load, but for the purposes of the scenario
it will assume all three messages dispatch by the decider will execute concurrently.  Each function will run a query for
the tasks it should process with a limit of two items, skipping any tasks that are currently being processed by another invocation.
The batch function downloads and merges the reports and then dispatches a `send` event.


### Failed processing

A receiver is configured to receive a file twice a day with each file containing 10 reports with 20 reports that have been 
translated in the last 12 hours.  The batch decider creates two queue messages (20 / 10) and dispatches them.  The batch function
processes the first message successfully, but there is an unexpected outage while processing the second invocation which
is resolved two hours later.  This leaves 10 reports that the system failed to process that still need to be delivered.

Five hours later, 20 more reports are translated for the receiver and five hours later the batch decider is triggered for the receiver again and
when it processes the receiver it finds 30 reports that need batching; it does this by calculating a look back window of
39 hours (3 * 12 hours + 3 hours of padding) and finds the 20 new reports as well as the 10 reports that were not processed
because of the unexpected outage and dispatches three batch messages to the queue.

The batch function then processes each of these messages, generating a batched report and dispatching a send event to the queue.

### Empty report

A receiver is configured to receive a file even when it does not contain any reports.  In this scenario, the batch decider
function will find 0 translated reports and will then look at the `timing` configuration for that receiver to see what the
behavior is on empty batches.  Since the receiver is configured to receive a file, the batch decider will dispatch a single
batch event with the `isEmpty` flag as `true`.  The batch function then looks for that flag and generates an empty report
for the receiver.

Few additional notes:

- A message with `isEmpty: true`, is only dispatched for receivers that want to receive empty reports
- This behavior is only supported for receivers who are delivered reports in the following formats
  - FHIR with `ndjson`
  - CSV
  - HL7 batch


## Common Operations

- Re-queuing a report for batching: if a batch event has fallen outside the look back window, but should be re-tried, it is possible to make the
system consider it again by updating the `next_action_at` to a time that would fall within the look back window for the receiver.