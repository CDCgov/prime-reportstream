# Batch Process Update / Retry
The batching process could use more robust retry functionality and there is an opportunity for additional improvements
to the functionality as a whole to address scalability and performance concerns.

Associated Ticket - #2902

### Current Batch Functionality  
Each incoming message puts an entry into the 'batch' queue per receiver that message is going to, using the hidden
'next action at' part of the queue and selecting the next 'batch bucket' for the associated receiver.

When that time comes all queue messages for the receiver in question are added to the queue and picked up by individual 
instances of the batch function (one per message on the queue). 

Each of these functions then race to select and lock 
all records from the Task table that are for the receiver in question. The function instances that fail to get any
records die.

If a batch function fails to succeed it will be lost in the ether and require manual resetting to pick up and batch
included records.

### Proposed New Functionality
We can improve this situation and add retry/scalability by adding a cron job and altering the way we use the batch
queue. Instead of adding each message-per-receiver-per-bucket to the queue at ingestion a cron job could run at
the minimum granularity for batching (1 minute), determine which receivers need to be batched at that time, and put a 
message on the queue to be handled immediately by the batchFunction. 

The BatchDecider function will iterate over each Receiver and determine if the receiver should have been batched
within the last minute. If so, the decider will get the maximum batch size for the receiver and the total number
of outstanding records to batch (total number of Actions for this receiver with next_action of BATCH).

The decider will determine the number of queue message to add based on [total number]/[batch size] and rounding up 
to the nearest integer.

This is a minimal-change approach that leaves the
Task table management as it is but severely reduces our performance bottlenecks and race conditions. Once done, the
single run of the batch function for that receiver and batch them.

This will cover situations where, for example, Batch has been down for some time and Batch work is horrendously backed up.

### Nifty Warehouse Metaphor
Right now we have a front desk person who is getting paper reports - people are dropping by and sticking them in the
inbox at any time. The front desk person is copying incoming reports as needed, adding a few stamps, and for each 
report is calling the mail room to say 'hey, got a report for Receiver X, come get it ready to ship at 
noon' and then sticking it in the internal folder for that receiver. At noon the mail room sends one person per call 
they got to get files ready to send to the receiver. Each of these people takes the time to get up from their desk and 
walk over - the first person to get there for each receiver grabs them all out of the folder, puts them in an
envelope, and sticks it in the outbox for the receiver. The rest of them show up, grumble that there is nothing to do
and go back to their desk.

In the proposed changes the front desk person still gets the reports and puts them in a folder, but instead of calling
the mail room each time they get a report there is a front desk assistant that keeps track of schedules and receivers.
At noon, they call over to the mail room to say 'got some reports for Receiver X, please send someone over'. One person
walks over, packages up the reports, and gets to them to appropriate outbox. If for some reason they are unable to 
do that (tripped on their shoelaces or something) the assistant can call back to the mail room to ask them to send
someone else.

### Dev Notes
To implement these changes, we would need to
* determine minimal batching granularity
* create a cron job that runs at that granularity
* either cache or dynamically build a directory of which receivers get batches at which time (this can be a future
phase of this task)
* when the cron job triggers, add a 'batch' queue message with the name of the receiver
* rename 'ReceiverEvent' to 'BatchEvent' for clarity (in the codebase - Event.kt)
* look into the 'at' parameter in batchFunction to determine if it is still needed
* we would need to look into a setting change for receiver to indicate 'sendIfEmptyBatch' and generate empty batch
files if set to true
* Since the batchFunction locks the task rows that it is processing, it doesn't matter if runs for the same receiver
overlap (this is not ideal, but still a far cry better than what we have today)

### Benefits Gained / Problems Addressed
- Scalability - one queue message per receiver per batch bucket
- Recoverabilty/Retry - if a batch fails, the queue message can be re-added to the queue and re-processed to ensure
that all outstanding messages have been processed. This will need to look at 'batch time' and get all messages that
were created before that batch time and have not yet been batched.
- Remove Race Condition - the race condition of pulling #n Task records is one of the most hit queries in RS
- Queue Visibility - removing the 'nextActionAt' part of messages from the queue gives visibility
- Easier 'Empty Batch' - for receivers that want an empty batch file each batch period if there are no records to send
- Batch Cancellation - ability to add a flag to stop batching for receivers in real time if needed
