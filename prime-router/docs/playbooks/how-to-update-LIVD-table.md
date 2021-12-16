# How To Update The LIVD Table

---
## Introduction

---
HHS maintains a list of all authorized covid test devices as an xlsx. ReportStream
keeps a local csv version copy of that list. This local csv file needs to be updated
once the new LIVD table comes out. 

## Steps

---
1. Download LIVD table from https://www.cdc.gov/csels/dls/sars-cov-2-livd-codes.html
2. Click LOINC Mapping tab
3. Save file in CSV format
4. Follow naming convention: LIVD-SARS-CoV-2-latest.csv
5. Go to prime-router/metadata/tables and replace old LIVD table with new one 
### Note:
Since we are using this naming convention, we don't need to do anything to the covid-19 schema.
If we ever goes back to using the old naming convention, with the date in the name, we would need to edit the covid-19 schema by 
replacing the old LIVD table name with the new one (in all entries in the covid-19 schema).

6. Run ```./gradlew generatedocs``` command
7. Make sure these tests pass, if not make appropriate changes to the test cases that fail
     - ```gradlew test```
     - ```gradlew testIntegration```
     - ```gradlew testSmoke```
8. Lastly, change the values in the supplemental table, for any model name, if it has change in the SARs table
    - To do this, you need to check the model name in LIVD-Supplemental-20201-06-07.csv file
    - Make sure that the model name and is present in the new LIVD table.
    - If the model name has change, record the new changes, in the Supplemental table; make sure the test kit name matches as well
    - Run tests in step #7
### Note: 
Model names in the SARs table don't change as often, making step 8 just a checking routine