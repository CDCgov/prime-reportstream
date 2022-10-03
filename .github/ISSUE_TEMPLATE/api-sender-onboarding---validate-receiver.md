---
name: API Sender Onboarding – Validate Receiver
about: This Issue is used for "validating a selected receiver" for the individual
  sender's API Sender Onboarding epic.
title: "[name of company] Onboarding – Validate Receiver  "
labels: onboarding-ops, sender
assignees: ''

---

### Acceptance Criteria 
- [ ] Contacted receiver to ask them to validate sample production data (real data) from sender  
- [ ] Contacted sender to inform which state we’ll be working with (need sample production data for that state)  
- [ ] Data received from sender for the selected state  
     - [ ] RS engineer accessed Azure “receive” folder in production to pull out reports from sender  
     - [ ] Ensured local receiver configuration for the state matches production  
          - [ ] Except the SFTP settings need to point to local  
     - [ ] RS engineer submitted reports to the local pipeline  
     - [ ] Engineer deliver processed reports to the state by whatever means they specify (email, SFTP, etc.)  
     - [ ] Contacted the state to inform them file is ready for validation  
     - [ ] Received confirmation from state that file is validated (clear to go Live after this step) 

### To Do 
- [ ] RS engineer: update this ticket with comments with every interaction with sender and receiver 
- [ ] RS team member who originates this ticket: add dependencies:  
     - [ ] [name of company] Onboarding – Create Org and Sender Settings 
     - [ ] [name of company] Onboarding - Validate Test Files 
     - [ ] [name of company] Onboarding – Testing in Staging and Prod 
- [ ] RS team member who originates this ticket: complete the following when creating this Issue. 
     - [ ] Pipelines: Onboarding & Operations (New) 
     - [ ] Labels: "Onboarding-ops" and "Sender" 
     - [ ] Epics: [select the epic for this API onboarding sender]
