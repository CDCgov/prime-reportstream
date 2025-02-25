# FHIR Path CLI Tool

## How to use

The `fhirpath` CLI tool evaluates FhirPath expressions using a FHIR bundle as input. This is especially 
useful for making sure a FHIRPath is correct when testing schemas. 
It can also be used for inspecting a bundle using FHIRPath.

```bash
./prime fhirpath -h
```
Output:
```bash
Usage: prime fhirpath [<options>]
Options:
-i, --input-file=<path>  Input file to process
-c, --constants=<value>  a constant in the form of key=value to be used in FHIR Path. Option can be repeated.
-h, --help               Show this message and exit
```

Run the command with a FHIR bundle
```bash
./prime fhirpath -i junk/test.fhir
```
Output:
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
### Special Commands
#### Setting %resource
By default, `%resource` is set to `Bundle`. 
It can be changed by setting to a FHIRPath that evaluates to a single resource (as shown below). 
Run `reset` to revert `%resource` back to `Bundle`.
```bash
resource=Bundle.entry.resource.ofType(Patient)
```
Output:
```bash
%resource = Bundle.entry.resource.ofType(Patient)
Last path = 
```

#### Using Last Path
The tool keeps track of the FHIRPath that was last ran and is displayed alongside the results.

Input:
```bash
Bundle.entry.resource.ofType(Patient).name
```

Output:

```bash
{  
        "extension": [ 
                extension('https://reportstream.cdc.gov/fhir/StructureDefinition/xpn-human-name'),
  ]
        "family": "GARCIA"
        "given": [ 
                SUSANA,
  ]
}

Number of results = 1 ----------------------------

%resource = Bundle
Last path =  Bundle.entry.resource.ofType(Patient).name
```

Use `!!` to append to the last path:
```bash
!!.family
```
Output:

```bash
Primitive: GARCIA
Number of results = 1 ----------------------------

%resource = Bundle
Last path =  Bundle.entry.resource.ofType(Patient).name.family
```

### Use Case: Inspecting a bundle to find AOEs


Input a valid FHIRPath that returns AOE's for the bundle. For example,
``` bash
Bundle.entry.resource.ofType(Observation).where(meta.tag.code = "AOE")
```
Output:
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
This shows that the bundle has one AOE. Notice that `subject` contains a "Reference to Patient/c0d8eb6d-0a51-4ffa-830f-e82316f189b7"
Use `resolve()` to drill into the data of such fields.
``` bash
Bundle.entry.resource.ofType(Observation).where(meta.tag.code = "AOE").subject.resolve()
```


### Use Case: Check why a bundle is not passing a filter
Given the following quality filter,
```bash
qualityFilter:
  - "Bundle.entry.resource.ofType(DiagnosticReport).result.resolve().where(method.empty() or value.coding.code.empty()).count()"
```
Input:

```bash
Bundle.entry.resource.ofType(DiagnosticReport).result.resolve().where(method.empty() or value.coding.code.empty()).count()
```
Output:
```bash
Primitive: IntegerType[0]
Number of results = 1 ----------------------------
```

Expressions in filters should evaluate to a boolean. 
In this case, the FHIRPath is evaluating to 0 which is an IntegerType.

The tool will also output any errors so we can easily fix invalid FHIRPath
```bash
Bundle.entry.resource.ofType(DiagnosticReport).result.resove()
```
Output:
```bash
{"message":"FHIRLexerException: Error in ?? at 1, 1: The name resove is not a valid function name. Trying to evaluate: Bundle.entry.resource.ofType(DiagnosticReport).result.resove().","thread":"main","timestamp":"2025-02-10T21:27:55.552Z","level":"ERROR","logger":"gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils"}
Number of results = 0 ----------------------------
````
### Use Case: Testing custom [FHIRPath functions](../getting-started/fhir-functions.md) 
Input:
```bash
Bundle.entry.resource.ofType(Patient).address.postalCode.getStateFromZipCode()
```
Output:
```bash
Primitive: OH
Number of results = 1 ----------------------------
```

---


[More CLI commands](README.md)