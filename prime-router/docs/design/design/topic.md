# Topic

A topic consists of a name and a boolean indicating whether it's associated with the universal pipeline.  The primary
intention is to silo data between different use cases.

## Uses

### Sender/Receivers

- A sender has a configured topic and reports sent through the Universal pipeline tagged with that topic
- A receiver has configured topic and will only be routed reports that are tagged with that topic

### Routing

This is primary usage for topic and accomplishes the siloing, the routing step will only consider receivers that match
the topic for the incoming messages.

Additionally, each topic is configured with a different set of default filters that are applied if the receiver has not
specifically defined their own.

**TODO: remove the default filters**

### Translating

The list of receivers generated from analyzing the `Endpoint` FHIR resources are filtered down to verify that the
receivers topic is associated with the universal pipeline.

**TODO: this check is likely unnecessary and can be removed**

### Batching

The receiver associated with the event is checked to see if it's associated with the universal pipeline.  The batch
queue is shared between the covid pipeline and the universal pipeline so the topic is checked to see which batching
function to use.

**TODO: this usage could be removed by creating a dedicated queue and step for the universal pipeline.**

## Current topics

The current usage of topics is inconsistent and does not necessarily reflect the intention of how they will be used 
moving forward.  Of note is that current usage is slightly blurry because some initial uses of topic were to delineate
conditions going through the legacy pipeline (covid vs monkeypox).  Within the universal pipeline, topics currently roughly
map to senders (FULL_ELR -> SimpleReport, ELR_ELIMS -> ELIMS, ETOR_TI -> ETOR), but this is more of the result of the current
limited pool of senders.

## When to create a new topic

- The same receiving org will be getting data from two sources that they would like to be kept distinct
- There are special behaviors that should apply

## Code Entry
[Topic Enum](https://github.com/CDCgov/prime-reportstream/blob/3355f1b1d8ffc169346a561569cc432b19ffb69e/prime-router/src/main/kotlin/SettingsProvider.kt#L48)