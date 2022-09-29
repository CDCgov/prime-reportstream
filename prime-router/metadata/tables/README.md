This folder constains various lookup tables as follows:

## ROOT folder - DEPRECATED
Contains lookup tables loaded directly from files.

## _livd_ folder 
Contains the LIVD supplemental table that gets merged into the downloaded LIVD table by
running the command `./prime livd-table-update`. 

To update the LIVD Supplemental table:
1. Make the needed updates to the [LIVD-Supplemental.csv](livd/LIVD-Supplemental.csv) file.
2. Run `./prime livd-table-update -a`, verify the diff looks correct, then create the new lookup table version.
3. Test the changes locally.
4. Once the changes are verified, deploy the updated lookup table to staging and production.

## _local_ folder
These are CSV copies of lookup tables from production used for local testing.  
To reload this tables run `./gradlew reloadTables`. These should be periodically updated from production.

Filename format is:
`<table name>.csv`

### Loading the tables for local testing
The tables in this folder are loaded automatically when starting your docker container.
To load the tables manually you can run `./prime lookuptables loadall` or `./gradlew reloadTables`
The table names are extracted from the filename.  Note that this loading method is ONLY allowed in 
your local environment.

### Updating the local table files from an environment
You can update these files with the data residing in one of the environments.  
1. Login to the environment by running the command `./prime login`
2. Get a list of tables by running `./prime lookuptables list` 
3. Run the following command to fetch the data for given table and version:

`./prime lookuptables get -n <table_name> -v <table version> -o ./metadata/tables/local/<table_name>.csv`


