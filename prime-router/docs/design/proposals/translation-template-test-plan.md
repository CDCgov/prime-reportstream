# Translation Template Test Plan

## Problem Statement
As a Reportstream engineer, I want tests that verify that the changes I make to translations will not break essential 
rules set forth by the spec or our users. 

Currently, we use integration tests for this purpose, but these do not give us the ability to easily tell which thing 
broke/changed or even really what is specifically being tested.

## Considerations
- We not only want to check the rules set forth in the spec, but the ones set forth by our users.

## Proposal on *what* to test
We should write tests when:
- The spec specifies a condition
- There is a requirement specified by a sender or receiver. This means that there needs to be a test for every rule in
  a sender or receiver transform, This includes the default transform since these are ReportStream specific rules we
  are implementing
- We have created an extension not specified in the spec

## Options for *how* to test 
1. Continue with integration tests
    - These would specific to the field they are trying to test rather than a cluster of fields
    - Named after the field they are trying to test

   - Pros:
     - Setup already exists, works, and it tested 
   - Cons:
     - The file to add/updates/remove tests would become difficult to read and find tests in because of the sheer number
     - Many files would need to be added
2. Unit Tests (Proposal)
    - We can leverage the new diff tool to check if the output is what we expect.

    - Pros:
        - Easier to read, can have a specific test for each field and have it cleanly labeled rather than a list of
files that you would need to scroll through.
        - If we use Strings instead of files, these will all be kept in one place.
        - Can break tests out into one file per transform file for further organization.
    - Cons: 
        - Many files would need to be added or there will be a lot of lengthy strings within the tests.
    






