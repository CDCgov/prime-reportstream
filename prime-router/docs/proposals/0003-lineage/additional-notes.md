## Additional notes as the code was developed

### ITEM Tracking

To ensure that items can be uniquely tracked, we make the following key assumption:

We assume that within any one report, the index ordering (eg, 1,2,3,...) and number of Items is fixed. That is, we can write a Report to a blob store, and read it back into memory later, and be certain that the order of the Items and the number of Items has not changed.   This simple assumption means our code can guarantee unique Item lineage tracking, because (item_index, report_id) is now a system-wide unique key for each Item.   When we read data from the blob store, we now have a simple way to match Items in the data blob to corresponding items tracked in our lineage database tables.

Note that other options are not as good:
- use the sending customer's trackingElement (bad: might not be unique, and doubly bad because it requires the send step to parse hl7, csv, and redox), or
- attaching our own additional unique id into the blob data (bad: the downstream receiving customer doesn't want our ids in their data)


### Notes on ITEM tracking in each of the main azure functions:

