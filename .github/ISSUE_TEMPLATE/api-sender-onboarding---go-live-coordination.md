---
name: API Sender Onboarding – Go Live Coordination
about: This Issue is used for "go live coordination" for the individual sender's API
  Sender Onboarding epic.
title: "[name of company] Onboarding – Go Live Coordination"
labels: onboarding-ops, sender
assignees: ''

---

### Acceptance Criteria 
- [ ] Contacted sender to confirm when they want to start sending  
- [ ] Introduced sender to the Customer Support form and CC: the Ops team (at least Sharon Liu and Berni Xiong)  
- [ ] Adjusted final settings:  
     - [ ] Modified organization.yml and created yamlitos  
          - [ ] Customer status = active  
          - [ ] For HL7 senders, schemaName: hl7/hl7-ingest-covid-19-prod 
     - [ ] Modified prod settings table  
     - [ ] Modified staging settings table 

### To Do 
- [ ] RS engineer: update this ticket with comments with every interaction with sender and ops team 
- [ ] RS team member who originates this ticket: add dependencies:  
     - [ ] [name of company] Onboarding – Create Org and Sender Settings 
     - [ ] [name of company] Onboarding - Validate Test Files 
     - [ ] name of company] Onboarding – Testing in Staging and Prod 
     - [ ] [name of company] Onboarding – Validate Receiver 
- [ ] RS team member who originates this ticket: complete the following when creating this Issue. 
     - [ ] Pipelines: Onboarding & Operations (New) 
     - [ ] Labels: "Onboarding-ops" and "Sender" 
     - [ ] Epics: [select the epic for this API onboarding sender]
