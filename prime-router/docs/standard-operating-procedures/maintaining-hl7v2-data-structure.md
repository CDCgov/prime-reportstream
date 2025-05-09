# Maintaining ReportStream's HL7v2 data structure

This section outlines the process of maintaining the data structure used by ReportStream to model HL7v2.

## Background

ReportStream uses FHIR R4 as the internal data model. Data coming into the system is converted to FHIR then data output 
can be transformed into a variety of formats dependent on the requirements of the receiver. This design intentionally 
decouples the input from the output so that the formats of the two can be different.

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

ReportStream uses the [HAPI HL7v2](https://github.com/hapifhir/hapi-hl7v2) library to parse HL7v2 messages. That library includes structures from v2.1 to 
v2.8.1. ReportStream uses a custom structure that is designed to be version agnostic; there is no use of the `NULLDT`
deprecated field type, and all fields use a maximally compatible type (e.g. `ST` and `CE` fields can be fully
represented by a `CWE` type). We settled on this approach as we found that in practice, vendors will pick and choose
elements from newer HL7v2 specifications as needed while not fully transitioning to the newer specification in other 
areas.

#### Steps to edit structure:

1. Find the existing custom structure to be edited. Current custom structure is
   located `prime-router/src/main/java/fhirengine/translation/hl7/structures/fhirinventory`
2. Starting with the file located in the `/message` directory and named with the HL7 message type (ex ORU_R01,
   ORM_O01,  
   ADT_A01, etc.) proceed through the structure to find the class that needs to be altered.
    - For example if there is a change to PID.7 you would open the `PATIENT_RESULT` class then `PATIENT` class
      then `PID` class where the edit can be made.
    - Only structures that required changes from the existing HAPI structures were created here. If you need to import a
      structure you can copy the existing HAPI structure and change the imports to refer to the new structure.

## Mapping

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
   located `prime-router/src/main/resources/metadata/hl7_mapping/`
2. Start with the file located in the directory named with the HL7 message type (ex ORU_R01, OML_O21,  
   ADT_A01, etc.) and named with the message type (ORU_R01-base.yml, ORU_R01-base.yml, OML_O21.yml, etc.). From that
   file proceed through the mapping to find the mapping element(s) that should be altered.
    - For example if there is a change to PID.7 you would open the `ORU_R01/base/patient-result/patient-result.yml`
      schema then `ORU_R01/base/patient-result/patient/patient-base.yml`
      then `ORU_R01/base/patient-result/patient/patient.yml` where the PID.7 element can be edited.


## Resources

- The FHIR to HL7 mappings extensively use [FHIR Path](https://hl7.org/fhirpath/N1/) to access values in FHIR
- [Mapping schema file structure](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/design/design/mapping-schemas.md) has good information on building elements in the mappings files

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