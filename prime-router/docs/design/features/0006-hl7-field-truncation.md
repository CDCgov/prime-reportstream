# HL7 Field Truncation

## Context

HL7 fields have their maximum lengths defined in the spec. When moving messages through the Universal Pipeline, we want to trim values down to their maximum length or a custom length if it is desired by the receiving party. 

## Problem

The functionality for truncating HL7 field values already exists in our Covid-19 pipeline, but we would like to extract this into something reusable for the Universal Pipeline. 

## Goal

Simplify HL7 truncation logic and put it in one place that can be used by both the Covid and Universal Pipeline. Comprehensively test new code to ensure that old pipeline remains intact and new features can be introduced without breaking the existing functionality.

## Implementation

 - Extract out max length calculation logic into a separate class that is then injected into our Hl7Serializer class.
 - Extract out max length constants into its own object, so we’re not overloading the Hl7Serializer class.
 - Ensure configuration options are passed directly rather than as a part of the larger Hl7Configuration object which is only relevant to the Covid Pipeline
 - Add the following configuration parameters to function
   - ```truncateHDNamespaceIds: Boolean```
   - ```truncateHl7FieldsToSpec: List<String>```
   - ```truncateCustomHl6FieldsToCustomLength: Map<String, Int>```
 - Logic
   - Check the existing HD namespace logic since that is a special case
   - Check if the field exists in the custom length map and select the configured value as its max length
   - Check if the field exists in the spec list and select the spec value as its max length
   - Otherwise, return null if none of the conditions match (this will let us know to not truncate at all)
 - Add a new function to the `TranslationFunctions` interface called `truncateHL7Field`
   - Inject our new class into implementations of `TranslationFunctions`
   - Pass the call through to our common function that we extracted earlier in each implementation class
 - Move new code to FHIRengine packages

## Operations

### Rollout

Once merged, there should be no special considerations for getting the code deployed and live. It should continue to work how it did in the past.

### Performance

This feature should contain no coroutines or network calls so there will be no bottlenecks.

### Error handling

If errors occur during the truncation logic, we should return the entire value to ensure no data is lost. We should also heavily log error cases so that we can spot any misconfigurations.

## Open Questions

Is the custom length logic truly unnecessary? I believe it is an easy add and could prove useful in the future if someone asks for it. The vast majority will just step over the custom length logic.

## Followups

## Update log

