# Gradle

## Using Gradle

`gradle tasks` will list all the tasks that Gradle can do for you.

You can invoke `gradlew` from the `./prime-router` directory to build the baseline as follows:

```bash
./gradlew clean package
```

The most useful gradle tasks are:

* `clean`: deletes the build artifacts
* `compile`: compiles the code
* `test`: runs the unit tests
* `testIntegration`: runs the integration tests
* `package`: packages the build artifacts for deployment
* `quickpackage`: re-packages the build artifacts for deployment without running the tests
* `testSmoke`: runs all the smoke tests; this requires [that you are running ReportStream](../getting-started/README.md#running-reportstream-backend)
* `testEnd2End`: runs the end-to-end test; this requires [that you are running ReportStream](../getting-started/README.md#running-reportstream-backend)
* `primeCLI`: run the prime CLI. Specify arguments with `"--args=<args>"`
