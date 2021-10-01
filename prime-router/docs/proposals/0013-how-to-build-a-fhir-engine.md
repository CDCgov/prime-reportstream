# How to build a FHIR engine
Directions from Here to There.

## Working Agreement
-   For any new feature, we will not add any hidden magic to existing behaviors, rather we will create new, properly encapsulated concerns
-   For every new feature implemented, we will evaluate how we can implement functionality that aligns to this future state architecture
-   In order to realize a revised architecture that enables longevity and improved velocity of the engineering team, we acknowledge that new features may take longer to implement

- [ ] accepted by Engineering
- [ ] accepted by Product

**North Star/s:**
- Longevity through clarity
- The potential to Ingest / Publish any health data
- Senders and Receivers control their own configuration

**Themes:**
- Extreme Clarity
- Running alongside / [strangler pattern](https://martinfowler.com/bliki/StranglerFigApplication.html)


## Internal FHIR

Goal: Safely and incrementally move internal modeling of data over to FHIR data structures.

This track will work on accepting data from a Large Hospital Group as HL7 and translating that data into FHIR models. 

### POC HL7 -> FHIR
Output of the first iteration of this is valid FHIR JSON saved to a file for an HL7 message received from the Large Hospital Group (These may only be example messages at this stage).
- https://github.com/CDCgov/prime-reportstream/issues/2328
- https://github.com/CDCgov/prime-reportstream/issues/2335
- https://github.com/CDCgov/prime-reportstream/issues/2329
- https://github.com/CDCgov/prime-reportstream/issues/2332

### Convert HL7 -> FHIR
How do we turn HL7 into FHIR?

Create `IConverter` or similar interface and an HL7 implementation of that interface.

`IConverter` should have the equivalent of:
```
is_valid() -> bool
convertToFHIR() -> FHIR Model
```

### Enrich FHIR
How do we enrich a FHIR message according to a Senders desires?
How do we enrich a FHIR message according to our desires?

### Determine Recipients of FHIR
How do we route Bundles to specific Receivers?

> **DELIVERABLE**: We now have a usable fully formed FHIR record stored by the system for HL7 messages.

### Transform FHIR
How do we transform FHIR into a format desired by a Receiver?

### Send Data to Receivers
Minimal work should be needed for this

> **DELIVERABLE**: HL7 Path is now moved over to FHIR

### Transform Current Internal to FHIR
How can we transform our custom internal model to FHIR?

>This will help establish patterns for transforming other CSVs and allow us to strangle the outbound half of the pipeline.

> **DELIVERABLE**: All outbound data now uses new work

### Convert Custom to FHIR
How do we Translate Sender specific headers to FHIR

Create an `IConverter` implementation for Custom CSV formats.
`is_valid` will check for both valid CSV formats _and then_ ensure it matches the Sender specific agreement (what fields they should be sending).
`convertToFHIR` will use sender specific settings (currently schemas) to map values in the CSV to values in FHIR.

> **DELIVERABLE**: Whole system now uses new engine
