# Prime CLI

## How to use the CLI

The PRIME command line interface allows you to interact with certain parts of report stream functionality without using the API or running all of ReportStream. A common use case for the CLI is testing while developing mappers for the new FHIR pipeline.

The primary way to access the cli is through the gradle command (although a deprecated bash script exists as well). 
> [!TIP]
> If you are an IntelliJ user, you can set up the gradle command to be run through your IDE and be run in debug mode to step through your code line by line.

```bash
cd ./prime-router
# Prints out all the available commands
./gradlew primeCLI
#  data                      process data
#  list                      list known schemas, senders, and receivers
#  livd-table-download       This updates the LIVD lookup table with a new version.
#  generate-docs             generate documentation for schemas
#  create-credential         create credential JSON or persist to store
#  compare                   compares two CSV files so you can view the
#                            differences within them
#  test                      Run tests of the Router functions
#  login                     Login to the ReportStream authorization service
#  logout                    Logout of the ReportStream authorization service
#  organization              Fetch and update settings for an organization
#  sender                    Fetch and update settings for a sender
#  receiver                  Fetch and update settings for a receiver
#  multiple-settings         Fetch and update multiple settings
#  lookuptables              Manage lookup tables
#  convert-file
#  sender-files              For a specified report, trace each item's ancestry
#                            and retrieve the source files submitted by
#                            senders.
#  fhirdata                  Process data into/from FHIR
#  fhirpath                  Input FHIR paths to be resolved using the input
#                            FHIR bundle
#  convert-valuesets-to-csv  This is a development tool that converts
#                            sender-automation.valuesets to two CSV files

# Converts HL7 to FHIR (IN DEV MODE)
./gradlew primeCLI --args='fhirdata --input-file "src/testIntegration/resources/datatests/HL7_to_FHIR/sample_co_1_20220518-0001.hl7"'

# Converts the FHIR file to HL7 using the provided schema (IN DEV MODE)
./gradlew primeCLI --args='fhirdata --input-file "src/testIntegration/resources/datatests/HL7_to_FHIR/sample_co_1_20220518-0001.fhir" -s metadata/hl7_mapping/ORU_R01/ORU_R01-base.yml'
```

### Command Documentation:

* [fhirpath](./fhir-path.md)