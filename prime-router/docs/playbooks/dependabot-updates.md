# How to do Dependabot Updates

## Introduction
As part of using GitHub, we have the dependabot service which automatically finds updates
for project dependencies and creates PRs for them. While this is really helpful, if you're
not careful with them, you can really mess up the application. Spider-Man rules apply here:
"With great power comes great responsibility"

Below are the steps for how to most safely check and merge in the dependabot PRs:

## Steps
1. Open the dependabot's pull request to be merged in and verify the build is successful.  Note that the unit, integration and smoke tests are run as part of the build.
2. Read the updated library's changelog and identify and communicate any risks you find.
3. Identify what places in the code the library is used then identify if the unit, integration and/or smoke tests provide enough coverage to verify the update does not break the baseline.  If the tests do not provide proper coverage then you MUST manually test as necessary to verify the library update BEFORE merging the update.
4. If not further testing is required then you can merge in the PR in GitHub
   1. Go to [Pull Requests](https://github.com/CDCgov/prime-reportstream/pulls) in Github and find the original branch (i.e. com.googlecode.libphonenumber-libphonenumber-8.12.31)
   2. Go to the Files changed tab click Review changes
   3. Leave a brief comment (i.e. "tested locally"), select Approve and click Submit review
   4. At the bottom of the Conversation tab, click on Update branch, then Enable auto-merge (squash)

## Exceptions
Sometimes you will encounter an error in the smoke test. If you think the change in the PR caused the error, then you block the PR in GitHub 
by marking as "changes requested" and assigning it to one of the team leads. If you're not sure
if the error was caused by the smoke test, you can run the smoke test a second time. You should
also look in the scroll of Docker messages for any possible exceptions that bubbled up. You should
document those in the PR.

Occasionally, as you merge in dependabot updates, more will appear. Remember you cannot merge them into your
current branch unless you do another `git fetch --all --prune`.

On occasion, a dependabot PR will try to update two dependencies at the same time. If you're not
paying attention this can cause you to have a merge conflict. If you want to rebase a dependabot PR
after you've merged in some updates you can leave a comment on the PR that looks like this: `@dependabot rebase`
and it will rebase its PR against master.

Another issue that you can encounter is that occasionally the Azure storage libraries will bump in version, and
the underlying `azurite` image in Docker is out of sync. You will begin to get errors about `x-ms-version` but no
other details about how to fix it. If you bump the Azure storage libraries and get an `x-ms-version` error, 
look at updating our version of `azurite` in Docker locally and testing again. More details are here: https://github.com/CDCgov/prime-reportstream/discussions/1830

If in doubt, ask!

