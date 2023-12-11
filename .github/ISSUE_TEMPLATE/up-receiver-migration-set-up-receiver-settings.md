---
name: Receiver UP Onboarding Receiver Settings Template
about: This is the first step in migrating a STLT to the UP
title: "[name of STLT] - UP Migration-Receiver Translation Settings"
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
- [ ] Use the attached SimpleReport covid postman collection and make sure the message gets routed to [STLT] locally. Modify the message to meet [STLT] filter if needed [Simple Report Covid 3.postman_collection.txt](https://api.zenhub.com/attachedFiles/eyJfcmFpbHMiOnsibWVzc2FnZSI6IkJBaHBBNFU2QWc9PSIsImV4cCI6bnVsbCwicHVyIjoiYmxvYl9pZCJ9fQ==--1d3699f4819e80309b51b02e7d4fad05048c28d8/Simple%20Report%20Covid%203.postman_collection.txt)

- [ ] Make a copy of the [STLT] organization settings to onboard them to the UP. See How to Onboard a receiver to the UP here: https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/onboarding-users/receivers.md
- [ ] Use this Postman collection to send a FHIR bundle the UP and make sure the message gets routed to the new UP [STLT] receiver. You may need to update the Simple Report sender to use the simple-report-sender-transform.yml if it's not using it. [Simple Report UP 2.postman_collection.txt](https://api.zenhub.com/attachedFiles/eyJfcmFpbHMiOnsibWVzc2FnZSI6IkJBaHBBNFk2QWc9PSIsImV4cCI6bnVsbCwicHVyIjoiYmxvYl9pZCJ9fQ==--dfab4aacbbb0df15ad1e97bb3328f29bd71ff86b/Simple%20Report%20UP%202.postman_collection.txt)

- [ ] Use a diff tool of your choice and compare the two messages generated between the two pipelines and document the differences. You can find an example here [RI/SR UP vs Covid](https://docs.google.com/spreadsheets/d/197AeFMvozqUGRE1BuvOSMiUL_r2EEkyQv4l8D_OhhZk/edit#gid=492389121)


### Acceptance Criteria 
- [ ] Created and sent data to [STLT] through the covid pipeline locally
- [ ] Created and sent data to [STLT] through the universal pipeline locally
- [ ] Compared messages from the covid and universal pipelines and documented differences
