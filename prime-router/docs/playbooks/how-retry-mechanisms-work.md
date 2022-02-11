# How Retry works in ReportStream

ReportStream's main pipeline steps are:
1. Receive (aka api/reports, ReportFunction.kt)
2. Process (ProcessFunction.kt)
3. Batch (aka `timing` in settings.  Aka Merge, BatchFunction.kt)
4. Send (aka `transport` in settings.  SendFunction.kt)

ReportStream aims for exactly-once semantics, however all software is mortal, and can fail, and therefore we have retry mechanisms for each.

Since the functions are subtly different, the retry mechanisms are also subtly different for each.

## Receive

We depend on our senders to have robust retry mechanisms if the receive does not return a 201.

## Process

The Process step is triggered by events placed on the `process` queue by the previous step, Receive.   The Process step then starts a new transaction, which grabs and locks the associated row in the TASK table.

On failure, Process makes use of Azure message queue's built-in retry mechanism.   If a Process function fails with an exception, the Azure queue will notice this, and will retry.  If it tries 4 times and fails, that message is placed on the `process_poison` queue, and manual intervention will be required.

Note that when Process fails, the transaction will abort, and the TASK table entry will remain unchanged.   This makes it easy to query the TASK table to find failed process steps.

You can also examine the `process_poison` queue in the Azure Portal.

### Useful Process Query

This query assumes that anything in the process queue older than 15 minutes is a problem.


```
select report_id, next_action, next_action_at, schema_name, receiver_name, item_count, created_at from task where next_action = 'process' and next_action_at < now() - interval '15 minutes';
```

## Batch

Batch function retry makes use of the natural cadence of delivery that each Receiver has.

There is a separate function, called BatchDecider, that runs once a minute, and determines which Receivers are meant to get data within that minute, based on the Receiver `timing` settings.  

For each receiver whose time has come, the BatchDecider will place one or more events on the `batch` Azure message queue.   These messages will get processed immediately, they are not on a timer.  They trigger the BatchFunction to run for that receiver's data.

The BatchFunction then starts a new transaction, and grabs and lock rows in the TASK table associated with that Receiver.    BatchFunction will grab all rows not processed that are not older than a "backstop time".   The backstop time is set to be 3 times the normal cycle time for that receiver, plus a constant minimum time currently set to three hours.

For example, if a Receiver is set to receive data once a day, its backstop-time is set to 3 days + 3 hours, and any unprocessed batch TASK older than that will be ignored.

On failure, the BatchFunction transaction will abort, and the TASK table entry will remain unchanged.  The next time that Receiver is scheduled to receive data, the aborted TASK entries will be re-run --- until the backstop time is reached, then it will not retry any longer, and manual intervention will be required.

### Useful Batch Query

This query will find batch tasks in the TASK table that should have been processed but have not

```
select report_id, next_action, next_action_at, schema_name, receiver_name, item_count, created_at from task where next_action = 'batch' and created_at > now() - interval '4 days' and next_action_at < now() - interval '15 minutes';
```

## Send

SendFunction uses the message queue and TASK table to do retries.

Currently SendFunction has inner try/catches at the individual transport implementation level, and an outer try/catch at the SendFunction level.

Any retry mechanism is implemented at the individual transport inner try/catch level.   The outer try/catch prints and swallows exceptions and therefore does not do retries.

The Send step is triggered by events placed on the `send` queue either by Batch or by Process, in cases where the Receiving has no `timing` settings.   The Send step then starts a new transaction, which grabs and locks the associated row in the TASK table.

Send is different because it allows for partial failures.   This was because the Redox transport (now removed) required us to send one Item at a time, so there could be (and were a lot of) partial failures.

When a failure occurs, Send puts a new message on the `send` queue, that contains information on what Items to retry.  That message is giving a start time in the future, using a simple exponential backoff strategy.  The message also contains the number of retries so far, and a `send_warning` action is placed in the action table.

In this case, the transaction is __not__ aborted.  The TASK table is updated with a new retry_token value, __and__ a new message is put in the `send` queue.  (This is done in the calls to `updateHeader` (which updates the TASK table) and `queue.sendMessage` in `WorkflowEngine.kt::handleReportEvent(...).)  Then SendFunction is done, and Azure thinks the Send succeeded - in other words, rather than using Azure's built-in retry mechanism, we have built our own retry for Send.

After 4 retries, Send gives up, and puts a `send_error` action in the Action table, and does not try any more.

### Query to find failed Sends

You can use the ACTION table to find failed Sends, searching for send_error actions.

You can also use the TASK table, as follows:

select report_id, next_action, next_action_at, schema_name, receiver_name, item_count, created_at from task where next_action = 'send' and created_at > now() - interval '4 days' and next_action_at < now() - interval '15 minutes';

Note this is identical to the `batch` query, above.

## Bonus for those who love complexity

There is another transport that also sends one Item at a time:  the default HL7 implementation, with the useBatchHeaders: false, will send one HL7 message at a time.   However this data is broken apart during the Batch step, and not during Send.  That is, individual Send items are put on the queue when useBatchHeaders is false.

So... the Batch function also splits as well as merges.

Note that as of this moment, no receiver is using useBatchHeaders: false.

