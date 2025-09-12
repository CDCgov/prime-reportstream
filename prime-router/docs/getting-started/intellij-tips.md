# IntelliJ Tips

## YAML Validation in Intellij IDEA

[See here](../../design/design/yaml-validation/yaml-intellij-setup.md)

## Source Dependency Workaround
The current ReportStream project builds properly using the Gradle build from the command line.  It works because Gradle has the concept of [source dependencies](https://blog.gradle.org/introducing-source-dependencies).  Specifically, ReportStream has a source dependency on its forked version of [hl7v2-fhir-converter in GitHub](https://github.com/CDCgov/hl7v2-fhir-converter).  When imported into the IDEA IntelliJ IDE, however, it results in an error referencing that module:

```
Could not determine the dependencies of task ':prime-router:compileJava'.
Could not resolve all dependencies for configuration ':prime-router:compileClasspath'.
   > Could not populate working directory from Git repository at https://github.com/CDCgov/hl7v2-fhir-converter.git.
```


### One Workaround

One workaround that avoids the dependency error in the IDE has been doscussed previously:

* Clone [https://github.com/CDCgov/hl7v2-fhir-converter.git]()
* Run `./gradlew publishToMavenLocal`
* Comment this block from `settings.gradle.kts`

```
sourceControl {
    gitRepository(URI("https://github.com/CDCgov/hl7v2-fhir-converter.git")) {
        producesModule("io.github.linuxforhealth:hl7v2-fhir-converter")
    }
}
```

* Add this to `shared.gradle.kts`

```
mavenLocal()
```

For example:

```
pluginManagement {
    repositories {
        mavenLocal()
        maven { url = uri("https://repo.spring.io/snapshot") }
        gradlePluginPortal()
    }
}
```

* Inside `build.gradle.kts` replace:

```
implementation("io.github.linuxforhealth:hl7v2-fhir-converter") {
    version {
        branch = "master"
    }
}
```

with:

```
implementation("io.github.linuxforhealth:hl7v2-fhir-converter:1.0.1-SNAPSHOT")
```

While this allows things to work inside of the IDE it also has the undesirable side effect of breaking the Gradle build.

### Alternative Solution

To make the Git source code dependency work in IntelliJ IDEA, you can manage it as a module dependency within the project. This approach allows you to include and build the dependent project locally as part of your main project (and doesn't appear to interfere with the Gradle build).  Follow these steps:

* Clone [https://github.com/CDCgov/hl7v2-fhir-converter.git]()
* Import `hl7v2-fhir-converter` as a dependent module of `prime-reportstream`
 * Navigate to **File | Project Structure**
 * In the Project Structure dialog, select **Modules** on the left.
 * Click the **+** button and choose **Import Module**.
 * Navigate to the directory where you cloned the `hl7v2-fhir-converter` project and select its `build.gradle` file.
 * Follow the prompts to import the module.
* Add the module dependency
 * While still in **Project Structure | Modules**, select the `prime-reportstream` module.
 * Navigate to the **Dependencies** tab.
 * Click the **+** button and choose **Module Dependency**.
 * Select the `hl7v2-fhir-converter` module (the dependent project) from the list and click **OK**.
 * Ensure the scope is set appropriately (e.g., **Compile** for general usage).
* **Build | Build project** in the IDE.

 After applying the changes, IntelliJ should recognize the changes and build the project automatically but it may be necessary to do it manually.


