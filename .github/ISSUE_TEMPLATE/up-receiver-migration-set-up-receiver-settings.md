---
name: UP Migrate Receiver - Receiver Migration Pre-Work
about: This is the first step in migrating a STLT to the UP
title: "[name of STLT] - UP Receiver Migration Pre-Work"
labels: onboarding-ops, receiver
assignees: ''

---

### User Story:
As a developer, I want to compare the messages generated from the Covid and Universal pipelines, so that I can successfully onboard [STLT] to the UP

### Description/Use Case
[STLT] is already receiving data from the covid pipeline they will be onboarded to the UP where they will be receiving data from SimpleReport including Covid, Flu and RSV. We need to make sure the message we generate from the UP matches the Covid pipeline message.

### Risks/Impacts/Considerations


### Dev Notes:

- [ ] Fetch [STLT] organization settings from production and load them locally
- [ ] Use the attached SimpleReport covid postman collection and make sure the message gets routed to [STLT] locally. Modify the message to meet [STLT] filter if needed [Simple Report Covid.postman_collection](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/onboarding-users/samples/SimpleReport/Simple%20Report%20Covid.postman_collection.json)
- [ ] Make a copy of the [STLT] organization settings to onboard them to the UP. See How to Migrate an existing receiver to the UP documentation for more details: https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/onboarding-users/migrating-receivers.md
- [ ] Use this Postman collection to send a FHIR bundle the UP and make sure the message gets routed to the new UP [STLT] receiver. You may need to update the Simple Report sender to use the simple-report-sender-transform.yml if it's not using it. [Simple Report UP.postman_collection](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/onboarding-users/samples/SimpleReport/Simple%20Report%20UP.postman_collection.json)
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
    - receivingOrganization
    - stripInvalidCharsRegex

- More documentation on how to set-up these transforms in the UP will be provided, but for now you can look for examples on how to set this up in either the NY-receiver-transforms or CA-receiver-transforms

- If the receiver uses any of those transforms you will need to create a receiver transform under `metadata/hl7_mapping/receivers/STLTs/` and update the receiver settings to point to this schema.

- After migrating the receiver setting to the UP. Send another test message using the postman collection or feel free to use the PrimeCLI to test the sender and receiver transforms using this command:
`./prime fhirdata --input-file {PATH TO INPUT FILE} -s metadata/fhir_transforms/senders/SimpleReport/simple-report-sender-transform.yml --output-format HL7 -r metadata/hl7_mapping/receivers/STLTs/{RECEIVER SCHEMA} --output-file {PATH TO OUTPUT FILE}`

- Compare the two messages again and document if there are any differences between the two messages. If there are review them with the team. We will probably need to ask the sender to add missing data or add a sender transform.

- [ ] Use a diff tool of your choice and compare the two messages generated between the two pipelines and document the differences. You can find an example here [RI/SR UP vs Covid](https://docs.google.com/spreadsheets/d/197AeFMvozqUGRE1BuvOSMiUL_r2EEkyQv4l8D_OhhZk/edit#gid=492389121)

- If there are no major differences we can move on to sending test messages to the STLTs staging environment.


### Acceptance Criteria
- [ ] Created and sent data to [STLT] through the covid pipeline locally
- [ ] Created and sent data to [STLTS] through the universal pipeline locally
- [ ] Migrated  Covid receiver translation settings to the UP receiver settings
- [ ] Successfully generated a message with migrated UP receiver settings
- [ ] Review transforms settings with the team
- [ ] Compared messages from the covid and universal pipelines and documented differences and review with team
