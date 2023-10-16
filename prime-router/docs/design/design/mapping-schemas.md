# Mapping schema file structure

Translation between HL7v2 and FHIR requires schema files that define how data is translated from one format to another. As we have built out these mappings, shortcomings in the current layout of the files have become apparent. This proposed structure aims to be self-documenting and logical so that the mappings are more maintainable and reusable.

## HL7v2 to FHIR mapping file organization

### Legacy organization

Currently the schemas are organized by the FHIR resource or datatype the HL7v2 data is mapped to, like so:

- hl7/message/ORU_R01.yml
- hl7/resource/MessageHeader.yml
- hl7/datatype/Identifier.yml
- hl7/datatype/Identifer_XON.yml

### Updated organization

A self-documenting directory structure would allow the developer to see the source HL7v2 segment or datatype and the target FHIR mapping from its name and location alone. One such proposed format could implement the following structure:

- hl7/messages/_**[HL7v2 message type]**_/  ← message types, such as ADT_A01, ORU_R01, etc.
- hl7/segments/_**[HL7v2 segment]**_/_**[FHIR path]**_
- hl7/segments/_**[HL7v2 segment]**_/_**[HL7v2 datatype]**_/_**[FHIR path]**_ ← for segment specific datatype definitions
- hl7/datatypes/_**[HL7v2 datatype]**_/_**[FHIR path]**_ ← for standard datatype mappings

For message types, each message type would have a directory named after itself. Ideally, the only content within that directory should be a schema that references the other top level segments and datatypes. If there are segments or datatypes that need to be defined differently from the standard, then this directory would have its own segments and datatypes directories as needed to store schemas specific to this message type.

- hl7/messages/ADT_A01/ADT_A01.yml
- hl7/messages/ADT_A01/segments ← only if base level segment cannot be used

For segments and datatypes, the basic structure is to define the HL7v2 segment or datatype being mapped, followed by the FHIR path the data is being mapped to. For example:

- hl7/segments/MSH/MessageHeader.yml ← MSH maps to MessageHeader
- hl7/segments/MSH/HD/MessageHeader-source.yml ← HD datatype in MSH maps to MessageHeader.source
- hl7/datatypes/HD/Organization.yml ← HD datatype maps to Organization
- hl7/datatypes/MSG/Coding.yml ← MSG datatype maps to Coding
- hl7/datatypes/XON/Organization.yml ← XON datatype maps to Organization

## FHIR to HL7v2 mapping file organization

### Legacy organization

Currently, the schema files under the hl7_mapping directory, where all FHIR to HL7v2 mappings are located, are organized by HL7 segment. One base file can refer to other schema files in multiple places, like so:

- hl7_mapping/ORU_R01/ORU_R01-base.yml ← only schema elements
- hl7_mapping/ORU_R01/base/order-observation.yml ← only schema elements
- hl7_mapping/common/patient-contact.yml ← one hardcoded value element, but mostly schema elements
- hl7_mapping/common/datatype/xpn-person-name.yml ← value elements from resource defined in ORU_R01-base (Bundle.entry.resource.ofType(Patient).contact)

### Updated organization

In order for the directory structure to be self-documenting, it should follow a defined hierarchy. The following structure would make both the source of data and the target mapping immediately apparent:

- hl7_mapping/messages/_**[HL7v2 message type]**_/ ← target message types, such as ADT_A01, ORU_R01, etc.
- hl7_mapping/resources/_**[FHIR path to resource]**_/_**[HL7v2 segment or datatype]**_.yml
- hl7_mapping/datatypes/_**[FHIR path to datatype]**_/_**[HL7v2 segment or datatype]**_.yml

Similarly to the HL7v2 to FHIR message schemas, each message type would have a directory named after itself, with subdirectories for message specific resources or datatypes schemas.

- hl7_mapping/messages/ADT_A01/ADT_A01.yml
- hl7_mapping/messages/ADT_A01/resources ← only if special handling of resources is needed for this message type

For resources and datatypes, the basic structure is to define the FHIR path being mapped, followed by the HL7v2 segment that is being mapped using data from the given FHIR path. For example:

- hl7_mapping/resources/MessageHeader/SFT.yml ← elements of MessageHeader that map to SFT
- hl7_mapping/resources/MessageHeader/MSH.yml ← elements of MessageHeader that map to MSH
- hl7_mapping/resources/Organization/HD.yml ← elements of Organization that map to HD
- hl7_mapping/resources/Organization/XON.yml ← elements of Organization that map to XON

Schemas that reference resource elements should be stored in a directory that represents the element:

- resources/MessageHeader/source/HD.yml ← map MessageHeader.source
- resources/MessageHeader/destination/HD.yml ← map MessageHeader.destination

Datatypes follow the same pattern:

- datatypes/Coding/MSG.yml ← Coding maps to MSG
- datatypes/Meta/PT.yml ← Meta maps to PT
