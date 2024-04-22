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
   ADT_A01, etc). This package and class are required.
3. Add a package named "group". Within that, add java classes for the HL7 groups present in the conformance profile.
4. Add a package named "segment". Within that, add java classes for the HL7 segments present in the conformance profile.
5. Add a package named "datatype". Within that, add java classes for the HL7 datatypes present in the conformance
   profile.

- Review the java classes in the corresponding packages for other data structures to see examples of how those classes
  should be arranged.

#### Choosing Message Model

Once the new structure is created the ReportStream code needs to be updated. Steps to do this:

1. `HL7Reader.getMessageProfile()` returns a MessageProfile object including two properties: typeID and profileID. Both
   properties are required to determine the structure. First, `typeID` is derived from `MSH-9`. Then `profileID` is
   looked up via `oidProfileMap`. The key for that map is the OID found in `MSH-21-3`. The expected OID needs to be
   added to that map and paired with string value to be used as the profileID.
2. The structure used to parse a given HL7 message is determined in `HL7Reader.getMessageModelClasses()`. Review that
   function to ensure the combination of `typeID` and `profileID` will return the appropriate structure.

## Mapping

