# Batch Step

This step will batch all the messages that have been received and send them all in one message to the receiver. This message can contain messages from multiple senders.


## Configuration

This step supports different configuration options on a **per receiver** basis:

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
- `useBatching`: this is configured in the translation setting for the receiver

**Note: in order for batching to take place both `useBatching` must be true and the timing `operation` must be `MERGE`**

## How it works

The batch step is handled by two different azure functions:

- `BatchDeciderFunction` runs on a cron trigger every minute to look for receivers that need to have reports batched
- `BatchFunction` merges several reports into a single file and queues that report to be sent

### `BatchDeciderFunction`

This function runs on a cron trigger every minute (the smallest unit at which batches can be triggered) and performs the 
following work on each invocation for every receiver with a configured `timing`:

- Checks if the receiver should have received a task in the last 60 seconds
- Determines how many reports need to be batched by fetching all the `Task` records where the `next_action` is `batch`
- Calculates how many batch messages should get added to the queue by dividing the number of reports by the configured `maxReportCount`
- Dispatches those messages to the queue

### `BatchFunction`

This function runs on a queue trigger (queue name `batch`) and is responsible for performing the work of merging many reports
into one.  It takes advantage of a SQL feature, `SKIP LOCKED` in order to handle multiple batch events for the same receiver
being processed in tandem (see example section for more details).

For each message that gets processed, the function:

- Queries the `Task` table for all records where `next_action` is `batch` applying the `SKIP LOCKED` modifier to only grab records
that are not already being processed by another invocation
- Each report associated with the `Task` record is then downloaded and then all of them are merged into a single file
    - For FHIR receivers, the bundles are each appended on a new line following the `ndjson` format
    - For HL7 receivers, the messages are concatenated into a HL7 batch message
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

This behavior serves a secondary behavior of preventing the system from processing old tasks in the system.  
See the example below for a more detailed example.

**Note: the look back window includes a three-hour padding which means for more frequent schedules (i.e. batch once a minute) a larger window will be examined.  
This padding is in place to handle extended periods of downtime.**

## Code entry points

- [BatchDeciderFunction](https://github.com/CDCgov/prime-reportstream/blob/ec1f33db50fabdfc02f6d07cce34f28951121dd5/prime-router/src/main/kotlin/azure/BatchDeciderFunction.kt#L20)
- [BatchFunction](https://github.com/CDCgov/prime-reportstream/blob/ec1f33db50fabdfc02f6d07cce34f28951121dd5/prime-router/src/main/kotlin/azure/BatchFunction.kt#L29)
- [Timing](https://github.com/CDCgov/prime-reportstream/blob/ec1f33db50fabdfc02f6d07cce34f28951121dd5/prime-router/src/main/kotlin/Receiver.kt#L160): batch configuration data class
- Bundling
  - [FHIR](https://github.com/CDCgov/prime-reportstream/blob/ec1f33db50fabdfc02f6d07cce34f28951121dd5/prime-router/src/main/kotlin/fhirengine/utils/FHIRBundleHelpers.kt#L295)
  - [HL7](https://github.com/CDCgov/prime-reportstream/blob/ec1f33db50fabdfc02f6d07cce34f28951121dd5/prime-router/src/main/kotlin/fhirengine/utils/HL7MessageHelpers.kt#L32)

### Relevant tables

- Task
- Report

## Example

## Common Operations

- Re-queuing a report for batching: if a batch event has fallen outside the look back window, but should be re-tried, it is possible to make the
system consider it by updating the `next_action_at` to a time that would fall within the look back window for the receiver.
