---
name: ReportStream Team Member Offboarding
about: Checklist for offboarding team members when they leave the project.
title: "[NAME] Offboarding"
labels: chore
assignees: ''

---

# Offboarding checklist

Note: please do not put sensitive information here (issues are public)

Initially assign to team lead for person being offboarded.

## Workflow:
Ideally some of these items can be done by person offboarding
- [ ] Reassign any github issues
- [ ] Verify no open git PRs
- [ ] Delete any old, open branches (maybe work in progress)
- [ ] Run `/reminder list` in slack and remove/transfer any reminders 
- [ ] Set Away message in CDC Outlook saying they've left and who to contact instead. 
This is required since it takes time for CDC to deactivate their account.

## General Access:
- [ ] Deactivate Okta accounts (Search for name, agency email, and contractor email)
  - [ ] staging
  - [ ] prod
- [ ] Slack access to channels
- [ ] Metabase
  - [ ] staging
  - [ ] prod
- [ ] Office 365 (CDC as a result of ActiveDirectory)
- [ ] Keybase
  - [ ] VPN creditials in `/prime_dev_ops/vpn` (revoke?)
  - [ ] Access to keybase itself
- [ ] Mural
- [ ] Lucid Chart
- [ ] SendGrid access
- [ ] Any additional CDC-specific off-boarding (did they get a PIV?)

## Engineering specific:
- [ ] Change GitHub access level
- [ ] Zenhub
- [ ] Azure (CDC as a result of ActiveDirectory)
- [ ] PagerDuty
- [ ] Splunk
- [ ] Akamai
- [ ] Docker Desktop license assignment
