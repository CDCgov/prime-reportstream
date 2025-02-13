# Postgres Database

## Introduction
The Postgres database powers [ReportStream's Data Model](../design/design/data-model.md) and is currently managed in 
Microsoft Azure. 

## Creating and Deploying Database Migrations

Database migrations are defined by the .sql files located in `prime-router/src/main/resources/db/migration` and are applied by Flyway. `Flyway migrate` runs as part of the gradle build and is also invoked in the process of the application creating a database connection (see `DatabaseAccess.getDataSource`). This process is able to automatically handle short-running migrations fairly well, but starts to cause problems when the migrations take a long time to complete. Special handling of long-running migrations is required to reduce potential application stability and availability issues.

> **Additional Reading:** Reference the following [incident report](https://cdc.sharepoint.com/:w:/r/teams/ReportStream/Shared%20Documents/Product/Incident%20Reports/2025_02_06%20-%20Application%20Outage%20Incident%20Report.docx?d=w5032ba7b86044669bdf29dcbf1399370&csf=1&web=1&e=qHeG1U) to understand why long-running database migrations require the special process outlined in the following section

### Special Handling of Long-Running Migrations
When deploying long-running migrations (usually index creations) to staging or production, the migrations should be performed manually by running the SQL statements directly on the database in question during the ReportStream maintenance window (Sunday morning) to minimize disruptions. Once the migrations have been manually applied, the migration file can be merged to the main branch for the purposes of documentation and future reproducibility. This means statements should be written in such a way as to prevent them from running again by using the `IF NOT EXISTS` keywords, like so:

```
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_item_lineage_created_at
ON item_lineage
USING btree (created_at)
;
```

When creating an index, special care should be taken to use the "CONCURRENTLY" parameter so Postgres will build the index without taking any locks that prevent concurrent inserts, updates, or deletes on the table in question.

## DB Flyaway Repair
If the Flyway migrations need a repair, the following commands will resolve in each environment.

### TEST

```shell
az login
export FLYWAY_PASSWORD=$(az account get-access-token --resource-type oss-rdbms | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")
FLYWAY_URL=jdbc:postgresql://pdhtest-pgsql.postgres.database.azure.com:5432/prime_data_hub_candidate?sslmode=require FLYWAY_USER=reportstream_pgsql_admin@pdhtest-pgsql ./gradlew flywayRepair
FLYWAY_URL=jdbc:postgresql://pdhtest-pgsql.postgres.database.azure.com:5432/prime_data_hub?sslmode=require FLYWAY_USER=reportstream_pgsql_admin@pdhtest-pgsql ./gradlew flywayRepair
```

### STAGING

```shell
az login
export FLYWAY_PASSWORD=$(az account get-access-token --resource-type oss-rdbms | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")
FLYWAY_URL=jdbc:postgresql://pdhstaging-pgsql.postgres.database.azure.com:5432/prime_data_hub_candidate?sslmode=require FLYWAY_USER=reportstream_pgsql_admin@pdhstaging-pgsql ./gradlew flywayRepair
FLYWAY_URL=jdbc:postgresql://pdhstaging-pgsql.postgres.database.azure.com:5432/prime_data_hub?sslmode=require FLYWAY_USER=reportstream_pgsql_admin@pdhstaging-pgsql ./gradlew flywayRepair
```

### PROD

```shell
az login
export FLYWAY_PASSWORD=$(az account get-access-token --resource-type oss-rdbms | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")
FLYWAY_URL=jdbc:postgresql://pdhprod-pgsql.postgres.database.azure.com:5432/prime_data_hub_candidate?sslmode=require FLYWAY_USER=reportstream_pgsql_admin@pdhprod-pgsql ./gradlew flywayRepair
FLYWAY_URL=jdbc:postgresql://pdhprod-pgsql.postgres.database.azure.com:5432/prime_data_hub?sslmode=require FLYWAY_USER=reportstream_pgsql_admin@pdhprod-pgsql ./gradlew flywayRepair
```

## How to Log and Debug Database Queries
Sometimes you need a bit more insight into what's actually happening on the database.

Here's a few ways to log queries.

### Log all queries using log4j

1. Locate and open the file `prime-router/src/main/kotlin/resources/log4j2.xml`
2. Find the section on loggers and locate the jooq logger
```xml
    <Loggers>
        <!-- Setting to debug enables logging queries -->
        <Logger name="org.jooq" level="info" additivity="false"/>
    </Loggers>
```
3. Set the `level` attribute in the JOOQ logger:
    1. `error` prints errors
    2. `warn` prints warnings
    3. `info` prints status information
    4. `debug` prints queries (object notation) and their output/params
    5. `trace` prints queries (raw sql) and other jooq internals (var binds, timings)  
       \* each level includes the levels above

The logs will appear alongside general application logging.

### Logging individual queries on the backend

Say we want to log a specific query(s), maybe we're iterating on a new query, implementing a query filter, or
optimizing. This is a little more annoying to set up (especially for multiple queries in multiple files), but the
output is a lot more readable.

#### Steps

1. Add these imports where you want to log the query:

```kotlin
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.DSLContext
```

2. Create a context to print the query

```kotlin
val prettyContext: DSLContext = DSL.using(SQLDialect.POSTGRES, Settings().withRenderFormatted(true))
```

3. Construct the query you want to log

```kotlin
val prettyQuery = prettyContext.select(
    ACTION.ACTION_ID, ACTION.CREATED_AT, ACTION.SENDING_ORG, ACTION.SENDING_ORG_CLIENT,
    REPORT_FILE.RECEIVING_ORG, REPORT_FILE.RECEIVING_ORG_SVC,
    ACTION.HTTP_STATUS, ACTION.EXTERNAL_NAME, REPORT_FILE.REPORT_ID, REPORT_FILE.SCHEMA_TOPIC,
    REPORT_FILE.ITEM_COUNT, REPORT_FILE.BODY_URL, REPORT_FILE.SCHEMA_NAME, REPORT_FILE.BODY_FORMAT
)
    .from(
        ACTION.join(REPORT_FILE).on(
            REPORT_FILE.ACTION_ID.eq(ACTION.ACTION_ID)
        )
    )
    .where(whereClause)
    .orderBy(sortedColumn)

if (cursor != null) {
    prettyQuery.seek(cursor)
}

prettyQuery.limit(pageSize)
```

4. Print it!

```kotlin
println(prettyQuery.sql)
```

### Logging all queries to a file

The firehose. Turn on query logging in postgres and log them all to the file. The output is a more difficult to read
but you get complete coverage of queries that run.

#### Steps

1. Add the following volume and command entry to `docker-compose.build.yml` under the `postgresql` container.

```yaml
  postgresql:
    volumes:
      - ./logs:/logs
    command: postgres -c logging_collector=on -c log_destination=stderr -c log_directory=/logs -c log_statement=all
```

2. Remove and rebuild your postgresql container using the build docker-file

```shell
docker compose -f docker-compose.build.yml stop postgresql
docker compose -f docker-compose.build.yml rm postgresql
docker compose -f docker-compose.build.yml up -d postgresql
```

## Logging In
This section documents the process for logging into the staging instance of the Postgres database via Azure Active Directory. For production access reach out to Devops.

### Staging
Make sure that your IP address is added to Connection Security on the database.
1. Go to Azure Database for Postgres Servers
2. Select the Staging DB 
3. Click Connection Security in the side panel
4. Add your IP address as both the `Start` and `End` IP Address and label it with your name

#### Login to Azure CLI

```shell
az login
```

#### Login to PostgreSQL
Get your access token and set as envvar `PGPASSWORD` in one go:
```shell
export PGPASSWORD=$(az account get-access-token --resource-type oss-rdbms | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")
```

Connect to the staging database with read-only access:
```shell
psql "host=pdhstaging-pgsql.postgres.database.azure.com user=reportstream_pgsql_developer@pdhstaging-pgsql dbname=prime_data_hub sslmode=require"
```

### Local Setup/Troubleshooting
The good news is, Postgres will work out of the box with our setup! However, there are times when things get out of wack
(for example if you get something like 
`Unable to obtain connection from database (jdbc:postgresql://localhost:5432/prime_data_hub) for user 'prime': FATAL: role "prime" does not exist`)
a good debugging step is to run the following:
```
export POSTGRES_URL=jdbc:postgresql://localhost:5432/prime_data_hub
export POSTGRES_USER=prime
export POSTGRES_PASSWORD=changeIT!
```

If you are getting and error like `Stack: java.lang.Exception: Error loading schema catalog: ./metadata/schemas` or 
other database setup issues take these 5 steps:
1. Shutdown RS
2. ./gradlew resetDB 
3. Start RS
4. ./gradlew reloadTables
5. ./gradlew reloadSettings
AND IT NEEDS TO BE IN THAT ORDER due to prerequisites


### Postgres Gradle Actions
There are three gradle actions that we use to interact with the database:
- `./gradlew clearDB` - Truncate/empty all tables in the database that hold report and related data, and leave settings
- `./gradlew reloadTables` - Load the latest test lookup tables to the database
- `./gradlew resetDB` - Delete all tables in the database and recreate from the latest schema
To see all gradle action definitions, go to `build.gradle.kts`

### Managing Postgres with Flyway
Flyway is an open-source database-migration tool that runs files in 
`prime-router/src/main/resources/db/migration` directory. If you need to make changes to the database, a PR needs to be 
submitted with an incremented file version added to the migration directory. 
