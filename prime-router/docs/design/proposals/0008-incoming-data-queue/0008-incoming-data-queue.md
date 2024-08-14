# Incoming Data Queue

## Introduction
As we begin to scale our operations up, and add more senders, we run 
the risk of not receiving data that needs to be reported to our State, Territory, 
Local, and Tribal partners. This is going to be especially true as SimpleReport moves to 
change their method of sending data to us. Currently they batch their records and then
send a CSV file every two hours. Their intent is to start placing individual test results
into an Azure Storage Queue and sending us records as they receive them. We are likely
not prepared for this.

## Analysis
Currently, we have configured the system to leave a maintenance window between 09:30 EST - 11:00 EST
in order to deployments. This works because SimpleReport does not post to use during that time, but as
they migrate to posting individual test results in real time we could potentially lose submissions from
them. We can build in some mechanism to return an HTTP code that signals we're offline for maintenance
(and we probably should be doing that), and hope that they will be able to check for that result and
resubmit any queued messages at a later time, but there is no guarantee for that.

This is going to be even more true as we connect to other senders in the future. It is possible, even likely,
that some of the sender partners we will deal with do not have the technical ability 
to check the message, and we will lose messages in transmission when we take down the system.

The long-term vision for ReportStream is that it works not just for COVID-19 test cases, but for all manner
of electronic lab reporting, as well as the potential for integrating with syndromic surveillance systems,
which means we need to strive for maximum uptime. There is work to ensure that we have zero-downtime 
deployments, which is a good first step, but there is more that we can do.

## Proposal
We should separate the logic that handles the incoming messages from the rest of the system and store the
posts into our own Azure Storage Queue. The intention is to allow many results to come into the queue,
and be processed by us when your system is back online. In many cases, this will be instantaneous, but
if we run into extended downtime we can be assured that messages coming in are not lost, they're just waiting.

On `POST` the application will push the result into the storage queue and generate (or retrieve)
the ID of the item in the queue. At this point, it will issue a 202 HTTP status code indicating 
that the work is still processing, and a `Location` header indicating where to get the status of the
work in process.

## Available Technologies
There are multiple options available for us to write to a queue:
- Azure Service Bus
- Azure Storage Queue
- A custom solution that we build, for example RabbitMQ and Postgres, or RabbitMQ and Redis

## Goals
Currently, we cannot risk taking down our application without potentially losing incoming messages, therefore
this type of decoupling would allow the queue application to capture and store incoming messages and
then ReportStream would read from the queue and process items there.

## What's Next?
We would need to make a determination about what tech stack we want to go with, and how we run this
inside Azure independently of the main ReportStream app. We would also need to look at how to 
integrate this new application into our CI/CD pipeline, and we would need to update our code in ReportStream
to be able to handle the processing from the queue.