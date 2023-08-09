# Postgres Database

## Data Restore
### Conditions Requiring Blob Restore

#### Storage Failover

If the storage account becomes unavailable in the East US region, you can initiate a failover to promote the secondary
storage location for use within the production system.

More in [these Azure docs](https://docs.microsoft.com/en-us/azure/storage/common/storage-disaster-recovery-guidance).

1. In the Azure Portal navigate to the storage account and click the `Geo-replication` blade.
2. Click the `Prepare for failover` button to initiate a failover to the secondary storage location.


### Storage Restore

If an entire storage account gets deleted it can be recovered within 90 days as long as an identically-named storage
account has not been created.

#### Actions to Mitigate

1. To restore a storage account, contact the CDC AD helpdesk.
2. At a minimum, provide the subscription ID and the name of the storage account that needs to be restored.  If
   possible, a resource ID would provide the with the most information.


### File Restore

If a file is inadvertently deleted or edited, a previous version of the file can be made the current version within the
60-day retention window.

More in [these Azure docs](https://learn.microsoft.com/en-us/azure/backup/backup-azure-restore-files-from-vm)

1. In Azure Portal navigate to the appropriate blob container under the `Containers` blade.
2. If a file was deleted, toggle on the `Show deleted blobs` button to be able to see it.
3. Click the deleted or edited file to be restored.
4. Click the `Versions` tab.
5. In the options for the correct version of the file, click `Make current version` to restore the file to the
   appropriate point in time.

## DB Failover restore
### Database Failure Conditions

In the event of an extended PostgreSQL server outage there are two options to restore service. The quickest and most
complete is to promote the read-replica server to a read/write replica. You can also provision a new server from a
point-in-time backup.

In either case you'll need to re-configure any necessary resources (i.e., the Function App and Metabase, at a minimum)
to connect to it. You should do this through Terraform to ensure the configuration is sticky, but you may want to first
test by editing the value in the Azure Portal.

More in
[these Azure docs](https://docs.microsoft.com/en-us/azure/postgresql/concepts-read-replicas?msclkid=186f9575ac6d11eca07f9c72ead6d20d#failover-to-replica).


#### Promote Read Replica to Read/Write

NOTE: This action is not reversible, so be certain that you want to do this.

1. In the Azure Portal, navigate to the PostgreSQL server that has read/write capabilities.
2. In the `Replication` blade, click the `Stop Replication` button. This will promote the read-replica to a standalone
   read/write instance.
3. Update the connection strings for any resources requiring database access.
4. NOTE: Performance may be degraded if your compute resources are in a different region than your database, so you may
   need to adjust any alert settings.


#### Provision New Server From Backup

1. In the Azure Portal, navigate to the PostgreSQL server that has read/write capabilities.
2. In the `Overview` blade, click the `Restore` button.
3. Select the date and time to which to restore a new uniquely named server to.
4. Click `Ok` to provision the new server from the point-in-time backup.

## DB Flyaway Repair
If the Flyway migrations need a repair, the following commands will resolve in each environment.

## TEST

```shell
az login
export FLYWAY_PASSWORD=$(az account get-access-token --resource-type oss-rdbms | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")
FLYWAY_URL=jdbc:postgresql://pdhtest-pgsql.postgres.database.azure.com:5432/prime_data_hub_candidate?sslmode=require FLYWAY_USER=reportstream_pgsql_admin@pdhtest-pgsql ./gradlew flywayRepair
FLYWAY_URL=jdbc:postgresql://pdhtest-pgsql.postgres.database.azure.com:5432/prime_data_hub?sslmode=require FLYWAY_USER=reportstream_pgsql_admin@pdhtest-pgsql ./gradlew flywayRepair
```

## STAGING

```shell
az login
export FLYWAY_PASSWORD=$(az account get-access-token --resource-type oss-rdbms | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")
FLYWAY_URL=jdbc:postgresql://pdhstaging-pgsql.postgres.database.azure.com:5432/prime_data_hub_candidate?sslmode=require FLYWAY_USER=reportstream_pgsql_admin@pdhstaging-pgsql ./gradlew flywayRepair
FLYWAY_URL=jdbc:postgresql://pdhstaging-pgsql.postgres.database.azure.com:5432/prime_data_hub?sslmode=require FLYWAY_USER=reportstream_pgsql_admin@pdhstaging-pgsql ./gradlew flywayRepair
```

## PROD

```shell
az login
export FLYWAY_PASSWORD=$(az account get-access-token --resource-type oss-rdbms | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")
FLYWAY_URL=jdbc:postgresql://pdhprod-pgsql.postgres.database.azure.com:5432/prime_data_hub_candidate?sslmode=require FLYWAY_USER=reportstream_pgsql_admin@pdhprod-pgsql ./gradlew flywayRepair
FLYWAY_URL=jdbc:postgresql://pdhprod-pgsql.postgres.database.azure.com:5432/prime_data_hub?sslmode=require FLYWAY_USER=reportstream_pgsql_admin@pdhprod-pgsql ./gradlew flywayRepair
```

## How to Log and Debug Database Queries

### Introduction
Sometimes you need a bit more insight into what's actually happening on the database.

Here's a few ways to log queries and execution plans.

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
docker-compose -f docker-compose -f docker-compose.build.yml stop postgresql
docker-compose -f docker-compose -f docker-compose.build.yml rm postgresql
docker-compose -f docker-compose -f docker-compose.build.yml up -d postgresql
```

## Logging In
This is the process for logging into our database via Azure Active Directory.

### Login to Azure CLI

```shell
az login
```

### Request a token for Active Directory

```shell
az account get-access-token --resource-type oss-rdbms
```

You will receive a response that looks like:

```json
{
  "accessToken": "token",
  "expiresOn": "2021-04-22 16:22:54.350957",
  "subscription": "sub",
  "tenant": "id",
  "tokenType": "Bearer"
}
```

Copy the value from `accessToken`. This will be your password. It will expire in one hour after request. If your token expires, run this command again.

After connecting using this password, your session will remain active beyond the token expiration.

### Login to PostgreSQL
**NOTE: the below is currently not working. There is discussion of updating the process. In the meantime, please contact your on-boarding buddy or a teammate that has access to prod, and they can get you set up.**

Using your PostgreSQL tool of choice, login with the following details:

* For write / schema access: `reportstream_pgsql_admin@dbservername`
    * Only use this if you have a specific reason
* For read-only access: `reportstream_pgsql_developer@dbservername`
* Password: `<the accessToken from above>`

Note: The tool pgAdmin is not supported at this time. [Microsoft notes access tokens are longer](https://docs.microsoft.com/en-us/azure/postgresql/howto-configure-sign-in-aad-authentication#connecting-to-azure-database-for-postgresql-using-azure-ad) than pgAdmin's char size for passwords.

#### Example in staging

Get your access token and set as envvar `PGPASSWORD` in one go:
```shell
export PGPASSWORD=$(az account get-access-token --resource-type oss-rdbms | python3 -c "import sys, json; print(json.load(sys.stdin)['accessToken'])")
```

Connect to the staging database with read-only access:
```shell
psql "host=pdhstaging-pgsql.postgres.database.azure.com user=reportstream_pgsql_developer@pdhstaging-pgsql dbname=prime_data_hub sslmode=require"
```



