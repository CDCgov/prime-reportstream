This folder is for lookup table test data to be uploaded for local testing only.  The
source of truth for database lookup tables are the tables stored in the production environment. 

Filename format is:
  `<table name>.csv`

## Loading the tables for local testing
The tables in this folder are loaded automatically when starting your docker container.
To load the tables manually you can run `./prime lookuptables loadall` or `./gradlew reloadTables`
The table names are extracted from the filename.  Note that this loading method is ONLY allowed in 
your local environment.

## Updating the local table files from an environment
You can update these files with the data residing in one of the environments.  
1. Login to the environment by running the command `./prime login`
2. Get a list of tables by running `./prime lookuptables list` 
3. Run the following command to fetch the data for given table and version:

`./prime lookuptables get -n <table_name> -v <table version> -o src/test/resources/metadata/tables/<table_name>.csv`


