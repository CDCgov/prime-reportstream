---
name: UP Migrating Receiver Tranlsation Settings Template
about: This is the second step in migrating a STLT to the UP and is only needed if the receiver has translation settings
title: "[name of STLT] - UP Migration-Receiver Translation Settings"
labels: onboarding-ops, receiver
assignees: ''

---

### User Story:
As a developer, I want to set up [STLT] receiver setting settings in the UP, so that they can receive data in the format they expect.

### Description/Use Case
[SLTS] is already receiving data from the covid pipeline they will be onboarded to the UP where they will be receiving data from SimpleReport including Covid, Flu and RSV. We need to make sure the message we generate from the UP matches the Covid pipeline message.

### Risks/Impacts/Considerations


### Dev Notes:

- To migrate the Covid translation settings start by looking at their current translation settings. If the receiver uses  any of the following settings you will need to create a receiver schema:
	- receivingApplicationName
	- receivingApplicationOID
	- receivingFacilityName
	- receivingFacilityOID
	- messageProfileId
	- replaceValue
	- replaceValueAwithB
	- reportingFacilityName
	- reportingFacilityId
    - reportingFacilityIdType
    - suppressQstForAoe: true
    - suppressHl7Fields
    - suppressAoe: true
    - defaultAoeToUnknown
    - replaceUnicodeWithAscii
    - useBlankInsteadOfUnknown
    - usePid14ForPatientEmail: true
    - suppressNonNPI
    - replaceDiiWithOid
    - useOrderingFacilityName not STANDARD
    - nameFormat not STANDARD
    - receivingOrganization
    - stripInvalidCharsRegex
    
   - More documentation on how to set-up these transforms in the UP will be provided, but for now you can look for examples on how to set this up in either the NY-receiver-transforms or CA-receiver-transforms

- If the receiver uses any of those transforms you will need to create a receiver transform under `metadata/hl7_mapping/receivers/STLTs/` and update the receiver settings to point to this schema.

- After migrating the receiver setting to the UP. Send another test message using the postman collection or feel free to use the PrimeCLI to test the sender and receiver transforms using this command:
`./prime fhirdata --input-file {PATH TO INPUT FILE} -s metadata/fhir_transforms/senders/SimpleReport/simple-report-sender-transform.yml --output-format HL7 -r metadata/hl7_mapping/receivers/STLTs/{RECEIVER SCHEMA} --output-file {PATH TO OUTPUT FILE}`

- Compare the two messages again and document if there are any differences between the two messages. If there are review them with the team. We will probably need to ask the sender to add missing data or add a sender transform.

- If there are no major differences we can move on to sending test messages to the STLTs staging environment.

### Acceptance Criteria 
- [ ] Migrated  Covid receiver translation settings to the UP receiver settings
- [ ] Successfully generated a message with migrated UP receiver settings
- [ ] Compared messages from the covid and universal pipelines and documented differences
