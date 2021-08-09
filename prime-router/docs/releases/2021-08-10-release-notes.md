#  ReportStream Release Notes

*August 10, 2021*

## General useful links:

- All Schemas are documented here:  [Link to detailed schema dictionaries](../schema_documentation)
- The Hub API is documented here: [Hub OpenApi Spec](../openapi.yml)
- [Click here for all release notes](../releases)

## For this release
Added scanning for secrets into our CI/CD pipeline via gitleaks.

HL7 serializer improvements including verification of CODE fields on HL7 ingest.

Documentation enhancements.

Improvements for generation of HL7 files for AK, NH, and CA.

Converted unit tests to use assertK.


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
