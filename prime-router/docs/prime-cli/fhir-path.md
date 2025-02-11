# FHIR Path CLI Tool

## How to use

The fhirpath cli tool allows for you to evaluate FHIR path expressions using a FHIR bundle as input. This is especially 
useful for making sure a FHIR path is correct when testing schemas. 
It can also be used for inspecting a bundle using FHIR path.

```bash
Usage: prime fhirpath [<options>]
Options:
-i, --input-file=<path>  Input file to process
-c, --constants=<value>  a constant in the form of key=value to be used in FHIR Path. Option can be repeated.
-h, --help               Show this message and exit
```

### Use Case: Inspecting a bundle to find AOEs

```bash
./prime fhirpath -i junk/test.fhir
```

``` bash
Using constants:
        rsext='https://reportstream.cdc.gov/fhir/StructureDefinition/'

Using the FHIR bundle in /Users/User/Documents/prime-reportstream/prime-router/junk/test.fhir...
Special commands:
        !![FHIR path]                     - appends specified FHIR path to the end of the last path
        quit, exit                       - exit the tool
        reset                            - Sets %resource to Bundle
        resource [=|:] [']<FHIR Path>['] - Sets %resource to a given FHIR path

%resource = Bundle
Last path = 
```
``` bash
FHIR path> Bundle.entry.resource.ofType(Observation).where(meta.tag.code = "AOE")
```
```bash
- {  
        "id": "Observation/a8c79303-758d-3459-a16d-0f00a180b84b"
        "meta": org.hl7.fhir.r4.model.Meta@79add732
        "extension": [ 
                extension('https://reportstream.cdc.gov/fhir/StructureDefinition/obx-observation'),
  ]
        "identifier": [ 
                org.hl7.fhir.r4.model.Identifier@3be3e76c,
  ]
        "status": "Enumeration[final]"
        "code": org.hl7.fhir.r4.model.CodeableConcept@6c07ad6b
        "subject": Reference to Patient/c0d8eb6d-0a51-4ffa-830f-e82316f189b7
        "effective[x]": "DateTimeType[2024-12-31T23:48:37+00:00]"
        "performer": [ 
                org.hl7.fhir.r4.model.Reference@10ed037a,
  ]
        "value[x]": org.hl7.fhir.r4.model.CodeableConcept@76e4212
        "interpretation": [ 
                org.hl7.fhir.r4.model.CodeableConcept@23121d14,
  ]
        "method": org.hl7.fhir.r4.model.CodeableConcept@72af90e8
}
Number of results = 1 ----------------------------
%resource = Bundle
Last path = Bundle.entry.resource.ofType(Observation).where(meta.tag.code = "AOE")
```
This shows that the bundle has one AOE. If we want to see more details, we can drill further by appending to the 
previous FHIR path using the !! notation like so:
``` bash
FHIR path> !!.code
```
``` bash
- {  
      "coding": [
          org.hl7.fhir.r4.model.Coding@50cbcca7,
      ]
      "text": "Has symptoms related to condition of interest"
  }
  Number of results = 1 ----------------------------

```

### Use Case: Check why a bundle is not passing a filter

```bash
qualityFilter:
  - "Bundle.entry.resource.ofType(DiagnosticReport).result.resolve().where(method.empty() or value.coding.code.empty()).count()"
```
Input into fhirpath command

```bash
FHIR path> Bundle.entry.resource.ofType(DiagnosticReport).result.resolve().where(method.empty() or value.coding.code.empty()).count()
```
```bash
Primitive: IntegerType[0]
Number of results = 1 ----------------------------
```

Expressions in filters should evaluate to a boolean. 
In this case, the FhirPath is evaluating to 0 which is an IntegerType.

The tool will also output any errors so we can easily fix invalid FhirPath
```bash
FHIR path> Bundle.entry.resource.ofType(DiagnosticReport).result.resove()
```
```bash
{"message":"FHIRLexerException: Error in ?? at 1, 1: The name resove is not a valid function name. Trying to evaluate: Bundle.entry.resource.ofType(DiagnosticReport).result.resove().","thread":"main","timestamp":"2025-02-10T21:27:55.552Z","level":"ERROR","logger":"gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils"}
Number of results = 0 ----------------------------
````
### Use Case: Testing custom [FhirPath functions](../getting-started/fhir-functions.md) 

By default, %resource is set to Bundle. For convenience/readability we can change it and then test the function:

```bash
FHIR path>  resource=Bundle.entry.resource.ofType(Patient).address
```
```bash
FHIR path>  %resource.postalCode.getStateFromZipCode()
```
```bash
Primitive: OH
Number of results = 1 ----------------------------
```

---


[More CLI commands](README.md)