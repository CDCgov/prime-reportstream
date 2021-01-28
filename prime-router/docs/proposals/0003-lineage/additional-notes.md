## Additional notes as the code was developed

### ITEM Tracking

To ensure that items can be uniquely tracked, we make the following key assumption:

We assume that within any one report, the index ordering (eg, 1,2,3,...) and number of Items is fixed. That is, we can write a Report to a blob store, and read it back into memory later, and be certain that the order of the Items and the number of Items has not changed.   This simple assumption means our code can guarantee unique Item lineage tracking, because (item_index, report_id) is now a system-wide unique key for each Item.   When we read data from the blob store, we now have a simple way to match Items in the data blob to corresponding items tracked in our lineage database tables.

Note that other options are not as good:
- use the sending customer's trackingElement (bad: might not be unique, and doubly bad because it requires the send step to parse hl7, csv, and redox), or
- attaching our own additional unique id into the blob data (bad: the downstream receiving customer doesn't want our ids in their data)


### Notes on ITEM tracking in each of the main azure functions:



#### TODO

- Enable actionHistory to be called fluent-style.
- Remove sending_org in REPORT_FILE during the`recieve` step.  As is, it will break the Download site (every file will appear twice).
- Likewise, the downloads will mess things up too, as they have the same organization.
- Write up a description of all the unicorns (see below)
- Do DownloadFunction.
- Do Items.
- Remove Task_Source table.
- Do design work around removing Task table.
- Write lots of unit tests
- Do the blob digest
- Fix the -1.
