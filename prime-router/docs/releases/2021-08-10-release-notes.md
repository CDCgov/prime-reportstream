#  ReportStream Release Notes

*August 10, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release

### Improved Security
Added scanning for secrets into our CI/CD pipeline via gitleaks.

### Enhanced Documentation
Documentation enhancements.

### HL7 serialization improvements
Improvements for generation of HL7 files for AK, NH, and CA.

HL7 serializer improvements including verification of CODE fields on HL7 ingest.

### Improved Unit Tests
Converted unit tests to use assertK.

### New `correctedTestId` column
A new column has been added to allow for test results to amend/update previous results.   This feature has been added for those who were first working with Waters, and are now sending CSV data directly to ReportStream.  It works like this:

First, for all rows of data you submit to ReportStream, you’ll need to make sure that every row of data you send is tagged with completely new/unique/never-previously-used ID.  You can use the `specimenId` column for this purpose, or if your `specimenId` value is re-used on an update, then you’ll need to add a new column called `testId` to your data, that meets this uniqueness rule.

Then, also add a new column called `correctedTestId`.  This column will almost always be blank, except when you are submitting a result that amends a previous result.  In those cases, the `correctedTestId` column will be filled in with the older/previous `testId` (or `specimenId`, if yours meets the uniqueness criteria above) row that is getting amended.  In this way, corrections can be “chained” and a history can be maintained, each `correctedTestId` pointing to the `testId` of the most recent previous version.

### Name updated in SFT segment
We recently updated our project's name, and we've updated it in the `SFT` segment of the HL7 we generate now too to reflect it.

### Dependency Updates
The following dependencies were updated:
- eslint
- flyway
- libphonenumber
- azurefunctions
- sendgrid-java
- fontawesome
- netty
- azure-core
- azure-identity
