# Flyway Database Repair

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