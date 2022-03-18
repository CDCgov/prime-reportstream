# How to do Dependabot Updates

## Introduction
As part of using GitHub, we have the dependabot service which automatically finds updates
for project dependencies and creates PRs for them. While this is really helpful, if you're
not careful with them, you can really mess up the application. Spider-Man rules apply here:
"With great power comes great responsibility"

Below are the steps for how to most safely check and merge in the dependabot PRs:

## Steps
1. Open the dependabot's pull request to be merged and identify if the PR is out of date from master.  If so, create a new comment in the PR with the text 
`@dependabot rebase` to let dependabot rebase the branch for you.  If you use any other method then dependabot will not be able to keep track of the PR.
1. Verify that the build for the PR is successful.  Note that the unit, integration and smoke tests are run as part of the build.
1. Read the updated library's changelog and identify and communicate any risks you find.  When in doubt ask! Library changes can affect many parts of the system.
1. Identify what places in the code the library is used then identify if the unit, integration and/or smoke tests provide enough coverage to verify the update does 
not break the baseline.  If the tests do not provide proper coverage then you MUST manually test as necessary to verify the library update BEFORE merging the update.
When in doubt ask!
1. If not further testing is required then you can merge in the PR in GitHub
   1. Go to [Pull Requests](https://github.com/CDCgov/prime-reportstream/pulls) in Github and find the original branch (i.e. com.googlecode.libphonenumber-libphonenumber-8.12.31)
   2. Go to the Files changed tab click Review changes
   3. Leave a brief comment (i.e. "tested locally"), select Approve and click Submit review
   4. At the bottom of the Conversation tab, click on Update branch, then Enable auto-merge (squash)

## Dealing with Breaking Changes
If you have identified breaking changes then handle them in one of these ways:
1. If you can make the changes in a short amount of time then block the PR in GitHub by marking as "changes requested," make the necesary changes, then request
other reviewers to review and approve the changes.  Document your findings in the PR as well.
1. If the changes are more extensive and require more time to make then open a new issue to deal with the changes and close the PR with a link to the issue.  Closing the 
PR will result in dependabot not opening a new one for that library name and version.
1. If it has been determined that the updated library is not to be used at all then close the PR with a related comment. Closing the 
PR will result in dependabot not opening a new one for that library name and version.

## Notes on Specific Library Updates
* Azure storage library bumps in version: the underlying `azurite` image in Docker gets out of sync. You will begin to get errors about `x-ms-version` but no
other details about how to fix it. If you bump the Azure storage libraries and get an `x-ms-version` error, 
look at updating our version of `azurite` in Docker locally and testing again. More details are here: https://github.com/CDCgov/prime-reportstream/discussions/1830

