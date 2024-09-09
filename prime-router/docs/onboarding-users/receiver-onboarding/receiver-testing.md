# How to test receiver transforms,settings and filters

Anytime a receiver transform, setting or filter is created or changed, it needs to be appropriately tested to ensure 
that the change behaves as expected and no unintended changes are introduced. Since the transforms, settings and filters
all work together to create the final message sent to a receiver, integration testing where a report is submitted to the
ReportStream application and the final product as it would be sent to the receiver is compared to expected should be
performed on any change.

## Testing 

### Testing Transforms
Basic testing on transforms can be performed using the fhirdata CLI command locally:

Example:
```
./prime fhirdata -i "@/c/user/some-file-location.fhir" -o "@/c/user/some-file-location.fhir" output-file.fhir -s "classpath:/metadata/fhir_transforms/senders/SimpleReport/simple-report-sender-transform.yml" --output-format FHIR

Usage: prime fhirdata [<options>]

  Process data into/from FHIR

Options:
  -i, --input-file=<path>       Input file to process
  -o, --output-file=<path>      output file
  --output-format=(HL7|FHIR)    output format
  --enrichment-schemas=<text>   comma separated enrichment schema name(s) from
                                current directory
  --diff-hl7-output=<text>      when true, diff the the input HL7 with the
                                output, can only be used going HL7 -> FHIR ->
                                HL7
  -r, --receiver-schema=<text>  Receiver schema location. Required for HL7
                                output.
  -s, --sender-schema=<text>    Sender schema location
  --input-schema=<text>         Mapping schema for input file
  -h, --help                    Show this message and exit

```
The CLI command will also display any errors or warnings that the transform is generating. Example: 
"{"message":"Element sr-patient-second-given-name is updating a bundle property, but did not specify a value","thread":"main","timestamp":"2024-09-09T21:21:35.665Z","level":"WARN","logger":"gov.cdc.prime.router.fhirengine.translation.hl7.FhirTransformer"}"

### Testing Settings
Certain receiver settings affect the message output. In the Universal Pipeline these settings are:

- customerStatus
  - Type: Custom
  - Description: Status of receiver. If a receiver is set to "inactive" ReportStream will not route any messages to that receiver.
  - Options:
    - active
    - inactive
    - testing
- schemaName
  - Type: String
  - Description: the path to the schema/transform used for the receiver
- enrichmentSchemasNames
  - Type: List of strings
  - Description: the paths to schema(s) used to enrich the bundle before translating it to its final format
- timeZone
  - Type: USTimeZone
  - Description:  The timezone for the receiver. This is different from the timezone in Timing, which controls the calculation of when and how often to send reports to the receiver.The timeZone for the receiver is the timezone they operate under and can convert date times in their data to if convertDateTimestoReceiverLocalTimes is set to "true"
  - Options:
    - PACIFIC("US/Pacific")
    - MOUNTAIN("US/Mountain")
    - ARIZONA("US/Arizona")
    - CENTRAL("US/Central")
    - EASTERN("US/Eastern")
    - SAMOA("US/Samoa")
    - HAWAII("US/Hawaii")
    - EAST_INDIANA("US/East-Indiana")
    - INDIANA_STARKE("US/Indiana-Starke")
    - MICHIGAN("US/Michigan")
    - CHAMORRO("Pacific/Guam")
    - ALASKA("US/Alaska")
    - UTC("UTC")
- dateTimeFormat
  - Type: DateTimeFormat
  - Description: the format to use for date and datetime values, either Offset or Local
- truncateHl7Fields
  - Type: List of strings
  - Description: List of HL7 fields to truncate to HL7 v2.5.1 specified max length.
- convertTimestampToDateTime
  - Type: Boolean
  - Description: converts all timestamps in messages to datetime format: YYYYMMDD
- convertDateTimesToReceiverLocalTime
  - Type: Boolean
  - Description: if set to "true" will convert all datetimes in messges to the timezone specified in timeZone setting
- useHighPrecisionHeaderDateTimeFormat
  - Type: Boolean
  - Description: sets batch and file header timestamps to format "YYYYMMDDHHMMSS.SSSS+/-ZZZZ"
- Type:
  - Type: Custom
  - Description: Defines what format the output message should be
  - Options:
    - HL7
    - FHIR

There is another list of settings found in [migrating-receiver.md](docs/migrating-users/migrating-receivers.md) section 4 that modify messages on the *Covid Pipeline* only. 
The functionality of these settings on the Universal Pipeline has been moved to receiver transforms.

In order to test changing settings it is recommended that you create two copies of the receiver that you wish to change locally, one with the current settings and one without and compare the output.

### Testing Filters
Filters will determine which messages get routed to receivers so it is important they are appropriately set up. Every filter in the Universal pipeline is made up of a list of fhirpath expressions that must evaluate to a boolean expression.
In order to check if your filter evaluates to a Boolean you can use the ./prime fhirdata CLI command

example:

```
./prime fhirpath -i "@/c/user/some-file-location.fhir"

Usage: prime fhirpath [<options>]

  Input FHIR paths to be resolved using the input FHIR bundle

Options:
  -i, --input-file=<path>  Input file to process
  -c, --constants=<value>  a constant in the form of key=value to be used in
                           FHIR Path. Option can be repeated.
  -h, --help               Show this message and exit
```
This command will provide a prompt to input a fhirpath expression:
```
$ ./prime fhirpath -i "@/c/user/some-file-location.fhir"
Using constants:
        rsext='https://reportstream.cdc.gov/fhir/StructureDefinition/'

Using the FHIR bundle in C:\Users\James.Gilmore\Desktop\test.fhir.txt...
Special commands:
        !![FHIR path]                     - appends specified FHIR path to the end of the last path
        quit, exit                       - exit the tool
        reset                            - Sets %resource to Bundle
        resource [=|:] [']<FHIR Path>['] - Sets %resource to a given FHIR path

%resource = Bundle
Last path =
FHIR path>
```
Inputting a fhirpath expression into the "FHIR path>" prompt location will return the type of the expression and return any fhir resources in the input file that meet the path criteria:
```
 ./prime fhirpath -i "C:\Users\James.Gilmore\Desktop\test.fhir.txt"
Using constants:
        rsext='https://reportstream.cdc.gov/fhir/StructureDefinition/'

Using the FHIR bundle in C:\Users\James.Gilmore\Desktop\test.fhir.txt...
Special commands:
        !![FHIR path]                     - appends specified FHIR path to the end of the last path
        quit, exit                       - exit the tool
        reset                            - Sets %resource to Bundle
        resource [=|:] [']<FHIR Path>['] - Sets %resource to a given FHIR path

%resource = Bundle
Last path = "(Bundle.entry.resource.ofType(MessageHeader).meta.tag.where(system = 'http://terminology.hl7.org/CodeSystem/v2-0103').code = 'P')"
FHIR path> (Bundle.entry.ofType(Observation).code.coding.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code').value.where(code in ('840539006')).exists())
Primitive: BooleanType[false]
Number of results = 1 ----------------------------
```
Ensure that any filters used have a BooleanType or the results of the filter will be unexpected. All filters used in the Universal Pipeline are lists of fhirpath expressions that will be evaluated together
with the exception of the jurisdictional filter which is evaluated prior to any of the other filters.

- jurisdictionalFilter
  - list of fhirpath expressions which determines the messages that qualify for a given jurisdiction. If no jurisdictional filter is provided for a receiver, no messages will qualify for that receiver
  - example: "(Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state = 'LA') or (Bundle.entry.resource.ofType(Patient).address.state = 'LA')"
- qualityFilter
  - list of fhirpath expressions which determines if a given messages is of sufficient quality to route to a receiver. This is similar to the concept of message validation and will eventually likely be superceded
  by a validation solution. These fhirpath expressions should all relate to the suitability of the message for the receivers needs. i.e. if a message doesn't have a patient date of birth it shouldn't be sent to a receiver.
  - example: "Bundle.entry.resource.ofType(Patient).birthDate.exists()"
- routingFilter
  - Similar in concept to a jurisdictional filter but on a more granular level. These expressions should all relate to some category or feature or the message that is not related to the message quality. i.e. a receiver 
  only wants final and corrected results not prelim results
  - example: "Bundle.entry.resource.ofType(DiagnosticReport).where(status in 'final'|'corrected').exists()
- processingModeFilter
  - List of fhirpath expressions that related to the processing mode code of the message. Can be used to separate test and production messages if needed
  - example: "(Bundle.entry.resource.ofType(MessageHeader).meta.tag.where(system = 'http://terminology.hl7.org/CodeSystem/v2-0103').code = 'P')"
- conditionFilter
  - List of fhirpath expressions which relate to which reportable conditions/tests a receiver will accept. Often uses a condition-code from the observation-mapping table in the db but does not have two. 
There are two ways to use the conditionFilter. If the fhirpath expression starts with "%resource" the conditionFilter will "prune" any observations that evaluate to "false" for the expression. If the resource is explicitly stated like "Bundle.entry.resource.ofType(Observation)"
the filter will reject the entire message/Bundle if it evaluates to "false".
  - example: "(Bundle.entry.ofType(Observation).code.coding.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code').value.where(code in ('840539006')).exists())"
  - example with pruning: "%resource.code.coding.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code').value.where(code in ('840539006')).exists()"
  The below filters are either used only in the covid pipeline or have no effective purpose in the Universal pipeline
- reverseTheQualityFilter (*do not use*)
- mappedConditionFilter (*do not use*)

### Integration Testing


## Test plans and testing with receivers

When a new receiver or a significant change is made to an existing receiver, it is important to thoroughly test with the
intended recipient. As part of this testing a test plan should be created to document the test cases performed and ensure
that all reasonably anticipated edge cases have been tested.
