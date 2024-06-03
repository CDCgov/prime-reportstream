# Load Testing Report Stream

## Goals

## Directory structure

## Tools

### Azure load testing

Out of the box, there are tools baked in to azure for running load tests and specifically azure functions can easily
be [tested](https://learn.microsoft.com/en-us/azure/load-testing/how-to-create-load-test-function-app).

**Note: currently do not have access with regular or SU accounts**

The entry point for the pipeline is just a single endpoint, so the load test should be configurable entirely via the UI;
including the sender configuration and the report payload. The way this would be set up is to add several different
requests with the different payloads.

#### JMeter support

Azure load testing also supports JMeter scripts, but this is likely not required since the workflow is so simple;
typically, JMeter would be used to simulate a complex user flow that invokes many different APIs in a defined order.

## Infrastructure

The load test to provide true representation of the system under load will need to be configured to run in an
environment where all the possible transports can be invoked.

**Note: some pre-work would be deprecating and disabling unused transport types**

The work required is to create the relevant mocked infrastructure elements in the load environment; this would include
items such as SFTP server, REST servers and an Azure storage account for blob storage.

### Environment setups

The nature of ReportStream (data that grows infinitely) means that we'll need two different environment configurations
for the following use cases:

- contrasting the performance of two different commits
- tracking the performance of the system over time as the data grows

To enable both of these tests, we'll need to configure the following environments

- an environment is a DB seeded with the same representative, but scaled amount of data at the beginning of each run and
  minimums and maximum instances set to reflect the amount the data was scaled down by
- an environment that gets seeded with the most recent PROD backup of the db and identical settings for the number of
  instances

## Workloads

Multiple workloads will get designed in order to test a few scenarios that have been evaluated as important to
understand how ReportStream performs.

### Standard load test

This is the workload that will get run regularly as part of CI process. It will align rough usage pattern over a day (
scaled down to an hour or so), where the number of reports submitted per minute scale up, plateau for a period of time
and scale back down. This workload will provide the ongoing baseline for the performance of the system as changes are
made.

Large architectural changes should use this workload to A/B test to see if the change negatively impacts the system.

### Spike test

This workload will be decided to quickly flood the system with a very large number of concurrently submitted reports.
The goal of the workload is to understand how the system performs under an extreme and sudden increase in load and helps
the team evaluate if the concurrent autoscaling mechanisms are sufficient.

This test will want to get scheduled to run at regular intervals and after an infrastructure adjustments.

### Soak test

This workload will serve as an endurance test where reports submitted per minute will ramp up and then plateau for an
extended period of time, on the scale of several hours. The goal of this is workload is to gain confidence that the
system is working as expected under an extended period of load and that system does not develop any unexpected
behaviors.

This test will want to get scheduled to run at regular intervals and after any large architectural or infrastructure
changes.

## Metrics

In order to

- Max number of items in the queue?
- Throughput of reports?
- Compare across runs
    - This is likely handled by azure load testing
- Number of items in the poison queue
- APM
    - Memory
    - CPU

## Datasets

- FHIR and HL7
- Mix of small and large batches

### Receiver configuration

- Mix of FHIR/HL7 receivers plus transforms

### Sender configurations

- Mix of FHIR/HL7 senders
- Some senders need to have transforms

## CI Strategy

## Open Questions

- Should the covid pipeline be included?
- Which transports can be dropped/used and percents?
- Should the APIs from the client be factored in too (i.e. submission history)?
- Should the initial tests reflect the expected mix of data or the actual (i.e. 95% FHIR or a more even mix)?
- Should we include ETOR data?
