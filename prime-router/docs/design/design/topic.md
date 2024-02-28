# Topic

A topic consists of a name and a boolean indicating whether it's associated with the universal pipeline.  The primary
intention is to silo data between different use cases.

## Uses

The primary use right now is to separate group of senders that route data through the universal pipeline and senders
that route data through the legacy pipeline.  See below for more details.

### Sender/Receivers

- All senders and receivers must be configured with a topic value
- Reports sent through the pipeline will be tagged with the topic of the sender that initiated the report
- A receiver will only receive reports that are tagged with that receiver's topic

### Routing

This is primary usage for topic and accomplishes the siloing, the routing step will only consider receivers that match
the topic for the incoming messages.

Additionally, each topic is configured with a different set of default filters that are applied if the receiver has not
specifically defined their own.

**TODO [#11441](https://app.zenhub.com/workspaces/platform-6182b02547c1130010f459db/issues/gh/cdcgov/prime-reportstream/11441) remove the default filters associated with topics.  They are currently mostly unused within the universal pipeline and
being linked to topics creates confusion and makes them less scalable.**

### Translating

The list of receivers generated from analyzing the `Endpoint` FHIR resources are filtered down to verify that the
receivers topic is associated with the universal pipeline.

**TODO [#11442](https://app.zenhub.com/workspaces/platform-6182b02547c1130010f459db/issues/gh/cdcgov/prime-reportstream/11442) this [check](https://github.com/CDCgov/prime-reportstream/blob/ce91d6748aae94c5ab7c4cfc27da11c6d189521c/prime-router/src/main/kotlin/fhirengine/engine/FHIRTranslator.kt#L88) is likely unnecessary and can be removed**

### Batching

The receiver associated with the event is checked to see if it's associated with the universal pipeline.  The batch
queue is shared between the covid pipeline and the universal pipeline so the topic is checked to see which batching
function to use.

**TODO [#11443](https://app.zenhub.com/workspaces/platform-6182b02547c1130010f459db/issues/gh/cdcgov/prime-reportstream/11443): this [usage](https://github.com/CDCgov/prime-reportstream/blob/ce91d6748aae94c5ab7c4cfc27da11c6d189521c/prime-router/src/main/kotlin/azure/BatchFunction.kt#L129) could be removed by creating a dedicated queue and step for the universal pipeline.**

### Reporting

Topic can be used to report on a slice of the data flowing through the universal pipeline and is used as a filtering
conditions in some custom queries written for organizations. 

## Current topics

The current usage of topics is inconsistent and does not necessarily reflect the intention of how they will be used 
moving forward.  Of note is that current usage is slightly blurry because some initial uses of topic were to delineate
conditions going through the legacy pipeline (covid vs monkeypox).  Within the universal pipeline, topics currently roughly
map to senders (FULL_ELR -> SimpleReport, ELR_ELIMS -> ELIMS, ETOR_TI -> ETOR), but this is more of the result of the current
limited pool of senders.

## When to create a new topic

- The same receiving org will be getting data from two sources that they would like to be kept distinct
- There are special behaviors that should apply to a specific slice of data (i.e. disallow receiver transforms)
- Simplify routing when a receiver should get all the reports for a given topic

## Code Entry
[Topic Enum](https://github.com/CDCgov/prime-reportstream/blob/3355f1b1d8ffc169346a561569cc432b19ffb69e/prime-router/src/main/kotlin/SettingsProvider.kt#L48)