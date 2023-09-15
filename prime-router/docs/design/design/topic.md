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

### Translating

The list of receivers generated from analyzing the `Endpoint` FHIR resources are filtered down to verify that the
receivers topic is associated with the universal pipeline.

**TODO: this check is likely unnecessary and can be removed**

### Batching

The receiver associated with the event is checked to see if it's associated with the universal pipeline.  The batch
queue is shared between the covid pipeline and the universal pipeline so the topic is checked to see which batching
function to use.

**TODO: this usage could be removed by creating a dedicated queue and step for the universal pipeline.**

## When to create a new topic

TODO

## Code Entry
[Topic Enum](https://github.com/CDCgov/prime-reportstream/blob/3355f1b1d8ffc169346a561569cc432b19ffb69e/prime-router/src/main/kotlin/SettingsProvider.kt#L48)