# History API - Submission Status
When a report is submitted into ReportStream, it is possible to keep track of its status via various endpoints. The value is always available for submitted reports via the History API:
```/waters/report/{reportId or submisionId}/history```

## Calculation Method
The status is dynamically calculated at the moment of the History API call. All pipelines use the same entry point.

The result is determined by a combination of checks:
* Has the Submission performed Routing?
* Is there a future action scheduled for the Submission?
* Are there any Destinations with items that passed filters?
* Are there any Destinations at all for this Submission?
* Of the available Destinations, how many have received their items?
* Of the Destinations that received items, did any items get filtered out?

### Submission Status
The status is updated as the Submission goes through the Pipeline:
* Received - The Submission was successfully received. This status remains until Routing has populated Destinations, or determined no data will be sent.
* Waiting to Deliver - At least one Destination has been found and at least one item has successfully passed all filters, but no data has been delivered yet.
* Partially Delivered - At least one Destination has had its data delivered, and at least one more Destination is expected to have data delivered to it.
* Delivered - All Destinations that were expected to receive data have had theirs delivered.
* Not Delivering - Either no Destinations were found for the submitted data, or all items were filtered out.
