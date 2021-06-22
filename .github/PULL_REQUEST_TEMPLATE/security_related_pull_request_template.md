---
name: "Security-related Pull Request"
about: "This pull request template is intended to be used when there is a change in our attack surface."
title: "Security-related Pull Request"
labels: "security"
assignees: "ronaldheft-usds", "td-usds", "jimduff-usds", "RickHawesUSDS"
---


This PR contains changes which require special attention to security-related aspects


## Changes
-
-

## Specific Security-related subjects a reviewer should pay specific attention to

- Newly introduced enpoints
    - new endpoint A
    - new endpoint B
- What are the potential security threats and mitigations? Please list the STRIDE threats and how they are mitigated
    - **S**poofing
        - Threat T, which could be achieved by A, is mitigated by M
    - **T**ampering
    - **R**epudiation
    - **I**nformation disclosure
    - **D**enial of service
    - **E**levation of Privilege
- Does this PR include changes in authentication and/or authorization of existing endpoints?
- Does this change require changes to our infrastructure?
- Does this change introduce new dependencies that need vetting?

## Checklist

### Testing
- [ ] Tested locally?
- [ ] Ran `quickTest all`?
- [ ] Ran `./prime test` against local Docker ReportStream container?
- [ ] Downloaded a file from `http://localhost:7071/api/download`?
- [ ] Added tests?

### Security
- [ ] Did you check for sensitive data, and remove any?
- [ ] Does logging contain sensitive data?
- [ ] Are there licensing issues with any new dependencies introduced?

### Process
- [ ] Includes a summary of what a code reviewer should verify?
- [ ] Updated the release notes?
- [ ] Database changes are submitted as a separate PR?
- [ ] DevOps team has been notified if PR requires ops support?

## Fixes
*List GitHub issues this PR fixes*
- #issue

## To Be Done
*Create GitHub issues to track the work remaining, if any*
- #issue
