# Managing Translation Schemas

## Context

Schemas are used at multiple points in the universal pipeline

- transforming items sent by senders
- transforming items right before they are delivered to a receiver

and cover different kinds of transforms:

- HL7 -> FHIR
- FHIR -> FHIR
- FHIR -> HL7

These schemas are currently stored as files that are included in the deployed application and read from the file sytem
when they are applied to an item.

## Problem

The overarching problem is that since the schemas are part of the deployed application they can only be updated at the
frequency at which the application itself is deployed.  This is quite limiting and downstream impacts sucha as:

- onboarding new senders/receivers can take a while as iterative changes to the schema have to tested out over several days or weeks
- bugs discovered in the schema cannot be immediately addressed without a hotfix
- the senders/receivers do not have any capability to self-serve changes to their own schemas

## Storage Solutions

### DB

### File Store (Azure blob storage)

- Turn on versioning and keep track of this in the db?

## Change management solutions

### API

### Validation

### Common schemas

#### Across all senders/receivers

#### For a particular organization

## Open Questions
- How should common schemas be treated?
  - Should they be a special case?
- How do `extends` schemas work?
  - They currently are using relative pathing which seems very brittle
- Should we store fully resolved schemas (i.e. don't actually persist references)?

## Update log
