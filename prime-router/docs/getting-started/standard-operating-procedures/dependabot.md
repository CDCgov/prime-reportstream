# Dependabot

## Introduction
As part of using GitHub, we have the dependabot service which automatically finds updates
for project dependencies and creates PRs for them. While this is really helpful, if you're
not careful with them, you can really mess up the application. Spider-Man rules apply here:
"With great power comes great responsibility"

Below are the steps for how to most safely check and merge in the dependabot PRs:

## Steps
1. Open the dependabot's pull request to be merged and identify if the PR is out of date from master.  If so, create a new comment in the PR with the text
   `@dependabot rebase` to let dependabot rebase the branch for you.  If you use any other method then dependabot will not be able to keep track of the PR.
2. Verify that the build for the PR is successful.  Note that the unit, integration and smoke tests are run as part of the build.
3. Read the updated library's changelog and identify and communicate any risks you find.  When in doubt ask! Library changes can affect many parts of the system.
4. Identify any library version conflicts for the updated library. This may happen when other libraries are dependent on a different version of the same library.  See
   [Identifying Library Version Conflicts](#identifying-library-version-conflicts)
5. Identify what places in the code the library is used then identify if the unit, integration and/or smoke tests provide enough coverage to verify the update does
   not break the baseline.  If the tests do not provide proper coverage then you MUST manually test as necessary to verify the library update BEFORE merging the update.
   When in doubt ask!
6. If not further testing is required then you can merge in the PR in GitHub
    1. Go to [Pull Requests](https://github.com/CDCgov/prime-reportstream/pulls) in Github and find the original branch (i.e. com.googlecode.libphonenumber-libphonenumber-8.12.31)
    2. Go to the Files changed tab click Review changes
    3. Leave a brief comment (i.e. "tested locally"), select Approve and click Submit review
    4. At the bottom of the Conversation tab, click on Update branch, then Enable auto-merge (squash)

## Identifying Library Version Conflicts
Updates to library versions can create conflicts when another library is dependent on an older version of the given library.
1. Run the command `./gradlew dependencies | grep <library name>`.
2. Look for different versions of the library.  If only one version is present then there are no issues with this update.  If more than one version is present then
   proceed to the next step.
3. Identify what libraries have the dependency to the older version by running `./gradlew dependencies > project_dependencies.txt` and inspecting
   the `project_dependencies.txt` file to trace the library dependencies.
4. Once identified, you need to look at what dependencies the libraries have and identify if other libraries need to be updated, if we cannot update the library,
   or if other specific steps need to be taken.  Each library is different and hence we cannot list all possible steps in this document.  Do your research and find
   a solution.

A real life example from 3/2022:
The library `com.fasterxml.jackson.core >> jackson-databind` was updated from version 2.13.1 to 2.13.2.  If you look at the dependecies for that library at
the [Maven Repository page](https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind/2.13.2) you can see that
`com.fasterxml.jackson.core >> jackson-core` also needs to be at version 2.13.2.  If you keep following the rabbit you will find that
`com.fasterxml.jackson.module >> jackson-module-kotlin` has a depency to those libraries, but at the time the `jackson-module-kotlin` has not been updated to version
2.13.2 and hence we could not update until all those libraries are matched.

## Dealing with Breaking Changes
If you have identified breaking changes then handle them in one of these ways:
1. If you can make the changes in a short amount of time then block the PR in GitHub by marking as "changes requested," make the necesary changes, then request
   other reviewers to review and approve the changes.  Document your findings in the PR as well.
2. If the changes are more extensive and require more time to make then open a new issue to deal with the changes and close the PR with a link to the issue.  Closing the
   PR will result in dependabot not opening a new one for that library name and version.
3. If it has been determined that the updated library is not to be used at all then close the PR with a related comment. Closing the
   PR will result in dependabot not opening a new one for that library name and version.

## Notes on Specific Library Updates
* Azure storage library bumps in version: the underlying `azurite` image in Docker gets out of sync. You will begin to get errors about `x-ms-version` but no
  other details about how to fix it. If you bump the Azure storage libraries and get an `x-ms-version` error,
  look at updating our version of `azurite` in Docker locally and testing again. More details are here: https://github.com/CDCgov/prime-reportstream/discussions/1830