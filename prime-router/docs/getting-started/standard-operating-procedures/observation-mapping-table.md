# How to Update the Observation Mapping Table

## Introduction
The `observation-mapping` table maps a plethora of conditions, observations, and aoe questions to codes in various code
systems. The data for this table comes from multiple public sources. Once loaded in the database, the prime CLI can be
used to poll public RESTful sources for updated data.

A local csv file is kept both as a reference and to seed the local development environment. It should be updated
periodically by dumping the table from staging/prod or using the CLI to update the local table.

## Update prod/staging tables using RESTful sources
Use the prime CLI `lookuptables update-mapping` command:
Change the `--env` value as needed and be sure to provide your API key
```zsh
prime lookuptables update-mapping -a -n observation-mapping -k YOUR_API_KEY --env local
```
Other useful options:
- `-o` to write a copy of the updated table
- `-s` to run silently (no requests for confirmation)
- `-d` to provide a comma-separated list of OIDs to update
- `-i` to use an input file as the base to update from
- `-v` to use a specific version of the table as the base to update from

See the NMLS VSAC Support Center [here](https://www.nlm.nih.gov/vsac/support/usingvsac/vsacfhirapi.html) to request an
API key.

### Important Notes on Update
- This utility cannot support new OIDS because they must be manually mapped to a condition code. 
- This utility does not update AOE question mappings

## Update the local CSV by getting tables from prod/staging
Use the prime CLI `lookuptables get` command:  
Change the `--env` value as needed
```zsh
prime lookuptables get -n observation-mapping -v 1 -o observation-mappings.csv --env prod
```

## Create or update prod/staging tables using a local CSV
**NOTE** Once the table has been created (i.e. seeded), normal updates occur via the auto-update utility.
Except in rare circumstances, you should be using the [first section above](#update-prodstaging-tables-using-restful-sources).

Use the prime CLI `lookuptables create` command:
```zsh
./prime lookuptables create prod -i ./observation-mapping.csv -a -n observation-mapping
```

## Reload table locally
Run the gradle task `reloadTables`:
```zsh
./gradlew reloadTables
```

## Creating a CSV from data sources

### Columns
Refer to the latest [observation-mapping.csv](/prime-router/metadata/tables/local/observation-mappings.csv) or
use the following columns at your peril (the columns below may be out of date vs the CSV):

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
- RCTC: [AIMS](https://ersd.aimsplatform.org/#/home) *Account required*
- AOE Questions *Account required for download (can copy/paste w/o)*
  - [LOINC General](https://loinc.org/81959-9)
  - [LOINC COVID](https://loinc.org/sars-cov-2-and-covid-19/#aoe)

#### RCTC
We need to source data from the following spreadsheets:
- Organism-Substance
- Lab Order Test Name
- Lab Obs Test Name

Scroll down in the spreadsheet and you will find an `Expansion List`. Each of the rows in this table needs to be mapped
to a row in the preceding table using its Member OID. 

#### AOE Questions
Either copy the values from the website and map them into the appropriate columns  
-OR-  
Sign up for an account and download the CSV and map the data from it
