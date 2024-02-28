# Azure Dashboard FAQ
Living repository of How Do I questions for Azure dashboard.

As of 12/7/2021 this is how we can accomplish these tasks. Once the logs are consolidated into a workspace dashboards
will be created that give more direct and immediate access to needed data.

###How do I view exceptions?
We do not have the best ways set up for viewing exceptions, but will in the future. 

* Log in to dashboard.
* Search for [pdhstaging|pdhprod]-appinsights
* Transaction Search
* Filter on Event types = 'Exception'
* Set timeframe as needed
* View exceptions, click on them to drill down
* Once drilled down, view details to determine if it is important and what the problem is
* In details, clicking 'All available telemetry 5 minutes before and after' can be helpful, but is often cluttered

 ### Where do we go to view scaling settings and triggers?
* Log in to dashboard
* Go to [pdhstaging|pdhprod]-serviceplan
* See 'scale out' (we do not scale up)
* Current rules shows what the scale out logic is
			
### How do we see current instances running?
* Log in to dashboard
* Go to [pdhstaging|pdhprod]-serviceplan
* See 'scale out' (we do not scale up)
* Click on tab 'Run History'
			
### Where do we see database performance?
* Log in to dashboard
* Go to [pdstaging\pdhprod]-pgsql
* See current performance
* Can go to 'Query Performance Insight' to see which queries are taking up resources
			
### Where do we see current queue numbers?
* Log in to dashboard
* Search for primte-data-hub-[staging|prod]
* Go to [pdhstagingstorageaccount|pdhprodstorageaccount]
* click on Queues on the left to see list of queues, not much data there thoughts
* click on metrics on the left - does not separate per queue