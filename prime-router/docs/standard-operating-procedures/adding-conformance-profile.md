# Adding support for a new HL7v2 conformance profile in ReportStream

This section outlines the process of integrating a HL7v2 conformance profile so that a new data model can be
processed by ReportStream.

## Background

ReportStream uses FHIR R4 as the internal data model. Data coming into the system is converted to FHIR then data  
output can be transformed into a variety of formats dependent on the requirements of the receiver. This design  
intentionally decouples the input from the output so that the formats of the two can be different.

### Editing existing vs Creating New

Integrating a new conformance profile involves editing existing data structures and mappings or creating new ones.
Modifying an existing structures and mappings is preferred, but that option depends on the differences present in the
new conformance profile. Here are a few example scenarios:

- **The new profile only deprecates fields**
    - No changes are needed to structures or mappings.
- **The new profile adds new fields**
    - The existing custom structure can be edited by adding the new fields. A new structure is not needed.
- **The new profile changes the datatype of existing fields**
    - If the datatypes are backwards compatible then the existing custom structure can be edited. Otherwise new
      structures and mappings are needed.

## Data Structures

### HL7

ReportStream uses the [HAPI HL7v2](https://github.com/hapifhir/hapi-hl7v2) library to parse HL7v2 messages. That
library  
includes structures from v2.1 to v2.8.1.

#### Steps to edit structure:

1. Find the existing custom structure to be edited. Current custom structure is
   located `prime-router/src/main/java/fhirengine/translation/hl7/structures/radxmars251`
2. Starting with the file located in the `/message` directory and named with the HL7 message type (ex ORU_R01,
   ORM_O01,  
   ADT_A01, etc.) proceed through the structure to find the class that needs to be altered.
    - For example if there is a change to PID.7 you would open the `PATIENT_RESULT` class then `PATIENT` class
      then `PID` class where the edit can be made.

#### Steps to add structure:

1. Add package to `prime-router/src/main/java/fhirengine/translation/hl7/structures/` to house structure
2. Add a package named "message". Within that, add a java class named with the HL7 message type (ex ORU_R01, ORM_O01,  
   ADT_A01, etc.). This package and class are required.
3. Add a package named "group". Within that, add java classes for the HL7 groups present in the conformance profile.
4. Add a package named "segment". Within that, add java classes for the HL7 segments present in the conformance profile.
5. Add a package named "datatype". Within that, add java classes for the HL7 datatypes present in the conformance  
   profile.

- Review the java classes in the corresponding packages for other data structures to see examples of how those classes  
  should be arranged.

#### Choosing a Message Model

Once the new structure is created the ReportStream code needs to be updated to use said structure. Steps to take are:

1. `HL7Reader.getMessageProfile()` returns a MessageProfile object including two properties: typeID and profileID.
   Both  
   properties are required to determine the correct structure. First, `typeID` is derived from `MSH-9`
   then `profileID`  
   is looked up via `oidProfileMap`. The key for that map is the OID found in `MSH-21-3`. The expected OID needs to be  
   added to that map and paired with string value to be used as the profileID.
    - Note: This strategy may change as the values that can be received via `MSH-21-3` are not under our control. An
      alternate proposed solution is to add a property to the topic or sender.
2. The structure used to parse a given HL7 message is determined in `HL7Reader.getMessageModelClasses()`. Review that  
   function to ensure the combination of `typeID` and `profileID` will return the appropriate structure.

## Mapping

The first step to adding mapping support for a new conformance profile is to review existing mapping suites with an eye
for re-usability.
Mappings are structured with base mappings separate from mappings that deviate from the base specification.

You can see the separation in the mapping for v251-elr. That conformance profile is closely aligned with the profile for
v251. That presents the opportunity for reuse.

### Editing existing mapping suite

#### HL7 to FHIR

1. Find the existing mapping suite to be edited. Current mapping suite is
   located `prime-router/metadata/HL7/catchall/hl7`
2. Starting with the file located in the `/message` directory and named with the HL7 message type (ex ORU_R01,
   ORM_O01,  
   ADT_A01, etc.) proceed through the mapping to find the mapping element(s) that should be altered.
    - For example if there is a change to PID.7 you would open the `segments/PID/Patient` schema where the edit can be
      made.

#### FHIR to HL7

1. Find the existing mapping suite to be edited. Current mapping suite is
   located `prime-router/src/main/resources/metadata/HL7/catchall/hl7_mapping/`
2. Start with the file located in the directory named with the HL7 message type (ex ORU_R01, OML_O21,  
   ADT_A01, etc.) and named with the message type (ORU_R01-base.yml, ORU_R01-base.yml, OML_O21.yml, etc.). From that
   file proceed through the mapping to find the mapping element(s) that should be altered.
    - For example if there is a change to PID.7 you would open the `ORU_R01/base/patient-result/patient-result.yml`
      schema then `ORU_R01/base/patient-result/patient/patient-base.yml`
      then `ORU_R01/base/patient-result/patient/patient.yml` where the PID.7 element can be edited.

### Adding new mapping suite

#### HL7 to FHIR

1. Review existing data type mappings located in `prime-router/metadata/HL7/datatypes/` for reuse
2. Add folder to `prime-router/metadata/HL7/` with a name indicating the new conformance profile
3. Copy folder `fhir` from `prime-router/metadata/HL7/catchall` and add to your new mapping folder.
4. Create folder `hl7` in your new mapping folder with the following sub-folders:
    - "message" - **required** - within that, add a yml file named with the HL7 message type (ex ORU_R01, ORM_O01,  
      ADT_A01, etc.)
    - "resource" - **required** - within that, add an empty file `Common.yml`. This file is required as an overwrite
      of  
      the library.
    - "segments" - **required** - within that, add folders for segments identified in the conformance profile
    - "codesystem" - **required** - two files are needed in this folder: `CodingSystemMapping.yml` and  
      `ExtensionUrlMapping.yml`. Review other mapping suites and copy those files to use as a starting place then  
      update as needed.
    - "datatypes" - **optional** - this folder will hold any new datatypes or datatypes that deviate from the base  
      datatypes
    - "extension" - **optional** - this folder will hold some base extension mappings.
5. Starting with message type file in your `/message` directory build the mappings.  
   The [Templating Guide](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TEMPLATING.md) can be  
   helpful.

#### FHIR to HL7

1. Review existing datatype mappings located in `prime-router/src/main/resources/metadata/hl7_mapping`
2. Add folder to `prime-router/src/main/resources/metadata/hl7_mapping` with a name indicating the new conformance  
   profile
3. Add YML file for the HL7 message type (ex ORU_R01, ORM_O01, ADT_A01, etc.)
    - This file will be the starting point for mapping
4. Create a folder named "base". This will hold folders and sub-folders for the HL7 message groups. See  
   the `prime-router/src/main/resources/metadata/hl7_mapping/v251-elr/` for an example
5. Create a folder "resources". This will hold yaml files from mapping directly from FHIR resources
6. Create a folder "datatypes". This folder will hold any new datatypes or datatypes that deviate from the base  
   datatypes

## Resources

- The FHIR to HL7 mappings extensively use [FHIR Path](https://hl7.org/fhirpath/N1/) to access values in FHIR
- [Mapping schema file structure](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/design/design/mapping-schemas.md)  
  has good information on building elements in the mappings files

# Adding/Updating Validation Configuration(s) for Profiles

This section outlines how to add or update validation configurations/files for a particular conformance profile.

## Background

HL7v2 and FHIR have tools/libraries that take in a file containing the rules of a particular conformance profile and run
them against a message to determine if the message is "valid". ReportStream has the ability to configure conformance
profile validation per topic, see `Topic` in `prime-router/src/main/kotlin/SettingsProvider.kt`. The validation files
are stored in `prime-router/src/main/resources/metadata` and the number and format of those files depends on the
underlying format/library.

## HL7v2 Validation Files

ReportStream uses https://hl7v2-gvt.nist.gov/gvt/#/cf and its underlying scala library to validate HL7v2 messages
against
a particular conformance profile. nist.gov has a built-in conformance profile building tool and published profiles can
be located in the top-right drop down.

### Adding or Updating HL7v2 Validation Files for a Particular Profile

Go to https://hl7v2-gvt.nist.gov/gvt/#/cf and:

1. Select the Tool Scope for the profile the needs to be added or updated in the top right
    - If the "Tool Scopes" dropdown doesn't appear, try refreshing
2. Go to the documentation tab
3. Select the profile specific sub-tab next to the "General Documents" sub-tab, like "RADx MARS HL7v2's Documents"
4. Go to "Test Case Documentation" and download the files under the following columns:
    - Conformance Profile
    - Constraints
    - CoConstraints
    - ValueSet Library
    - ValueSet Bindings
    - Slicings

5. Create (if adding new profile support) or locate (if updating existing profile) the validation profile folder in
   ReportStream. Example: `prime-router/src/main/resources/metadata/hl7_validation/v251/radxmars`
6. Rename the downloaded files, if needed, to match the file names as shown
   in `gov.cdc.prime.router.validation.AbstractItemValidator.Companion.getHL7Validator`
7. If adding support for a new profile, follow the example of `gov.cdc.prime.router.validation.MarsOtcElrValidator` to
   create a new validator class for the profile

### Adding or Updating FHIR Validation Files for a Particular Profile

TBD, no use-case exists for FHIR validation at the moment.