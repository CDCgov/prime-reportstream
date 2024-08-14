# Running ReportStream Tests

The tests in ReportStream assume that you have followed the project setup including having [built the project baseline](../getting-started/README.md#bulding-the-baseline) and have your docker containers running.

- [Smoke Tests](#smoke-tests)
- [Unit Tests](#unit-tests)
- [End-to-End Tests](#end-to-end-tests)
- [Integration Tests](#integration-tests)
- [Other Groups of Tests](#other-groups-of-tests)

## Smoke tests

Smoke tests are useful after performing the backend setup to confirm that everything is working.

```bash
# All gradle and prime commands require being run in the prime-router directory
cd ./prime-router

./gradlew testSmoke
```
or
```bash
./prime test 
```

### Running the smoke tests on Staging

To run the smoke tests on staging you will need to set several environment variables from `pdhstaging-functionapp` in Azure.

```bash
export POSTGRES_PASSWORD=<postgres-password>
export POSTGRES_USER=<postgres-user>@pdhstaging-pgsql                                                                                                          
export POSTGRES_URL=jdbc:postgresql://pdhstaging-pgsql.postgres.database.azure.com:5432/prime_data_hub
export AzureWebJobsStorage="<AzureWebJobsStorage>"
./prime test --env staging --key <reports-endpoint-function-key> 
```

To run the specific tests on Staging use this:

```bash
./prime test --run end2end_up --env staging --key <reports-endpoint-function-key>
```

## Unit tests

The build will run the unit tests for you when you invoke commands like `./gradlew package` and `./gradlew run`. However, you may sometimes want to invoke them explicitly. Use this command to run the unit tests manually:

```bash
./gradlew test
# Or to force the unit tests to run
./gradlew test -Pforcetest
```

### Running smaller sets of unit tests

The simplest way to run smaller sets of unit tests is through the IDE. You should specify the project name of the tests (`:prime-router` in the below example). Should you want to run a specific class of unit tests or single unit test in the command line, you may do so like the following:

```bash
./gradlew :prime-router:test --tests "[class path or class path with test name]"
```

To run an entire class: 
```bash
./gradlew :prime-router:test --tests "gov.cdc.prime.router.fhirengine.engine.FhirRouterTests"
```

To run a single test:
```bash
./gradlew :prime-router:test --tests "gov.cdc.prime.router.fhirengine.engine.FhirRouterTests.test applyFilters receiver setting - (reverseTheQualityFilter = true) "
```
 
## End-to-end tests

End-to-end tests check if the deployed system is configured correctly. The tests use an organization called IGNORE for running the tests. Note: These tests are included in the smoke test suite. 

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

## Integration tests

Integration tests are not run with the unit tests or the smoke tests. To run them do the following:

```bash
./gradlew testIntegration
```

To run an entire class:
```bash
./gradlew testIntegration --tests "gov.cdc.prime.router.FileNameTemplateIntegrationTests" 
```

To run a single test:
```bash
./gradlew testIntegration --tests "gov.cdc.prime.router.FileNameTemplateIntegrationTests.test literal name element" 
```

## Other groups of tests

The smoke tests are a subset of tests that can be invoked directly from the command line. To see a list of available tests, their categorization and a brief description, run:

```bash
./prime test --list
```

These tests can be invoked using the listed name like so:

```bash
./prime test --run <name>
```