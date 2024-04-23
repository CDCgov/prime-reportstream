# Adding a New Conformance Profile in ReportStream

This document outlines the process of integrating a conformance profile so that a new data model can be sent via  
ReportStream.

## Background

ReportStream uses FHIR R4 as the internal data model. Data coming into the system is converted to FHIR then data
output  
can be transformed into a variety of formats dependent on the requirements on the receiver. This design intentionally  
decouples the input from the output so that the formats of the two can be different.

## Data Structures

### HL7

ReportStream uses the [HAPI HL7v2](https://github.com/hapifhir/hapi-hl7v2) library to parse HL7v2 messages. That  
library includes structures for v2.1 to v2.8.1. When HL7 conformance profiles differ from the base version specs a new  
structure is needed. Steps to add a new structure:

1. Add package to `prime-router/src/main/java/fhirengine/translation/hl7/structures/`  to house structure
2. Add a package named "message". Within that, add a java class named with the HL7 message type (ex ORU_R01, ORM_O01,
   ADT_A01, etc.). This package and class are required.
3. Add a package named "group". Within that, add java classes for the HL7 groups present in the conformance profile.
4. Add a package named "segment". Within that, add java classes for the HL7 segments present in the conformance profile.
5. Add a package named "datatype". Within that, add java classes for the HL7 datatypes present in the conformance
   profile.

- Review the java classes in the corresponding packages for other data structures to see examples of how those classes
  should be arranged.

#### Choosing a Message Model

Once the new structure is created the ReportStream code needs to be updated to use said structure. Steps to do this:

1. `HL7Reader.getMessageProfile()` returns a MessageProfile object including two properties: typeID and profileID. Both
   properties are required to determine the correct structure. First, `typeID` is derived from `MSH-9` then `profileID`
   is looked up via `oidProfileMap`. The key for that map is the OID found in `MSH-21-3`. The expected OID needs to be
   added to that map and paired with string value to be used as the profileID.
2. The structure used to parse a given HL7 message is determined in `HL7Reader.getMessageModelClasses()`. Review that
   function to ensure the combination of `typeID` and `profileID` will return the appropriate structure.

## Mapping

When looking to add a new mapping suite the first step to review existing mapping suites with an eye for reusability.
Mappings are structured with base mappings separate from mappings that deviate from the base specification.

You can see the separation in the mapping for v251-elr. That conformance profile is closely aligned with the profile for
v251. That presents the opportunity for reuse.

### HL7 to FHIR

1. Review existing data type mappings located in `prime-router/metadata/HL7/datatypes/` for reuse
2. Add folder to `prime-router/metadata/HL7/` with a name indicating the new conformance profile
3. Copy folder `fhir` from `prime-router/metadata/HL7/v251-elr` and add to your new mapping folder.
4. Create folder `hl7` in your new mapping folder with the following subfolders:
    - "message" - **required** - within that, add a yml file named with the HL7 message type (ex ORU_R01, ORM_O01,
      ADT_A01, etc)
    - "resource" - **required** - within that, add an empty file `Common.yml`. This file is required as an overwrite of
      the library.
    - "segments" - **required** - within that, add folders for segments identified in the conformance profile
    - "codesystem" - **required** - two files are needed in this folder: `CodingSystemMapping.yml`
      and `ExtensionUrlMapping.yml`. Review other mapping suites and copy those files to use as a starting place then
      update as needed.
    - "datatypes" - **optional** - this folder will hold any new datatypes or datatypes that deviate from the base
      datatypes
5. Starting with message type file in your `/message` directory build the mappings.
   The [Templating Guide](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TEMPLATING.md) can be
   helpful.

### FHIR to HL7

1. Review existing datatype mappings located in `prime-router/src/main/resources/metadata/hl7_mapping`
2. Add folder to `prime-router/src/main/resources/metadata/hl7_mapping` with a name indicating the new conformance
   profile
3. Add YML file for the HL7 message type (ex ORU_R01, ORM_O01, ADT_A01, etc)
    - This file will be the starting point for mapping
4. Create a folder named "base". This will hold folders and sub-folders for the HL7 message groups. See
   the `prime-router/src/main/resources/metadata/hl7_mapping/v251-elr/` for an example
5. Create a folder "resources". This will hold yaml files from mapping directly from FHIR resources
6. Create a folder "datatypes". This folder will hold any new datatypes or datatypes that deviate from the base
   datatypes

#### Resources

- The FHIR to HL7 mappings extensively use [FHIR Path](https://hl7.org/fhirpath/N1/) to access values in FHIR