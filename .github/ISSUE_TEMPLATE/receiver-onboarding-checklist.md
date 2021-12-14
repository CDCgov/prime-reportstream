---
name: Receiver Onboarding Checklist
about: Checklist of items to onboard receivers
title: ''
labels: ''
assignees: ''

---

The following items, generally in order, must be completed to successfully onboard a receiver to ReportStream:

* [ ] Added to organization.yml
* [ ] Added to organizations-local.yml
* [ ] Added to staging settings table
* [ ] Added to prod settings table
* [ ] Added at least one receiver
* [ ] Added OKTA group to download site
* [ ] Added customers to OKTA group for download site
* [ ] Is customer contact made?
* What doc format are we supporting for the receiver (HLV, CSV, other)? **(Add comment below)**
* What transport(s) are we supporting for the receiver (SFTP, other)? **(Add comment below)**
* [ ]  Got credentials?
* [ ] Tested ELR connection locally?
* [ ] Tested ELR connection from Azure?
* [ ] Has the receiver accessed the download site?
* [ ] Has the receiver been sent at least one test file?
* [ ] Has the receiver signed off on file formatting?
* [ ] Are there labs sending data?
* [ ] Has the receiver confirmed receipt of the data?
* [ ] Have the files processed successfully?
* [ ] Are we in production?
* [ ] Update customerStatus flag in production settings to active
