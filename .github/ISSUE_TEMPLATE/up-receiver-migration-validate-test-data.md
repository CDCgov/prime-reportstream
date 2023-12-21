---
name: UP Migrating Receiver - Validate Test Data
about: This is the fourth step in migrating a STLT to the UP 
title: "[name of STLT] - UP Migration - Validate Test Data"
labels: onboarding-ops, receiver
assignees: ''

---

### User Story:
As a developer, I want the [STLT] to validate test messages in staging, so that they can receive data in the format they expect.

### Description/Use Case
[STLT] is already receiving data from the covid pipeline they will be onboarded to the UP where they will be receiving data from SimpleReport including Covid, Flu and RSV. We need to make sure the message we generate from the UP passes the STLTs validation. We can use SimpleReport to submit test messages to the STLT.


### Risks/Impacts/Considerations


### Dev Notes:

- [ ] Upload the newly created receiver settings to Staging
- [ ] Copy the receiver's secret key with the new receiver name in Azure's staging environment
- [ ] Verify we can connect to the newly created receiver
- [ ] Send a test message through Simple Report and make sure it gets routed to the STLT
- [ ] Send test messages according to the STLT needs

### Acceptance Criteria 
- [ ] Created and sent data using Simple Report
- [ ] Confirmed with STLT that data passes validation 
