## Mapping sender codes (Compendium) to Reportable conditions

### Context
Because most STLT routing will have a condition filter, we need to ensure that all possible results from a sender are mapped to a reportable condition for routing. In order to accomplish this we will need the list of LOINC/SNOMED order and result codes the sender will be sending (also known as a "compendium") in a CSV file.

### Compenium File Format
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

### Comparing Using the API Endpoint

An API endpoint also exists to achieve the same function as the CLI command. This endpoint
can be utilized by sending a `POST` request to `/api/sender/conditionCode/comparison` with the
compendium CSV as the body payload. The compendium should be in the exact CSV format as shown in
previous sections.

If a valid compendium is posted, the server will perform the same code to condition mapping as
the CLI command. The result is returned in JSON format with the mapping results added to the 
compendium data. The result of the mapping check will be added as the `mapped?` key for each entry.

Example:

Input compendium CSV as HTTP POST body:
```csv
test code,test description,coding system
97097-0,SARS-CoV-2 (COVID-19) Ag [Presence] in Upper respiratory specimen by Rapid  immunoassay,LOINC
80382-5,Influenza virus A Ag [Presence] in Upper respiratory specimen by Rapid immunoassay,LOINC
12345,Flu B,LOCAL
```

Output JSON:
```json
[
    {
        "test code": "97097-0",
        "test description": "SARS-CoV-2 (COVID-19) Ag [Presence] in Upper respiratory specimen by Rapid  immunoassay",
        "coding system": "LOINC",
        "mapped?": "Y"
    },
    {
        "test code": "80382-5",
        "test description": "Influenza virus A Ag [Presence] in Upper respiratory specimen by Rapid immunoassay",
        "coding system": "LOINC",
        "mapped?": "Y"
    },
    {
        "test code": "12345",
        "test description": "Flu B",
        "coding system": "LOCAL",
        "mapped?": "N"
    }
]
```