# [17171] How to Filter by Individual LOINC Codes

Documentation is required for filtering by individual LOINC codes instead of using condition codes so that engineers can reference it when onboarding STLTs.

## Difference Between Condition Codes and LOINC Codes

ReportStream utilizes SNOMED, LOINC and site-specific condition codes (see [Mapping sender codes (Compendium) to Reportable conditions](sender-onboarding/mapping-sender-codes-to-condition.md)) in a compendium form as a lookup table (`lookup_table_version` is basically an aggregated compendium of sender compendiums).  Filtering based on condition code (see [How to test receiver transforms,settings and filters](receiver-onboarding/receiver-testing.md)) only works if the FHIR **Bundle** under consideration has had the **Observation** in question annotated with a ReportStream-specific extension:

```
        {
            "fullUrl": "Observation/1667861767955966000.f3f94c27-e225-4aac-b6f5-2750f45dac4f",
            "resource": {
                "resourceType": "Observation",
                "id": "1667861767955966000.f3f94c27-e225-4aac-b6f5-2750f45dac4f",
                ...
                "code": {
                    "coding": [
                        {
                            "extension": [
                                {
                                    "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code",
                                    "valueCoding": {
                                        "system": "SNOMEDCT",
                                        "code": "840539006",
                                        "display": "Disease caused by severe acute respiratory syndrome coronavirus 2 (disorder)"
                                    }
                                }
                            ],
                            "system": "http://loinc.org",
                            "code": "94558-4"
                        }
                    ],
                    "text": "SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay"
                },
                ...
```


## Where Does This Apply in ReportStream
Primarily in **FHIRReceiverFilter** with the application of the [FHIRPath](https://build.fhir.org/fhirpath.html) expressions to the **Observation** resources.  In reality, though, for purposes of use within ReportStream one only needs to be concerned about creating an equivalent expression utilizing LOINC codes instead of condition codes.  The logic inside of the ReportStream application remains the same.

### Condition Code FHIRPath Example

`(Bundle.entry.ofType(Observation).code.coding.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code').value.where(code in ('840539006')).exists())`

### LOINC Code FHIRPath Example

`(Bundle.entry.ofType(Observation).code.coding.where.(system = 'http://loinc.org' and code = '94558-4').exists())`

`(Bundle.entry.ofType(Observation).code.coding.where.(system = 'http://loinc.org' and (code = '95209-3' or code = '94558-4')).exists())`

### Simple Demonstration
We can demonstrate this by simply using our existing tools with a file containing the **Observation** shown above:

```
% ./prime fhirpath -i src/test/resources/fhirengine/engine/routing/valid.fhir                                                                                           
...
Using constants:
        rsext='https://reportstream.cdc.gov/fhir/StructureDefinition/'

Using the FHIR bundle in /Users/bill/projects/report-stream/prime-reportstream/prime-router/src/test/resources/fhirengine/engine/routing/valid.fhir...
Special commands:
        !![FHIR path]                     - appends specified FHIR path to the end of the last path
        quit, exit                       - exit the tool
        reset                            - Sets %resource to Bundle
        resource [=|:] [']<FHIR Path>['] - Sets %resource to a given FHIR path

%resource = Bundle
Last path = 
FHIR path> (Bundle.entry.ofType(Observation).code.coding.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code').value.where(code in ('840539006')).exists())
Primitive: BooleanType[false]
Number of results = 1 ----------------------------

%resource = Bundle
Last path = (Bundle.entry.ofType(Observation).code.coding.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code').value.where(code in ('840539006')).exists())
FHIR path> (Bundle.entry.ofType(Observation).code.coding.where.(system = 'http://loinc.org' and code = '94558-4').exists())
Primitive: BooleanType[false]
Number of results = 1 ----------------------------

%resource = Bundle
Last path = (Bundle.entry.ofType(Observation).code.coding.where.(system = 'http://loinc.org' and code = '94558-4').exists())
FHIR path> (Bundle.entry.ofType(Observation).code.coding.where.(system = 'http://loinc.org' and (code = '95209-3' or code = '94558-4')).exists())
Primitive: BooleanType[false]
Number of results = 1 ----------------------------

%resource = Bundle
Last path = (Bundle.entry.ofType(Observation).code.coding.where.(system = 'http://loinc.org' and (code = '95209-3' or code = '94558-4')).exists())
FHIR path> 

```








