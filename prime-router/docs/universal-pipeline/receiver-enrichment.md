# Universal Pipeline Receiver Enrichment Step
After the [destination filter](./destination-filter.md) step has completed, each configured receiver will receive the converted FHIR bundle for potential further enrichment.  If the specific receiver in question has been configured with one or more enrichment schema names, each enrichment schema will be applied in turn and update the FHIR bundle.  These enrichments are small customizations that don't warrant the creation of an entirely new translation schema.

## Configuration

The configuration for this step is done on an individual receiver level.  The `enrichmentSchemaNames` is an attribute of the receiver element in the receiver YAML configuration that contains an array of file or classpath references which contain transforms that will be applied to the FHIR bundle.

### Transforms
The transforms are stored in the files enumerated in the `enrichmentSchemaNames` array.  For each element in the array a `FHIRTransform` object is instantiated with the individual enrichment schema name.  Those objects contain [FHIRPath](http://hl7.org/fhirpath/r2) expressions which are subsequently used to processes the bundle.  (Additional useful information regarding FHIRPath can be found in [FHIR Functions](../getting-started/fhir-functions.md).)

## How It Works
This step comes directly after the [destination filter](./destination-filter.md) step in the pipeline.  For every receiver configured for the given organization, the converted FHIR bundle from the destination filter step will be forwarded to the receiver enrichment step (**elr-fhir-receiver-enrichment** queue) of the pipeline.  If any enrichment transforms are defined, they will be applied to the bundle in order.  Once processing is completed the bundle is uploaded to Azure and control is passed to the [receiver filter](./receiver-filter.md) step.
