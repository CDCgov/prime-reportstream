---
name: UP Migrate Receiver - Move to Production and Monitor
about: This is the third and final step in migrating a STLT to the UP 
title: "[name of STLT] - UP Migration - Move to Production and Monitor"
labels: onboarding-ops, receiver
assignees: ''

---

### User Story:
As a developer, I want to move the [STLT] settings to production after getting approval from the STLT, so that they can start receiving live data.

### Description/Use Case
[STLT] is already receiving data from the covid pipeline they will be onboarded to the UP where they will be receiving data from SimpleReport including Covid, Flu and RSV. We need to make sure the message we generate from the UP passes the STLTs validation. We can use SimpleReport to submit test messages to the STLT.


### Risks/Impacts/Considerations


### Dev Notes:

- [ ] Upload the staging receiver settings to Production
- [ ] Copy the receiver's secret key with the new receiver name in Azure's production environment
- [ ] Verify we can connect to the newly created receiver
- [ ] Monitor the UP data for the conditions the receiver is interesed in and notify them when reports are routed to them
- [ ] Send a test message through Simple Report and make sure it gets routed to the STLT
- [ ] Send test messages according to the STLT needs

### Acceptance Criteria 
- [ ] Receiver UP settings are pushed to production
- [ ] ReportStream can connect to STLT in production
- [ ] Notify STLT when reports are routed to them for each condition they are receiving
