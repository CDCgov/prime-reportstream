## Mapping sender codes (Compendium) to Reportable conditions

### Context
Because most STLT routing will have a condition filter, we need to ensure that all possible results from a sender are mapped to a reportable condition for routing. In order to accomplish this we will need the list of LOINC/SNOMED order and result codes the sender will be sending (also known as a "compendium") in a CSV file.

### ReportStream Mapping Template
A [Mapping template](docs/onboarding-users/sender-onboarding/mapping-sender-codes-to-condition/ReportStream Mapping Template.xlsm) is available to senders to help them generate the needed CSV file. This template can be found on the ReportStream website.

### Compendium File Format
This CSV file should be in the following format:

| test code                                 | test description              | coding system         |
|-------------------------------------------|-------------------------------|-----------------------|
| LOINC,SNOMED or LOCAL order/result code   | test order/result description | LOINC,SNOMED or LOCAL |


Example compendium:

| test code      | test description                                                                                             | coding system |
|----------------|--------------------------------------------------------------------------------------------------------------|---------------|
| 97099-6        | Influenza virus A and B and SARS-CoV-2 (COVID-19) Ag panel - Upper respiratory specimen by Rapid immunoassay | LOINC         | 
| 123456         | LDT Flu Test                                                                                                 | LOCAL         | 
| 47457-7        | Influenza virus A H8 Ab [Titer] in Serum                                                                     | LOINC         |
| 25141000087100 | Antigen of Bordetella pertussis fimbriae 2 (substance)                                                       | SNOMED        |

### Comparing compendium to the Observation-Mapping table

The sender compendium CSV can be compared to the current observation-mapping table using the ./prime lookuptables compare-mapping CLI command. If comparing against a table on a remote environment you will first need to use the ./prime login --env <prod-or-staing> command to get an access token. This command will generate an

Example
```
./prime lookuptables compare-mapping -n observation-mapping -v "table version to compare" -i "input-file-location" -o "output-file-location" -e "prod or staging"
```
options:
```
  -e, --env=<name>          Connect to <name> environment. Choose between
                            [local|test|staging|prod]
  -i, --input-file=<path>   Input CSV file with the sender compendium table
                            data
  -o, --output-file=<path>  Specify file to save annotated table data as CSV
  -n, --name=<text>         The name of the table to perform the comparison on
  -v, --version=<int>       The version of the table to get
  -h, --help                Show this message and exit
```

The output of the command will be another CSV file with an additional column indiciated a "Y" if the code is mapped in the observation mapping table and a "N" if it is unmapped. For all unmapped codes, work with the sender and receiving STLT to determine the correct condition to map the code to.

Example:

| test code      | test description                                                                                             | coding system | mapped?|
|----------------|--------------------------------------------------------------------------------------------------------------|---------------|--------|
| 97099-6        | Influenza virus A and B and SARS-CoV-2 (COVID-19) Ag panel - Upper respiratory specimen by Rapid immunoassay | LOINC         | Y      | 
| 123456         | LDT Flu Test                                                                                                 | LOCAL         | N      |
| 47457-7        | Influenza virus A H8 Ab [Titer] in Serum                                                                     | LOINC         | Y      |
| 25141000087100 | Antigen of Bordetella pertussis fimbriae 2 (substance)                                                       | SNOMED        | Y      |