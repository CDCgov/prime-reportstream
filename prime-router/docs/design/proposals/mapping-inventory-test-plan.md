# Mapping Inventory Test Plan

## Problem Statement
As a ReportStream engineer, I want tests that will validate the following statement: "The Universal Pipeline (UP) shall be
able to transform valid NIST 2.5.1 messages losslessly to FHIR using the official HL7v2 mapping inventory as a guide. 
UP shall be able to transform FHIR ELR messages to valid NIST 2.5.1."

Currently, we have integration tests that test an unknown combination of parts of sender transforms, receiver 
transforms, and mapping inventory rules. We want a clear and definitive way of knowing what is already tested, and we 
want the mapping inventory testing to be thorough. This means testing not only that each piece of the message ends up 
in the Primary Target (as defined in column J of the mapping inventory spreadsheet), but also that the conditions set
forth in the Computable ANTLR column of the mapping inventory spreedsheet are tested. This will just test moving from 
HL7 V2 -> FHIR. We also need to test that the resulting FHIR message can be converted back to HL7 V2 losslessly. To 
achieve this, we can reverse the order of the files and ensure that the output from processing the FHIR file matches 
the HL7 V2 file. This will help keep the number of files needed down. 

Integration tests are currently rather challenging to read. At a minimum, we need a standard for naming the files. When
testing mapping inventory segments, we will name files with the format `segment_#`. An example, 
`PID_1`. The corresponding FHIR file will be named the same, but will have a `.fhir` extension. This way, it is 
very apparent not only what the file is testing, but which file goes with it. If it gets down to where there may only 
be one complicated Computable ANTLR rule being tested, the field should be specified as well since there is only one thing being tested at that point. ex. `PID_patient_name_5`. 

Instead of piling these files into the existing file structure,
especially since we will be using them for multidirectional tests, we will put them in a folder structure 
`datatests/mapping-inventory`. So far, the spreadsheets are shared between message types. If that ever changes, we will
add a folder within `datatests/mapping-inventory` labeled as the message type and will add the files with the same 
naming convention there. 

Within the files, we will only put the bare minimum number of segments in a file. `MSH` is required, so, for 
instance, when testing `PID` you would only have an `MSH` segment and a `PID` segment in the file. 

`translation-test-config.csv` is where our current tests like this live. We will create a separate file called 
`mapping-inventory-test-config.csv` since this file is already bloated, difficult to read, and lacks organization. 
Later down the road, we will also have separate files for sender transforms and receiver transforms. These will be 
broken down by sender/receiver so that, again, there is a lot of organization around the tests.

The other important thing that can happen either before or after the creation of the tests is cleaning up the output.
It is currently challenging to figure out what does not match when the tests break. We want to change the tests to use 
the HL7 V2 diff tool which will specify exactly what did not match. This can be completed before, after, or in parallel
of the actual tests being created. 

## Further Considerations
- We need to be careful and ensure that the various message types link to the same spreadsheet per segment and, if they 
do not, have separate tests per message type, per segment. 