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
  -i, --input-file=<path>        Input file to process
  -o, --output-file=<path>       output file
  --output-format=(HL7|FHIR)     output format
  --enrichment-schemas=<text>    comma separated enrichment schema name(s) from
                                 current directory
  --diff-hl7-output=<text>       when true, diff the the input HL7 with the
                                 output, can only be used going HL7 -> FHIR ->
                                 HL7
  -r, --receiver-schema=<text>   Receiver schema location. Required for HL7
                                 output.
  --receiver-name=<text>         Name of the receiver settings to use
  --org=<text>                   Name of the org settings to use
  --receiver-setting-env=<text>  Environment that specifies where to get the
                                 receiver settings
  -s, --sender-schema=<text>     Sender schema location
  --input-schema=<text>          Mapping schema for input file
  -h, --help                     Show this message and exit
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

When making changes to receiver settings or transforms it is important to update the integration tests. Integration tests
can be defined to use sender and receiver settings. Keep in mind a single message may not be sufficient to test all settings/transform elements
and multiple messages may need to be used for each transform/receiver. See testing plans and testing with receivers below for more information

## Test plans and testing with receivers

When a new receiver or a significant change is made to an existing receiver, it is important to thoroughly test with the
intended recipient. As part of this testing a test plan should be created to document the test cases performed and ensure
that all reasonably anticipated edge cases have been tested.

### Testing Plans
A good testing plan needs to test every single receiver setting, filter and transform element to ensure that no unexpected data will make it to the receiver. The type of transform being performed will
determine how many messages you need to test to be sure that all transform cases are being covered. The following example can help identify how many and what type of messages you will need

Example transform:

```yaml
hl7Class: ca.uhn.hl7v2.model.v251.message.ORU_R01

extends: classpath:/metadata/hl7_mapping/ORU_R01/ORU_R01-base.yml

elements:

- name: test-receiving-application
  condition: 'true'
  value: [ '"TEST-DOH"' ]
  hl7Spec: [ 'MSH-5-1' ]

- name: test-patient-race-coding-system
  resource: 'Bundle.entry.resource.ofType(Patient).extension("http://ibm.com/fhir/cdm/StructureDefinition/local-race-cd").value.coding'
  condition: '%resource.code.exists()'
  value: [ '%resource.system.getCodingSystemMapping()' ]
  hl7Spec: [ '/PATIENT_RESULT/PATIENT/PID-10-3' ]

- name: test-patient-county-codes
  condition: 'Bundle.entry.resource.ofType(Patient).address.district.empty().not() and Bundle.entry.resource.ofType(Patient).address.state.empty().not()'
  hl7Spec: [ '/PATIENT_RESULT/PATIENT/PID-11-9' ]
  value: ["FIPSCountyLookup(Bundle.entry.resource.ofType(Patient).address.district,Bundle.entry.resource.ofType(Patient).address.state)[0]"]

- name: test-patient-ethnicity-identifier-code
  value:
    - 'Bundle.entry.resource.ofType(Patient).extension(%`rsext-ethnic-group`).value.coding[0].code'
      hl7Spec: [ '/PATIENT_RESULT/PATIENT/PID-22-1' ]
      valueSet:
      values:
      H: 2135-2
      N: 2186-5

# Needed to convert HL7 timestamp to HL7 date for OBX-5
- name: obx-value-dtm-dt
  condition: '%context.extension(%`rsext-obx-observation`).extension.where(url = "OBX.2").value = "DT"'
  value: [ '%resource.value.extension(%`rsext-hl7v2-date-time`).value.toString().replace("-","")' ]
  hl7Spec: [ '%{hl7OBXField}-5' ]

- name: test-specimen-source-site-text
  condition: 'true'
  value: [ 'Bundle.entry.resource.ofType(Specimen).collection.bodySite.text' ]
  hl7Spec: [ '/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/SPM-8-2' ]

# TEST DOH does not want AOEs at all, so this is overridden to prevent identified AOEs from mapping to an HL7 segment
- name: observation-result-with-aoe
  resource: '%resource.result.resolve()'
```
In the example above you can see we have several different types of transforms. Some of them like element "test-receiving-application" are just defaulting a value and all we
have to do is make sure that value exists on every message we test. Others like "test-patient-ethnicity-identifier-code" are using a valueset to transform one value into another and elements like
"obx-value-dtm-dt" have a complex condition and are modifying the format of a field. For these kinds of elements we need to make sure and test both the positive and negative case. i.e. for "test-patient-ethnicity-identifier-code" we need to test a message with "H" or "N" in "Bundle.entry.resource.ofType(Patient).extension(%`rsext-ethnic-group`).value.coding[0].code"
and then also test when "Bundle.entry.resource.ofType(Patient).extension(%`rsext-ethnic-group`).value.coding[0].code" is neither of those values. For "obx-value-dtm-dt" we need to test where the condition is both "true" and "false"

You can see how some of these test cases can be combined. For example, we can have a single message with both a "Bundle.entry.resource.ofType(Patient).extension(%`rsext-ethnic-group`).value.coding[0].code" value of "N" and that
meets the condition for "obx-value-dtm-dt".

One of the most complex items we commonly test are the condition filters. For example take a conditionFilter such as:

```
"%resource.code.coding.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code').value.where(code  in ('840539006'|'55735004'|'6142004')).exists() and %resource.interpretation.coding.code = 'A'"
```

We have three possible condition codes that also need to have a positive result to qualify. In addition, we are pruning observations that do not meet that criteria.
you can see how in order to test both the positive and negative cases we will need several test messages. Also, because there is the possibility that we may receive a message
with multiple observations that meet the criteria we also want to test that case.

It is also important to keep in mind that we have different sources of data. SimpleReport Manual entry, SimpleReport CSV entry and direct HL7 to ReportStream. We should make sure to test
examples of all sources the receiver will get production data from. An example test plan can be found here [Example Test Plan](prime-router/docs/onboarding-users/receiver-onboarding/example-test-cases.xlsx).

### Simple Report Test data

A quick and easy way to get test data to send to a STLT is by going into SimpleReport's test environment https://test.simplereport.gov.
* Access can be requested on the [shared-simple-report-universal-pipeline](https://nava.slack.com/archives/C0411VC78DN) thread.
* Instructions on how to send a test message can be found on this youtube playlist https://www.youtube.com/playlist?list=PL3U3nqqPGhab0sys3ombZmwOplRYlBOBF.
* The file [SR upload](../onboarding-users/samples/SimpleReport/SR-UPLOAD.csv) can be used test sending reports through SimpleReport's CSV upload.
* To route the report to a specific STLT either the patient or facility state needs to updated to the STLT's jurisdiction. Keep in mind that if they are not updated the message might get routed to the incorrect STLT.
* The report sent by SimpleReport can be found in the Azure BlobStorage. The UP message will be stored in the `receive/simple_report.fullelr` and the covid pipeline message will be stored in `receive/simple_report.default`. This message can be used locally to test any new sender or receiver transforms.
* To access the blob storage. Microsoft Storage Explorer needs to be installed and login with your CDC SU credentials.

### Testing with receivers
Ultimately the receiver will be the judge of whether sufficient testing has been completed or not, receivers tend to not
assume that all data will look the same from each sender since they do not know what exact transforms and settings are being applied.
Receivers also have a tendency to overlook certain items which do not cause errors in their application but can cause problems in other 
ways like incorrect timezones. If is important that you ask your receiver to check for specific items in the test messages to ensure they
are coming across appropriately. These items may include:

- date/timestamps
- race/ethnicity
- patient demographic information (county code)
- specimen source/type
- Abnormal flags
- Order status flags
- Any complex custom logic done for the receiver (i.e. turning observations into notes).

