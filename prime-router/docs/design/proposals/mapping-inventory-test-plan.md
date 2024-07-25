# Mapping Inventory Test Plan

## Problem Statement
As a ReportStream engineer, I want tests that will validate the following statement: "The Universal Pipeline (UP) shall 
be able to transform valid NIST 2.5.1 messages losslessly to FHIR using the official HL7v2 mapping inventory as a guide. 
UP shall be able to transform FHIR ELR messages to valid NIST 2.5.1."

## Solution
Currently, we have integration tests that test an unknown combination of parts of sender transforms, receiver 
transforms, and mapping inventory rules. We want a clear and definitive way of knowing what is already tested, and we 
want the mapping inventory testing to be thorough. This means testing not only that each piece of the message ends up 
in the Primary Target (as defined in column J of the mapping inventory spreadsheet), but also that the conditions set
forth in the Computable ANTLR column of the mapping inventory spreadsheet are tested. This will just test moving from 
HL7 V2 -> FHIR. We also need to test that the resulting FHIR message can be converted back to HL7 V2 losslessly. To 
achieve this, we can reverse the order of the files and ensure that the output from processing the FHIR file matches 
the HL7 V2 file. This will help keep the number of files needed down. 

### File naming
Integration tests are currently rather challenging to read. At a minimum, we need a standard for naming the files. When
testing mapping inventory segments, we will name files with the format `segment_spreadsheetRow#_testDesc`. An example, 
`PID_1_setting_condition`. The corresponding FHIR file will be named the same, but will have a `.fhir` extension. 
This way, it is very apparent not only what the file is testing, but which file goes with it. To make sure that these 
don't get out of sync, we will keep copies of the spreadsheets in our project which we will periodically, manually sync 
to the actual spreadsheets. During the sync, we will also write tickets for any new or changed rules/fields that need 
to be tested.

### Datatypes 
There are datatypes that are within the primary segments, for instance `XPN`. We don't want to test these repeatedly 
for each segment they appear in, so we will have separate files for each of them
labeled like so `XPN_spreadsheetRow#_testDesc`. 

### File Structure
Instead of piling these files into the existing file structure,
especially since we will be using them for multidirectional tests, we will put them in a folder structure 
`resources/datatests/mappinginventory/segment-name`.

### Segments
Within the files, we will only put the bare minimum number of segments in a file. `MSH` is still required, so, for 
instance, when testing `PID` you would only have an `MSH` segment and a `PID` segment in the file. To allow this, when 
converting from the HL7 V2 file to the FHIR file, use the new test schema 
`metadata/hl7_mapping/testing/ORU_R01-test.yml`

### Where to test
`translation-test-config.csv` is where our current tests like this live. We will create separate files for each data 
type that will follow the pattern laid forth in XTNTests. These will be stored in
`kotlin/datatests/mappinginventory/segment-name`. Using this pattern relieves us of the requirement of using CSV files and 
allows us to give each test a name, description, and comment. Later down the road, we will also have separate files for
sender transforms and receiver transforms. These will be broken down by sender/receiver so that, again, there is a 
lot of organization around the tests.

### Testing Strategies
Updating mapping tests for a large amount of files manually is tedious and inefficient. There is a bash scrip which can
be used to update all the `.fhir` files for an entire directory at once: `prime-reportstream/recreate-fhir-in-dir.sh`. 
Once new mappings have been added, this file can be run on an existing `.h7` test input file and will generate the 
resulting `.fhir` output. 

## Future Work
The other important thing that can happen either before or after the creation of the tests is cleaning up the output.
It is currently challenging to figure out what does not match when the tests break. We want to change the tests to use 
the HL7 V2 diff tool which will specify exactly what did not match. This can be completed before, after, or in parallel
of the actual tests being created. 

## Further Considerations
- We need to be careful and ensure that the various message types link to the same spreadsheet per segment and, if they 
do not, have separate tests per message type, per segment. 