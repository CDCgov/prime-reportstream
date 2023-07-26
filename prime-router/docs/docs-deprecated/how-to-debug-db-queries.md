# How to Log and Debug Database Queries

## Introduction
Sometimes you need a bit more insight into what's actually happening on the database.

Here's a few ways to log queries and execution plans.

## Log all queries using log4j

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

## Logging individual queries on the backend

Say we want to log a specific query(s), maybe we're iterating on a new query, implementing a query filter, or
optimizing. This is a little more annoying to set up (especially for multiple queries in multiple files), but the
output is a lot more readable.

### Steps

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

## Logging all queries to a file

The firehose. Turn on query logging in postgres and log them all to the file. The output is a more difficult to read
but you get complete coverage of queries that run.

### Steps

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