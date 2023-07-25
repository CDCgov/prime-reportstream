---
name: 'API Sender Onboarding – Create Org and Sender Settings '
about: This Issue is used for "creating org and sender settings" for the individual
  sender's API Sender Onboarding epic.
title: "[name of company] Onboarding – Create Org and Sender Settings  "
labels: onboarding-ops, sender
assignees: ''

---

### Acceptance Criteria 
- [ ] Added to organization.yml and create yamlitos  
    - [ ] Customer status = testing  
    - [ ] For HL7 senders, schemaName: hl7/hl7-ingest-covid-19-training  

- [ ] Added to prod settings table  
- [ ] Added to staging settings table  
- [ ] Key exchange  
    - [ ] 1. Shared secret  
         - [ ] Created API keys in Azure staging   
         - [ ] Created API keys in Azure prod  
         - [ ]  Sent API keys to Sender via Keybase  
     - [ ] 2. Token auth  
         - [ ] Received public key from sender in staging  
              - [ ] Saved public key in keybase  
         - [ ] Received public key from sender in prod  
              - [ ] Saved public key in keybase  
         - [ ] Associated public key with sender settings in staging  
         - [ ] Associated public key with sender settings in prod 

FYI: this ticket can be completely concurrently with “Validate Test Files” ticket for this API sender 

### To Do 
- [ ] RS engineer: update this ticket with comments with every interaction with sender 
- [ ] RS team member who originates this ticket: add any dependencies, as needed 
- [ ] RS team member who originates this ticket: complete the following when creating this Issue. 
     - [ ] Pipelines: Onboarding & Operations (New) 
     - [ ] Labels: "Onboarding-ops" and "Sender" 
     - [ ] Epics: [select the epic for this API onboarding sender]
