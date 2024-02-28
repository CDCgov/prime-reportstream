---
name: Receiver Onboarding - Set Up Connection
about: This Issue is used for "setting up the connection" for the STLT's Onboarding
  epic.
title: "[STLT abbreviation] Onboarding - Set Up Connection"
labels: onboarding-ops, receiver
assignees: ''

---

### Acceptance Criteria 

CONNECTION TYPE:
If SFTP:
- [ ] 1. Submitted host name for CDC whitelisting
- [ ] 2. Retrieve username and PW from STLT 
- [ ] 3. Determine if they have public key authentication 
- [ ] 4. Add private info to Keybase 
- [ ] 5. Create Azure secret staging 
- [ ] 6. Create Azure secret production 
- [ ] 7. Test connection 

If SOAP:
- [ ] 1. Submitted host name for CDC whitelisting
- [ ] 2. Retrieve username and PW from STLT  
- [ ] 3. Create Azure secret staging 
- [ ] 4. Create Azure secret production 
- [ ] 5. Add a SOAP envelope 
- [ ] 6. Test connection 

If REST:
- [ ] 1. Submit host name for CDC whitelisting 
- [ ] 2. Retrieve username and PW from STLT 
- [ ] 3. Create private and public key 
- [ ] 4. Send public key to STLT 
- [ ] 5. Build REST transport [we can flesh out further if needed, could be another epic] 
- [ ] 6. Add private info to Keybase 
- [ ] 7. Create Azure secret staging 
- [ ] 8. Create Azure secret production 
- [ ] 9. Test connection 

If Download Site:
- [ ] 1. Retrieve email addresses for STLT staff that need to access the data 
- [ ] 2. Create Okta staging profiles for STLT staff that need access to data 
 - [ ] a. Ensure checkbox for Password setting 
- [ ] 3. Add profiles to STLT group in Okta staging 
- [ ] 4. Create Okta prod profiles for STLT staff that need access to data 
 - [ ] a. Ensure checkbox for Password setting 
- [ ] 5. Add profiles to STLT group in Okta production 

### To Do 
- [ ] RS engineer: update this ticket with comments with every interaction with receiver 
- [ ] RS team member who originates this ticket: add any dependencies, as needed 
- [ ] RS team member who originates this ticket: complete the following when creating this Issue. 
     - [ ] Pipelines: Onboarding & Operations (New) 
     - [ ] Labels: "Onboarding-ops" and "Receiver" and the state label
     - [ ] Epics: [select the epic for this receiver we're onboarding]
