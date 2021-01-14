## Background and Goals
[Zenhub Ticket](https://app.zenhub.com/workspaces/prime-data-hub-5ff4833beb3e08001a4cacae/issues/cdcgov/prime-data-hub/147)


This design covers improvements to the underlying postgres database schema.

## Goals

### Support these primary Use Cases:

- As a receiver of Hub reports, I want to know where my data came from, when it was created, who created it, and who else received it.
- As a sender of Hub reports, I want to know where my data went, when, and to whom.
- A reciever can review basic summary information:  Number of Reports recieved, number of items recieved, list of senders from whom reports have been received, over some time period (eg "in the last week").
- Similar for senders:  summary info on numbers of reports sent, number of items sent, and a list of senders to whom reports have been sent.

### General Goals

- Build trust in the Hub - the schema should enable features that give both receivers and senders a sense of trust in the system.  The hub should be able to reassure our users that their data has transferred safely and securely.
- Secure data at all steps, both in-transit and at rest, while still making it available to those who are authorized access.

### Future Use Cases

For the first rounds of work, I'm avoiding tracking any specific data *internal* to a report, other than the `tracking_ids`.   However, the schema as is designed to be expanded in future sprints, to potentially accomodate these use cases:

- Tracking Positivity rates, sharp increases/drops for specific senders, geographic spikes, etc.

## Definitions

In the Hub, an **Item** is a single record of data.  A typical **Item** might be a Covid test result for one patient.   If the data is in CVS format, an **Item** is a single row in the CSV file.

A **Report** in the hub is simply a set of **Item**, with additional metadata.   For example, if the data is in CSV format, the Hub expects a single row of Column Header strings, followed by one or more rows of **Item** data.    It is acceptable for a Report to have just a single Item.  A Report with no Items is not processed.

The Hub implements a simple workflow engine.   Each step in the engine is a **Task**.  Most (but not all) tasks create one or more Reports. Current **Task** in the system include: 

| Task      |     Description
| ---       | ---
| Receive   | Recieve a new Report into the Hub
| Transform | Validate a received report, and transform into desired output formats, according to configured data filter business rules
| Batch     |  Merge one or more reports together.   Typically done to consolidate data per reciever requirements.
| Send      | Reliably delivery data to an external reciever, eg, an API, sftp site, or provide for upload (and notify with an email)
| Wipe      | Remove data, per business rules


## Assumptions

- Our three main things to track: TASKs create REPORTs which have ITEMs
- ITEMs are like quarks.They only exist inside of a REPORT.  Its fine and quite common to have a report with one item.
- We must track lineage per-item, however, people think in terms of REPORTs.Hence we need to track both.
- Tracking of sending- and receiving- orgs is crucial, to know who gets permissions to see the data.
- Many:many -- A given ACTION could take in many reports, and produce many reports.  Need to allow for both splitting and merging.
- Many:many -- An ITEM can be the result of multiple parent items, and can result in multiple child ITEMs.
- A few schema best practices:  Every table must have a single col PK (either a serial or a UUID), and a created_date, Normalize independent data.   Use singular nouns.
-- Reciept of a dataset implies the right to see the lineage of that dataset.
-- Sending a dataset implies the right to see the lineage of that dataset. 

## Proposed Design

Please [see the ER diagram](./lineage-er-diagram.jpeg) for the detailed proposed schema.

(Note: [Original lucid chart diagram is here](https://lucid.app/lucidchart/7b81cccb-91a0-44b3-b17f-90b9b0c8304e/edit?beaconFlowId=332110D5160D6847&page=0_0#))

### Notes on the Proposed Schema

- The separation of Task from Report allows us to have Tasks tat don't necessarily create Reports.
- Addition of the `TASK.task_result` fills a gap where we're not tracking the error messages sent back to users.
- Apparently I can't name the REPORT table 'REPORT', due to a limitation in Flyway/jooq.  Hence the weird name `REPORT_FILE`.   I also thought about `REPORT_IN`, `REPORT_DATA`, `REPORT_REC`.  Haven't sunk as low as `REPURT`, but I'm on my way there...
- Many values in REPORT_FILE will be empty depending on the task creating the report.  For example, all the sending_* fields are only filled in for an initially recieved report.
- might need a report_status in the future.  For example, `wiped` or `archived`, to explain why a blob might have disappeared.
- The primary keys for ITEM is tricky.  The sender gives us the tracking_element, but we have little control over how robust that is.  Because of this, all queries on tracking_id must also filter on report_id.
- So there's an open question:  should we require global uniqueness of tracking_ids?   It would be wonderfully convenient to use tracking_ids as primary keys in our database.    However, they would have to be globally unique, because report data can get merged together in arbitrary ways in steps downstream from Receive.
- There's subtlety with Report parent/child relationships.   Because any given task might take in a bunch of reports, and produce a bunch of reports, there's no guarantee that any of the data in output Report Y came from input Report X.  However we're tracking it in the REPORT_LINEAGE table as if every input report fed every output report.  In practice, its very hard to tell if that's true, in general.

### How this design helps build trust

- Fine-grained lineage tracking at the ITEM level, with ability to query both "backward" and "forward"
- Improved tracking of sender and reciever ownership
- Tracking versions of the functions that created the data, and SHA hash of every report.
- Next steps: Have an easy-to-use API that delivers history
- Next steps: Have an easy-to-use API that can retrieve the data in an individual ITEM. (?)

### Rollout plan:

1.  Build out the new schema in parallel with the existing TASK, TASK_SOURCE tables.
2.  Wire the REPORT_FILE and REPORT_LINEAGE tables first, then the ITEM and ITEM_LINEAGE.
3.  Code into the validation step a requirement for uniqueness of tracking Ids within each Recieved report. 
4.  Migrate existing data to the new tables, and retire or modify the old ones.


#### Queries

To validate that we can design recursive parent/child queries, I experimented with a much simplified schema found here:
[recursive-parent-child-test.sql](recursive-parent-child-test.sql)

You can run this locally to try it out:
```
psql prime_data_hub -f recursive-parent-child-test.sql 
```

Based on the experiment, I believe we can write quries that execute the following use cases:

#### Looking "forward":
- Find **all** the reports or items that are descended from my report or item, including intermediate internal steps.
- Find the **sent** reports or items that are desendants of my report or item.
- Find all the reports or items that were never sent.  (possible error situations)

#### Looking "backward":
- Find **all** the reports or items that are ancestors of my report or item, including intermiediate internal steps.
- Find the **submitted** reports or items that are ancestors of my report or item.

#### Who else got this data besides me?
- Given an item, look backward to find all its ancestors.  Then look forward, to find all the other **sent** items descended from any of those ancestors.

