# Updating Observation-Mapping table

## Context

Individual FHIR observations are "stamped" with a SNOMED value that represents the reportable condition indicated by the test represented by a LOINC or SNOMED code
on that observation. In addition, observations that do not represent a reportable condition can be 
stamped with a value that assists in identifying their administrative purpose such as Ask-at-Order-Entry questions (AOEs).

The observation mapping table is used to map LOINC/SNOMED codes to their relevant reportable conditions. The initial 
table is made up of values that are contained in valuesets maintained by the Council of State and Territorial 
Epidemiologists (CSTE) on the National Library of Medicines' [Value Set Authority Center (VSAC)]
(https://vsac.nlm.nih.gov/). The valuesets are compiled and mapped to conditions by CSTE and published in the Reportable
Conditions Trigger Codes (RCTC). The RCTC is available to be downloaded from AIMS at 
https://ersd.aimsplatform.org/#/home. A login is required (registration is free).

The Observation Mapping table is made up of CSTE ValueSets and contains the following columns:

| Column Name                   | Description                                   | Example                                                                        |
|-------------------------------|-----------------------------------------------|--------------------------------------------------------------------------------|
| Member OID                    | ValueSet Identifier                           | 2.16.840.1.113762.1.4.1146.239                                                 |
| Name                          | ValueSet Name                                 | Chlamydia trachomatis Infection (Tests for Chlamydia trachomatis Nucleic Acid) |
| Code                          | LOINC or SNOMED coded value                   | 16601-7                                                                        |
| Descriptor                    | LOINC or SNOMED term descriptio               | Chlamydia trachomatis rRNA [Presence] in Urine by Probe                        |
| Code System                   | Indicates whether code is LOINC or SNOMED     | LOINC                                                                          |
| Version                       | LOINC or SNOMED release version               | 2.74                                                                           |
| Status                        | Indicates if code is active or deprecated     | Active                                                                         |
| Condition Name                | Name of associated reportable condition       | Chlamydia trachomatis infection (disorder)                                     |
| Condition Code                | SNOMED value associated with condition        | 240589008                                                                      | 
| Condition Code System         | System used for condition code                | SNOMEDCT                                                                       |
| Condition Code System Version | SNOMED version associated with condition code | 2023-03                                                                        |
| Value Source                  | Source of value (e.g. RCTC vs manual mapping) | RCTC                                                                           |   
| Created At                    | Date that table entry updated                 | 20231020                                                                       |

The column names are taken directly from the [RCTC spreadsheet](https://docs.google.com/spreadsheets/d/1rO_p145xXO1AD76vx8vBqXgoQUnLqfc8/edit#gid=739612351) with the exception of "Value Source" and "Created At" which are additional columns added for administrative purposes that will be used when updating the table. Both LOINC and SNOMED codes are combined in this table and can be identified by column "Code System".
The RCTC does a fairly good job of keeping up to date with LOINC and SNOMED codes and is regularly updated. It is anticipated that despite this there will be a requirement to map codes that are not present in the condition mapping table. These will have to be mapped manually after review by RS personnel in order to ensure that the proper condition code is mapped to the LOINC or SNOMED code. These codes can be submitted to CSTE valueset reviewers to be included in future releases. If a column is not applicable it can be left blank unless it is the "Code", "Code System", "Condition Name", "Condition Code"  "Value Source" columns.
It is possible that an observation may not contain information intended to convey the results of a diagnostic test but instead represent additional important information regarding the testing sample of patient. Information of this type is referred to as "Ask at Order Entry" (AOE) questions commonly referred to by the abbreviation AOE only. The specific information being asked is identified in the observation with a LOINC code in OBX-3 (HL7-V2) or Observation.Code.Coding.Code (FHIR). We need to be able to identify these observations as AOEs in the same manner as we would identify which reportable condition the observation represents so that they can be filtered out if requested by STLTs and also to eliminate them as being unmapped observations. The LOINC codes for the base table come from two lists on LOINC.org.
<br>1.) [LOINC terms for SARS-CoV-2 AOE questions](https://loinc.org/sars-cov-2-and-covid-19/)<br>
2.) [Public health laboratory ask at order entry panel](https://loinc.org/81959-9)<br>

## Updating the table 
There are two ways to update the table.

1.) Automatically using the Value Set Authority Center (VSAC) API <br>
2.) Manually by downloading and manipulated the table locally

In the event that an update to the RCTC introduces a new valueset, or we need to map a code that does not exist in the RCTC such as a local code or AOE, we will need to update the table manually for the time being. This is accomplished by downloading the table locally to a CSV
file and then add or remove rows as needed. The Condition Code System for any mappings added ad-hoc (included the AOEs) should be labled as "ReportStream" since they do not come from any standardized ValueSet.

*********Important, ensure that when editing the file locally, entries containing longer codes do not get converted to a different format (If editing in excel, it will attempt to use scientific notation for longer numeric values)*********

All tables should be updated in staging first using any of the below methods and smoke tests run against staging prior to pushing changes to production. In order to run the smoke tests use the ./prime test CLI command. Steps to run this tests against the staging environment can be found in the "Deployment Process" document pinned to the top to #prime-reportstream-deployment channel or in the [running-tests document](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/getting-started/running-tests.md). In order to run any commands against the remote environments, you will first need to run the ./prime login --env <prod-or-staging> CLI command to obtain an access token.

### Updating RCTC valuesets

The RCTC valuesets are updated regularly. These valuesets are stored in the Value Set Authority Center (VSAC) and can be accessed via an [API](https://www.nlm.nih.gov/vsac/support/usingvsac/vsacsvsapiv2.html).
In order to update the table automatically using the VSAC API you will need a UMLS license (free). To obtain a license follow these steps: <br>

1.) Visit the [VSAC Support Center](https://www.nlm.nih.gov/vsac/support/index.html)
2.) Click on "Request a UMLS License" under "How to Use VSAC"
3.) Login with an identity provider
4.) Get apikey from profile to populate parameter -k in commands below
    a.) Navigate to https://uts.nlm.nih.gov/uts/
    b.) Select "My Profile" in top right
    c.) Copy value from "API KEY"

### Updating all RCTC valuesets from the Value Set Authority Center (VSAC)
Use ./prime lookuptables update-mapping CLI command to update all RCTC value sets from the VSAC. Be sure to select yes when prompted to write the table or output a new local csv. In order to run the commands against a remote environment (Prod or Staging) you will first need to run the ./prime login --env <prod-or-staging>  CLI command to obtain a token to access the remote environment.

Example:
```
./prime lookuptables update-mapping -a -n observation-mapping -k YOUR_API_KEY -e prod
```
options:
```
-e, --env <name>          Connect to <name> environment. Choose between [local|test|staging|prod]
-s, --silent              Do not generate diff or ask for confirmation
-a, --activate            Activate the table upon creation
-n, --name TEXT           The name of the table to perform the operation on
-o, --output-file=<path>  Specify a file to save the table's data as CSV  
-d, --OID                 Provide a comma-separated list of OIDs to update         
```

### Update one of more specific valueset(S) from the Value Set Authority Center(VSAC)
To update only specific valuesets in the table include the -d parameter and provide a comma separated list of OIDs. This will add new values from only the specified valuesets. 

Example:
```
./prime lookuptables update-mapping -a -n observation-mapping -k YOUR_API_KEY -d comma_separated_list_of_OIDS -e prod
```
options:
```
-e, --env <name>          Connect to <name> environment. Choose between [local|test|staging|prod]
-s, --silent              Do not generate diff or ask for confirmation
-a, --activate            Activate the table upon creation
-n, --name TEXT           The name of the table to perform the operation on
-o, --output-file=<path>  Specify a file to save the table's data as CSV  
-d, --OID                 Provide a comma-separated list of OIDs to update   
```

### Downloading the active table manually

When the table needs to be updated manually you should first download the active table locally before updating using the ./prime lookuptables CLI command. 

Example
```
./prime lookuptables get -n observation-mapping -v "table version to get" -o "file-to-output-to"
```
options:
```
  -e, --env=<name>          Connect to <name> environment. Choose between
                            [local|test|staging|prod]
  -o, --output-file=<path>  Specify a file to save the table's data as CSV
  -n, --name=<text>         The name of the table to perform the operation on
  -v, --version=<int>       The version of the table to get
  -h, --help                Show this message and exit
```

## Finding the active version of a table

Table versions can be found by looking in the "Lookup Table Version" table in staging or prod Metabase. The active version of a table will have the value "true" in the "Is Active" column of the table.  

### Uploading new table manually
- Sign in or register to an ERSD account: [AIMS](https://ersd.aimsplatform.org/#/home)
- Click on "Download Latest RCTC Spreadsheet & Change Log". This will create a zip file containing one Excel file for the
whole list of codes and one for the codes that have changed in the current version.
- Import the spreadsheet into Google sheets (can also use Excel if you have access). 
  - File > Import > Select RCTC_Release.xlsx file that was previously downloaded.
- We need to source data from the following sheets:
  - Organism_Substance
  - Lab Order Test Name
  - Lab Obs Test Name
  
  Each sheet contains a `Grouping List` and an `Expansion List` table below it. Each row in the `Expansion List` table represents a test or
some resource associated with an OID. Use this OID to map to a condition in the `Grouping List` table.
The resulting row is then mapped to the appropriate columns in the lookup table (See reference table below for specific mappings.)

  | Column in ReportStream Table  | Column in RCTC Grouping List      | Column in RCTC Expansion List  | Notes                                      |
  |:------------------------------|:----------------------------------|:-------------------------------|:-------------------------------------------|
  | Code                          |                                   | Code                           |                                            |
  | Name                          | Name                              |                                |                                            |
  | Status                        |                                   | Status                         |                                            |
  | Version                       |                                   | Version                        |                                            |
  | Created At                    |                                   |                                | Set to current date                        |
  | Descriptor                    |                                   | Descriptor                     |                                            |
  | Member OID                    | OID                               | Member OID                     | These columns are used to join both tables |
  | Code System                   |                                   | Code System                    |                                            |
  | Value Source                  |                                   |                                | Set to "RCTC" or "LOINC" for AOE questions |
  | condition_code                | Condition Code                    |                                |                                            |
  | condition_name                | Condition Name                    |                                |                                            |
  | Condition Code System         | Condition Code System             |                                |                                            |
  | Condition Code System Version | Condition Code System Version     |                                |                                            |


- One way to automate this process is to import the following Apps Script macro and run it in Google Sheets. (can also be converted to VBA macro to run in Excel)
  - Extensions > Import Macro (If "Import Macros" and "Manage Macros" options are disabled, record a Macro and the options will enable.)
    <details>
        <summary>Macro</summary>

    ```Java
    function formatToRSTable() {
        var spreadsheet = SpreadsheetApp.getActive();
        spreadsheet.getRange('A1:G1').activate();
        // Create New Sheet
        spreadsheet.insertSheet(6);
        
        // add Sheet Name cell, ready for user's input in cell B1 which is then used for all formulas
        spreadsheet.getRange('A1').activate();
        spreadsheet.getCurrentCell().setValue('Sheet Name');
        
        // Grouping List Table
        // Grouping List Row Num label
        spreadsheet.getRange('A2').activate();
        spreadsheet.getCurrentCell().setRichTextValue(SpreadsheetApp.newRichTextValue()
        .setText('Grouping List Row Num')
        .setTextStyle(0, 20, SpreadsheetApp.newTextStyle()
        .setFontFamily('Arial')
        .build())
        .build());
        // Find the cell that contains "Grouping List" in first column of sheet - returns row num
        spreadsheet.getRange('B2').activate();
        spreadsheet.getCurrentCell().setFormula('=Match("Grouping List", indirect("\'"&$B$1&"\'!A:A"),0)');
        
        // Expansion List Table
        // Expansion List Row Num label
        spreadsheet.getRange('A3').activate();
        spreadsheet.getCurrentCell().setRichTextValue(SpreadsheetApp.newRichTextValue()
        .setText('Expansion List Row Num')
        .setTextStyle(0, 21, SpreadsheetApp.newTextStyle()
        .setFontFamily('Arial')
        .build())
        .build());
        // Find the cell that contains "Expansion List" in first column of sheet - returns row num
        spreadsheet.getRange('B3').activate();
        spreadsheet.getCurrentCell().setFormula('=Match("Expansion List", indirect("\'"&$B$1&"\'!A:A"),0)');
        
        // OID column in grouping list which is used to map to OIDs in expansion list
        // OID column in grouping list label
        spreadsheet.getRange('A6').activate();
        spreadsheet.getCurrentCell().setRichTextValue(SpreadsheetApp.newRichTextValue()
        .setText('OID column in grouping list')
        .setTextStyle(0, 26, SpreadsheetApp.newTextStyle()
        .setFontFamily('Arial')
        .build())
        .build());
        // Find the "OID" column in the Grouping List Table which starts in row num specified in B2 - returns column number
        spreadsheet.getRange('B6').activate();
        spreadsheet.getCurrentCell().setFormula('=Match("OID", INDIRECT("\'"&$B$1&"\'!"&B2 +1&":"&B2 + 1),0)');
        // OID column name label
        spreadsheet.getRange('A7').activate();
        spreadsheet.getCurrentCell().setRichTextValue(SpreadsheetApp.newRichTextValue()
        .setText('OID column name')
        .setTextStyle(0, 14, SpreadsheetApp.newTextStyle()
        .setFontFamily('Arial')
        .build())
        .build());
        // Column name of "OID" column
        spreadsheet.getRange('B7').activate();
        spreadsheet.getCurrentCell().setFormula('=SUBSTITUTE(ADDRESS(1, B6, 4), "1", "")');
        
        // Rows 4 and 5 correspond to the column headers in row 8. These formulas search the sheet for the columns we need from each of the tables (Grouping and Expansion). 
        // The columns we need are: "Code, Name, Status, Version, Descriptor, Member OID, Code System, Condition Code, Condition Name, Condition Code System, Condition Code System Version"
        // We hard-code these values as the first parameter of the MATCH function and use them to get the column number (1,2,3...) and corresponding column name (A,B,C...) where they are found.
        // For example, the formulas for A4 and A5 below would return 2 and B respectively. This means that the "Code" column is the second column (or column B) in the expansion list table headers (B3). 
        // We need both the column number and column name to construct different formulas in other cells.
        //
        // Find the "Code" column in the Expansion List Table which starts in row num specified in B3 - returns column number
        spreadsheet.getRange('A4').activate();
        spreadsheet.getCurrentCell().setFormula('=Match("Code", INDIRECT("\'"&$B$1&"\'!"&B3 +1&":"&B3 + 1),0)');
        // Column name of "Code" column
        spreadsheet.getRange('A5').activate();
        spreadsheet.getCurrentCell().setFormula('=SUBSTITUTE(ADDRESS(1, A4, 4), "1", "")');
        // Find the "Name" column in the Grouping List Table which starts in row num specified in B2 - returns column number
        spreadsheet.getRange('B4').activate();
        spreadsheet.getCurrentCell().setFormula('=Match("Name", INDIRECT("\'"&$B$1&"\'!"&B2 +1&":"&B2 + 1),0)');
        // Column name of "Name" column
        spreadsheet.getRange('B5').activate();
        spreadsheet.getCurrentCell().setFormula('=SUBSTITUTE(ADDRESS(1, B4, 4), "1", "")');
        // Find the "Status" column in the Expansion List Table which starts in row num specified in B3 - returns column number
        spreadsheet.getRange('C4').activate();
        spreadsheet.getCurrentCell().setFormula('=Match("Status", INDIRECT("\'"&$B$1&"\'!"&B3 +1&":"&B3 + 1),0)');
        // Column name of "Status" column
        spreadsheet.getRange('C5').activate();
        spreadsheet.getCurrentCell().setFormula('=SUBSTITUTE(ADDRESS(1, C4, 4), "1", "")');
        // Find the "Version" column in the Expansion List Table which starts in row num specified in B3 - returns column number
        spreadsheet.getRange('D4').activate();
        spreadsheet.getCurrentCell().setFormula('=Match("Version", INDIRECT("\'"&$B$1&"\'!"&B3 +1&":"&B3 + 1),0)');
        // Column name of "Version" column
        spreadsheet.getRange('D5').activate();
        spreadsheet.getCurrentCell().setFormula('=SUBSTITUTE(ADDRESS(1, D4, 4), "1", "")');
        // Create Date label - no formulas needed for this column
        spreadsheet.getRange('E4').activate();
        spreadsheet.getCurrentCell().setRichTextValue(SpreadsheetApp.newRichTextValue()
        .setText('Create Date')
        .setTextStyle(0, 10, SpreadsheetApp.newTextStyle()
        .setFontFamily('Arial')
        .build())
        .build());
        // Find the "Descriptor" column in the Expansion List Table which starts in row num specified in B3 - returns column number
        spreadsheet.getRange('F4').activate();
        spreadsheet.getCurrentCell().setFormula('=Match("Descriptor", INDIRECT("\'"&$B$1&"\'!"&B3 +1&":"&B3 + 1),0)');
        // Column name of "Descriptor" column
        spreadsheet.getRange('F5').activate();
        spreadsheet.getCurrentCell().setFormula('=SUBSTITUTE(ADDRESS(1, F4, 4), "1", "")');
        // Find the "Member OID" column in the Expansion List Table which starts in row num specified in B3 - returns column number
        spreadsheet.getRange('G4').activate();
        spreadsheet.getCurrentCell().setFormula('=Match("Member OID", INDIRECT("\'"&$B$1&"\'!"&B3 +1&":"&B3 + 1),0)');
        // Column name of "Member OID" column
        spreadsheet.getRange('G5').activate();
        spreadsheet.getCurrentCell().setFormula('=SUBSTITUTE(ADDRESS(1, G4, 4), "1", "")');
        // Find the "Code System" column in the Expansion List Table which starts in row num specified in B3 - returns column number
        spreadsheet.getRange('H4').activate();
        spreadsheet.getCurrentCell().setFormula('=Match("Code System", INDIRECT("\'"&$B$1&"\'!"&B3 +1&":"&B3 + 1),0)');
        // Column name of "Code System" column
        spreadsheet.getRange('H5').activate();
        spreadsheet.getCurrentCell().setFormula('=SUBSTITUTE(ADDRESS(1, H4, 4), "1", "")');
        // RCTC label - no formulas needed for this column
        spreadsheet.getRange('I4').activate();
        spreadsheet.getCurrentCell().setRichTextValue(SpreadsheetApp.newRichTextValue()
        .setText('RCTC')
        .setTextStyle(0, 3, SpreadsheetApp.newTextStyle()
        .setFontFamily('Arial')
        .build())
        .build());
        // Find the "Condition Code" column in the Grouping List Table which starts in row num specified in B2 - returns column number
        spreadsheet.getRange('J4').activate();
        spreadsheet.getCurrentCell().setFormula('=Match("Condition Code", INDIRECT("\'"&$B$1&"\'!"&B2 +1&":"&B2 + 1),0)');
        // Column name of "Condition Code" column
        spreadsheet.getRange('J5').activate();
        spreadsheet.getCurrentCell().setFormula('=SUBSTITUTE(ADDRESS(1, J4, 4), "1", "")');
        // Find the "Condition Name" column in the Grouping List Table which starts in row num specified in B2 - returns column number
        spreadsheet.getRange('K4').activate();
        spreadsheet.getCurrentCell().setFormula('=Match("Condition Name", INDIRECT("\'"&$B$1&"\'!"&B2 +1&":"&B2 + 1),0)');
        // Column name of "Condition Name" column
        spreadsheet.getRange('K5').activate();
        spreadsheet.getCurrentCell().setFormula('=SUBSTITUTE(ADDRESS(1, K4, 4), "1", "")');
        // Find the "Condition Code System" column in the Grouping List Table which starts in row num specified in B2 - returns column number
        spreadsheet.getRange('L4').activate();
        spreadsheet.getCurrentCell().setFormula('=Match("Condition Code System", INDIRECT("\'"&$B$1&"\'!"&B2 +1&":"&B2 + 1),0)');
        // Column name of "Condition Code System" column
        spreadsheet.getRange('L5').activate();
        spreadsheet.getCurrentCell().setFormula('=SUBSTITUTE(ADDRESS(1, L4, 4), "1", "")');
        // Find the "Condition Code System Version" column in the Grouping List Table which starts in row num specified in B2 - returns column number
        spreadsheet.getRange('M4').activate();
        spreadsheet.getCurrentCell().setFormula('=Match("Condition Code System Version", INDIRECT("\'"&$B$1&"\'!"&B2 +1&":"&B2 + 1),0)');
        // Column name of "Condition Code System Version" column
        spreadsheet.getRange('M5').activate();
        spreadsheet.getCurrentCell().setFormula('=SUBSTITUTE(ADDRESS(1, M4, 4), "1", "")');
        
        
        // Row 8 : Set current ReportStream observation-mapping table headers
        spreadsheet.getRange('A8').activate();
        spreadsheet.getCurrentCell().setFormula('=split("Code,Name,Status,Version,Created At,Descriptor,Member OID,Code System,Value Source,condition_code,condition_name,Condition Code System,Condition Code System Version",",")');
        
        // The following formulas use the Expansion List
        // Grab all values in the OID column in the expansion list
        spreadsheet.getRange('A9').activate();
        spreadsheet.getCurrentCell().setFormula('=INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$3+2,A4)&":"&A5)');
        // Grab all values of "Status" column in Expansion List - column num specified in C4
        spreadsheet.getRange('C9').activate();
        spreadsheet.getCurrentCell().setFormula('=INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$3+2,C4)&":"&(SUBSTITUTE(ADDRESS(1, C4, 4), "1", "")))');
        // Grab all values of "Version" column in Expansion List - column num specified in D4
        spreadsheet.getRange('D9').activate();
        spreadsheet.getCurrentCell().setFormula('=INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$3+2,D4)&":"&(SUBSTITUTE(ADDRESS(1, D4, 4), "1", "")))');
        // Grab all values of "Descriptor" column in Expansion List - column num specified in F4
        spreadsheet.getRange('F9').activate();
        spreadsheet.getCurrentCell().setFormula('=INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$3+2,F4)&":"&(SUBSTITUTE(ADDRESS(1, F4, 4), "1", "")))');
        // Grab all values of "Member OID" column in Expansion List - column num specified in G4 - used for MATCH parameter to map to Grouping List
        spreadsheet.getRange('G9').activate();
        spreadsheet.getCurrentCell().setFormula('=INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$3+2,G4)&":"&(SUBSTITUTE(ADDRESS(1, G4, 4), "1", "")))');
        // Grab all values of "Code System" column in Expansion List - column num specified in H4
        spreadsheet.getRange('H9').activate();
        spreadsheet.getCurrentCell().setFormula('=INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$3+2,H4)&":"&(SUBSTITUTE(ADDRESS(1, H4, 4), "1", "")))');
        // Hard-code RCTC value
        spreadsheet.getRange('I9').activate();
        spreadsheet.getCurrentCell().setRichTextValue(SpreadsheetApp.newRichTextValue()
        .setText('RCTC')
        .setTextStyle(0, 4, SpreadsheetApp.newTextStyle()
        .setFontFamily('Arial')
        .build())
        .build());
        
        // The following formulas use the OID in the current row to match to an OID in the Grouping List
        // Find row with the same OID in Grouping List table and grab "Name" value
        spreadsheet.getRange('B9').activate();
        spreadsheet.getCurrentCell().setFormula('=Index(INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$2+2,1)&":"&$B$3),MATCH($G9,INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$2+2,$B$6)&":"&$B$7&$B$3),0),B$4)');
        // Find row with the same OID in Grouping List table and grab "Condition Code" value
        spreadsheet.getRange('J9').activate();
        spreadsheet.getCurrentCell().setFormula('=Index(INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$2+2,1)&":"&$B$3),MATCH($G9,INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$2+2,$B$6)&":"&$B$7&$B$3),0),J$4)');
        // Find row with the same OID in Grouping List table and grab "Condition Name" value
        spreadsheet.getRange('K9').activate();
        spreadsheet.getCurrentCell().setFormula('=Index(INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$2+2,1)&":"&$B$3),MATCH($G9,INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$2+2,$B$6)&":"&$B$7&$B$3),0),K$4)');
        // Find row with the same OID in Grouping List table and grab "Condition Code System" value
        spreadsheet.getRange('L9').activate();
        spreadsheet.getCurrentCell().setFormula('=Index(INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$2+2,1)&":"&$B$3),MATCH($G9,INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$2+2,$B$6)&":"&$B$7&$B$3),0),L$4)');
        // Find row with the same OID in Grouping List table and grab "Condition Code System Version" value
        spreadsheet.getRange('M9').activate();
        spreadsheet.getCurrentCell().setFormula('=Index(INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$2+2,1)&":"&$B$3),MATCH($G9,INDIRECT("\'"&$B$1&"\'!"&ADDRESS($B$2+2,$B$6)&":"&$B$7&$B$3),0),M$4)');
        spreadsheet.getRange('B2').activate();
        
        // Expand functions to all rows
        spreadsheet.getRange('B9').activate();
        spreadsheet.getActiveRange().autoFillToNeighbor(SpreadsheetApp.AutoFillSeries.DEFAULT_SERIES);
        spreadsheet.getRange('I9').activate();
        spreadsheet.getActiveRange().autoFillToNeighbor(SpreadsheetApp.AutoFillSeries.DEFAULT_SERIES);
        spreadsheet.getRange('J9').activate();
        spreadsheet.getActiveRange().autoFillToNeighbor(SpreadsheetApp.AutoFillSeries.DEFAULT_SERIES);
        spreadsheet.getRange('K9').activate();
        spreadsheet.getActiveRange().autoFillToNeighbor(SpreadsheetApp.AutoFillSeries.DEFAULT_SERIES);
        spreadsheet.getRange('L9').activate();
        spreadsheet.getActiveRange().autoFillToNeighbor(SpreadsheetApp.AutoFillSeries.DEFAULT_SERIES);
        spreadsheet.getRange('M9').activate();
        spreadsheet.getActiveRange().autoFillToNeighbor(SpreadsheetApp.AutoFillSeries.DEFAULT_SERIES);
        
        // formatting: (set background color for observation-mapping table headers and auto-resize columns)
        spreadsheet.getRange('A8:M8').activate();
        spreadsheet.getActiveRangeList().setBackground('#cccccc');
        spreadsheet.getRange('A9').activate();
        spreadsheet.getActiveSheet().autoResizeColumns(1, 1);
        spreadsheet.getActiveSheet().autoResizeColumns(2, 1);
        spreadsheet.getActiveSheet().autoResizeColumns(6, 1);
        spreadsheet.getActiveSheet().autoResizeColumns(7, 1);
        spreadsheet.getActiveSheet().autoResizeColumns(11, 1);
        spreadsheet.getActiveSheet().autoResizeColumns(13, 1);
        spreadsheet.getRange('8:8').activate();
        spreadsheet.getActiveSheet().setFrozenRows(8);
        // set B1 background to yellow to indicate as input field
        spreadsheet.getRange('B1').activate();
        spreadsheet.getActiveRangeList().setBackground('#fff2cc');
        spreadsheet.getRange('F1').activate();
        spreadsheet.getCurrentCell().setRichTextValue(SpreadsheetApp.newRichTextValue()
        .setText('Note: Change the sheet name to grab values from another sheet. Rows 1-7 are used to facilitate formulas, delete if exporting to update the observation mapping table.')
        .setTextStyle(0, 99, SpreadsheetApp.newTextStyle()
        .setFontFamily('Arial')
        .build())
        .build());
        spreadsheet.getRange('F2').activate();
    }
    ```
    </details>

- Export rows from all three sheets to CSV file and use it to replace `observation-mapping.csv`.
- Add rows for AOE Questions:
    - Either copy the values from the website and map them into the appropriate columns 
      - [LOINC General](https://loinc.org/81959-9)
      - [LOINC COVID](https://loinc.org/sars-cov-2-and-covid-19/#aoe)

  -OR-  
    - Sign up for an account, download the CSV, and map the data from it
- Add rows for codes that have been added ad-hoc (not coming from any standardized source). 
Look for these codes in the current version of `observation-mapping.csv` file. They are listed at the end
and have `Condition Code System` = `ReportStream`.


Uploading the table to remote environments can utilize the lookuptables CLI command (./prime lookuptables create). Creating a new table with the same name will automatically create a new version of that table with that name and activate it if the -a parameter is used.

Example:

```
./prime lookuptables create -i "prime-router/metadata/tables/local/observation-mapping.csv" -s -a -n observation-mapping -e "prod or staging"
```
options:
```
-e, --env <name>       Connect to <name> environment. Choose between [local|test|staging|prod]
-i, --input-file PATH  Input CSV file with the table data
-s, --silent           Do not generate diff or ask for confirmation
-a, --activate         Activate the table upon creation
-n, --name TEXT        The name of the table to perform the operation on
--show-table           Always show the table to be created
-f, --force            Force the creation of new table(s) even if it is already exist
-h, --help             Show this message and exit
```

## Checking for mapping failures
Mapping failues are logged in the action log and can be viewed using the below query:

```postgresql
SELECT action_log.created_at,
       detail ->> 'message'      as message,
       detail ->> 'fieldMapping' as field,
       action_log.report_id,
       report_file.body_url
FROM action_log
         INNER JOIN report_file ON report_file.report_id = action_log.report_id
WHERE action_log.detail ->> 'errorCode' = 'INVALID_MSG_CONDITION_MAPPING'
ORDER BY action_log.created_at DESC
LIMIT 100;
```

Output will include the missing code, its origin, and the URL of the source data. Use the azure storage explorer
or the azure portal to download the file being careful to observe PII precautions.

### Example output

| message | field | report\_id | body\_url |
| :--- | :--- | :--- | :--- |
| Missing mapping for code\(s\): N | observation.valueCodeableConcept.coding.code | 3a947e0f-0832-403d-a9d8-92f9b88557a8 | http://localhost:10000/devstoreaccount1/reports/receive%2Fdevelopment.dev-elims%2FNone-3a947e0f-0832-403d-a9d8-92f9b88557a8-20240102233706.fhir |
| Missing mapping for code\(s\): Y | observation.valueCodeableConcept.coding.code | 3a947e0f-0832-403d-a9d8-92f9b88557a8 | http://localhost:10000/devstoreaccount1/reports/receive%2Fdevelopment.dev-elims%2FNone-3a947e0f-0832-403d-a9d8-92f9b88557a8-20240102233706.fhir |
| Missing mapping for code\(s\): N | observation.valueCodeableConcept.coding.code | 3a947e0f-0832-403d-a9d8-92f9b88557a8 | http://localhost:10000/devstoreaccount1/reports/receive%2Fdevelopment.dev-elims%2FNone-3a947e0f-0832-403d-a9d8-92f9b88557a8-20240102233706.fhir |
| Missing mapping for code\(s\): N | observation.valueCodeableConcept.coding.code | 3a947e0f-0832-403d-a9d8-92f9b88557a8 | http://localhost:10000/devstoreaccount1/reports/receive%2Fdevelopment.dev-elims%2FNone-3a947e0f-0832-403d-a9d8-92f9b88557a8-20240102233706.fhir |
| Missing mapping for code\(s\): 260415000 | observation.valueCodeableConcept.coding.code | 3a947e0f-0832-403d-a9d8-92f9b88557a8 | http://localhost:10000/devstoreaccount1/reports/receive%2Fdevelopment.dev-elims%2FNone-3a947e0f-0832-403d-a9d8-92f9b88557a8-20240102233706.fhir |


## Creating a new observation-mapping table from scratch

Note: This should not ever be necessary for production use as the table has already been created and should be iterated on. The instructions here are to document how the base table was created.

### Columns
Refer to the latest [observation-mapping.csv](/prime-router/metadata/tables/local/observation-mappings.csv). The column names below
are accurate as of the publish date of this document.

- Member OID
- Name
- Code
- Descriptor
- Code System
- Version
- Status
- condition_name
- condition_code
- Condition
- Code System
- Condition Code System Version
- Value Source
- Created At

The `Value Source` column needs to be manually entered based on data source below. Current possible values:
- LOINC.org
- RCTC

### Data sources
- Reportable Conditions Trigger Codes (RCTC): [AIMS](https://ersd.aimsplatform.org/#/home) *Account required*
  - Data from the following sheets:
    - Organism_Substance
    - Lab Order Test Name
    - Lab Obs Test Name
- AOE Questions *Account required for download (can copy/paste w/o)*
    - [LOINC General](https://loinc.org/81959-9)
    - [LOINC COVID](https://loinc.org/sars-cov-2-and-covid-19/#aoe)

## Sender Onboarding

As part of sender onboarding we will need to check the list of codes that the sender will be sending against the list of codes that are mapped in the observation-mapping table. In order to accomplish this we will need the list of LOINC/SNOMED order and result codes the sender will be sending (also known as a "compendium") in a CSV file. Information on this process can be found in [senders.md](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/onboarding-users/senders.md)

### How to map condition to LOINC/SNOMED code not found in RCTC and not an AOE

The RCTC is frequently updated and should contain the majority of codes that we receive. In the event we need to map a Lab Developed Test (LDT) or a LOINC/SNOMED code that is not represented in the RCTC
we will need to determine what standardized SNOMED condition code it should be mapped to. In some cases this will be obvious and we can use our best judgment (i.e. a test for Flu should be mapped to Influenza, a test for chlamydia should be mapped to chlmydia etc.).
In cases where the condition is not obvious we should check with the sender (this will usually be accomplished in the sender onboarding as described in [sender.md](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/onboarding-users/sender-onboarding/senders.md)). The sender's assertion of the appropriate condition the code is mapped to should be validated with a receiving STLT.
