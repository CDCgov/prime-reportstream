This PR ...

**If you are suggesting a fix for a currently exploitable issue, please disclose the issue to the prime-reportstream team directly outside of GitHub instead of filing a PR, so we may immediately patch the affected systems before a disclosure. See [SECURITY.md/Reporting a Vulnerability](https://github.com/CDCgov/prime-reportstream/blob/master/SECURITY.md#reporting-a-vulnerability) for more information.**

Test Steps:
1. *Include steps to test these changes*

## Changes
- *Include a comprehensive list of changes in this PR*
-

## Checklist

### Testing
- [ ] Tested locally?
- [ ] Ran `./prime test` or `./gradlew testSmoke` against local Docker ReportStream container?
- [ ] Added tests?

### Process
- [ ] Are there licensing issues with any new dependencies introduced?
- [ ] Includes a summary of what a code reviewer should test/verify?
- [ ] Updated the release notes?
- [ ] Database changes are submitted as a separate PR?
- [ ] DevOps team has been notified if PR requires ops support?

## Fixes
- #issue - List GitHub issues this PR fixes

## To Be Done
*Create GitHub issues to track the work remaining, if any*
- #issue

## Specific Security-related subjects a reviewer should pay specific attention to
- Does this PR introduce new endpoints?
    - new endpoint A
    - new endpoint B
- Does this PR include changes in authentication and/or authorization of existing endpoints?
- Does this change introduce new dependencies that need vetting?
- Does this change require changes to our infrastructure?
- Does logging contain sensitive data?
- Does this PR include or remove any sensitive information itself?

If you answered '_yes_' to any of the questions above, conduct a detailed Review that addresses at least:

- What are the potential security threats and mitigations? Please list the _STRIDE_ threats and how they are mitigated
    - **S**poofing (faking authenticity)
        - Threat _T_, which could be achieved by _A_, is mitigated by _M_
    - **T**ampering (influence or sabotage the integrity of information, data, or system)
    - **R**epudiation (the ability to dispute the origin or originator of an action)
    - **I**nformation disclosure (data made available to entities who should not have it)
    - **D**enial of service (make a resource unavailable)
    - **E**levation of Privilege (reduce restrictions that apply or gain privileges one should not have)
- Have you ensured logging does not contain sensitive data?
- Have you received any additional approvals needed for this change?

## Guiding questions for Author and Reviewer:

- Do these changes meet/improve code clarity?
	- Do these changes avoid introducing any magic?
- Do these changes improve separation of concerns? (Does this thing have more than one reason to change?)
- Do these changes follow Dependency Inversion, at least for 'top level' classes?
- Do these changes follow patterns already in the codebase? (If not, can / should they?)