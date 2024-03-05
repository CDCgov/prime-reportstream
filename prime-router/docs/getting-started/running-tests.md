# Running ReportStream Tests

The tests in ReportStream assume that you have followed the project setup including having [built the project baseline](../getting-started/README.md#bulding-the-baseline) and have your docker containers running.

## Smoke tests

You should be able to run tests after performing the backend setup to confirm that everything is working.

```bash
# Smoke test checks that most of the system is working
./gradlew testSmoke
```
or
```
./prime test 
```

## Running the unit tests

The build will run the unit tests for you when you invoke `./gradlew package`. However, you may sometimes want to invoke them explicitly. Use this command to do run the Unit Tests manually:

```bash
cd ./prime-router
./gradlew test
# Or to force the unit tests to run
./gradlew test -Pforcetest
```
 
## Running the end-to-end tests locally

End-to-end tests check if the deployed system is configured correctly. The tests use an organization called IGNORE for running the tests. 

```bash
./gradlew testEnd2End
./gradlew testEnd2EndUP
```
or 
```bash
./prime test --run end2end
./prime test --run end2end_up
```

Upon completion, the process should report success.

## Running the end2end test on Staging

To run the end2end test on Staging you'll need a `<postgres-user>` and `<postgres-password>`, VPN tunnel access, and a `<reports-endpoint-function-key>` 

With your VPN running, do the following:
    
```    
export POSTGRES_PASSWORD=<postgres-password>
export POSTGRES_USER= <postgres-user>@pdhstaging-pgsql                                                                                                          
export POSTGRES_URL=jdbc:postgresql://pdhstaging-pgsql.postgres.database.azure.com:5432/prime_data_hub
./prime test --run end2end --env staging --key <reports-endpoint-function-key> 
```    

To run the entire smoke test suite on Staging use this:

```
./prime test -env staging --key <reports-endpoint-function-key>
```
