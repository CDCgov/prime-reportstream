# Running ReportStream Tests

## Smoke Tests

You should be able to run tests now to confirm that everything is working.

```bash
# Smoke test checks that most of the system is working
./gradlew testSmoke
```

## Running the unit tests

The build will run the unit tests for you when you invoke `./gradlew package`. However, you may sometimes want to invoke them explicitly. Use this command to do run the Unit Tests manually:

```bash
cd ./prime-router
./gradlew test
# Or to force the tests to run
./gradlew test -Pforcetest
```

## Data conversion quick test

The quick test is meant to test the data conversion and generation code. Use this following command to run all quick tests, which you should do as part of a Pull Request:

```bash
./quick-test.sh all
```

## Running the end-to-end tests locally

End-to-end tests check if the deployed system is configured correctly. The tests use an organization called IGNORE for running the tests. In order to successfully run the end-to-end tests, you will need to:

1. Have built successfully
2. Export the vault's credentials

    ```bash
    cd ./prime-router
    export $(xargs < .vault/env/.env.local)
    ```

3. Create the SFTP credentials and upload organizations' settings

    ```bash
    cd ./prime-router
    ./prime create-credential --type=UserPass \
            --persist=DEFAULT-SFTP \
            --user foo \
            --pass pass
    ```

4. Ensure that your docker containers are running (see also "[Running ReportStream](#running-reportstream)")

    ```bash
    cd ./prime-router
    # Specify restart if they are already running and you want
    # them to pick up new bianries
    # i.e. ./devenv-infrastructure.sh restart
    ./devenv-infrastructure.sh
    ```

5. Run the tests

    ```bash
    ./gradlew testEnd2End
    ```
    or 
    ```bash
    ./prime test --run end2end
    ```
    Or to run the entire smoke test suite locally:
    ```
    ./prime test
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
