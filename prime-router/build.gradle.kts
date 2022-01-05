/*
Build script for Prime Router.

Properties that can be overridden using the Gradle -P arguments or environment variables:
  DB_USER - Postgres database username (defaults to prime)
  DB_PASSWORD - Postgres database password (defaults to changeIT!)
  DB_URL - Postgres database URL (defaults to jdbc:postgresql://localhost:5432/prime_data_hub)

Properties that can be overriden using an environment variable only:
  PRIME_RS_API_ENDPOINT_HOST - hostname on which your API endpoint runs (defaults to localhost);
                               This will enable you to connect to your API endpoint from (e.g.)
                               the builder container

Properties to control the execution and output using the Gradle -P arguments:
  forcetest - Force the running of the test regardless of changes
  showtests - Verbose output of the unit tests
  E.g. ./gradlew clean package -Ppg.user=myuser -Dpg.password=mypassword -Pforcetest
 */

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.flywaydb.flyway") version "8.3.0"
    id("nu.studer.jooq") version "6.0.1"
    id("com.github.johnrengelman.shadow") version "7.1.1"
    id("com.microsoft.azure.azurefunctions") version "1.8.2"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    id("com.adarshr.test-logger") version "3.1.0"
    id("jacoco")
}

group = "gov.cdc.prime"
version = "0.1-SNAPSHOT"
description = "prime-router"
val azureAppName = "prime-data-hub-router"
val azureFunctionsDir = "azure-functions"
val primeMainClass = "gov.cdc.prime.router.cli.MainKt"
azurefunctions.appName = azureAppName

// Local database information, first one wins:
// 1. Project properties (-P<VAR>=<VALUE> flag)
// 2. Environment variable
// 3. Default
val KEY_DB_USER = "DB_USER"
val KEY_DB_PASSWORD = "DB_PASSWORD"
val KEY_DB_URL = "DB_URL"
val KEY_PRIME_RS_API_ENDPOINT_HOST = "PRIME_RS_API_ENDPOINT_HOST"
val dbUser = (
    project.properties[KEY_DB_USER]
        ?: System.getenv(KEY_DB_USER)
        ?: "prime"
    ) as String
val dbPassword = (
    project.properties[KEY_DB_PASSWORD]
        ?: System.getenv(KEY_DB_PASSWORD)
        ?: "changeIT!"
    ) as String
val dbUrl = (
    project.properties[KEY_DB_URL]
        ?: System.getenv(KEY_DB_URL)
        ?: "jdbc:postgresql://localhost:5432/prime_data_hub"
    ) as String

val reportsApiEndpointHost = (
    System.getenv(KEY_PRIME_RS_API_ENDPOINT_HOST)
        ?: "localhost"
    )

val jooqSourceDir = "build/generated-src/jooq/src/main/java"
val jooqPackageName = "gov.cdc.prime.router.azure.db"

/**
 * Add the `VAULT_TOKEN` in the local vault to the [env] map
 */
fun addVaultValuesToEnv(env: MutableMap<String, Any>) {
    val file = File(".vault/env/.env.local")
    if (!file.exists()) return
    val prop = Properties()
    FileInputStream(file).use { prop.load(it) }
    prop.forEach { key, value -> env[key.toString()] = value.toString().replace("\"", "") }
}

defaultTasks("package")

val ktorVersion = "1.6.7"
val kotlinVersion = "1.6.10"
jacoco.toolVersion = "0.8.7"

// Set the compiler JVM target
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val compileKotlin: KotlinCompile by tasks
val compileTestKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "11"
compileKotlin.kotlinOptions.allWarningsAsErrors = true
compileTestKotlin.kotlinOptions.jvmTarget = "11"
compileTestKotlin.kotlinOptions.allWarningsAsErrors = true

tasks.clean {
    // Delete the old Maven build folder
    delete("target")
    // clean up all the old event files in the SOAP set up
    doLast {
        val eventsDir = File("../.environment/soap_service/soap/event/v1")
        if (eventsDir.exists() && eventsDir.isDirectory && eventsDir.listFiles().isNotEmpty()) {
            // Note FileUtils does not like when the folder is empty.
            FileUtils.listFiles(eventsDir, arrayOf("event"), true).forEach {
                it.delete()
            }
        }
    }
}

/**
 * Building tasks
 */
val coverageExcludedClasses = listOf("gov/cdc/prime/router/azure/db/*", "gov/cdc/prime/router/cli/tests/*")
tasks.test {
    // Use JUnit 5 for running tests
    useJUnitPlatform()

    // Set the environment to local for the tests
    environment["PRIME_ENVIRONMENT"] = "local"
    environment["POSTGRES_URL"] = dbUrl
    environment["POSTGRES_USER"] = dbUser
    environment["POSTGRES_PASSWORD"] = dbPassword

    // Set max parellel forks as recommended in https://docs.gradle.org/current/userguide/performance.html
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    dependsOn("compileKotlin")
    finalizedBy("jacocoTestReport")
    // Run the test task if specified configuration files are changed
    inputs.files(
        fileTree("./") {
            include("settings/**/*.yml")
            include("metadata/**/*")
        }
    )
    outputs.upToDateWhen {
        // Call gradle with the -Pforcetest option will force the unit tests to run
        if (project.hasProperty("forcetest")) {
            println("Rerunning unit tests...")
            false
        } else {
            true
        }
    }
    configure<JacocoTaskExtension> {
        // This excludes classes from being analyzed, but not from being added to the report
        excludes = coverageExcludedClasses
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    // Jacoco wants the source file directory structure to match the package name like in Java, so 
    // move the source files to a temp location with that structure.
    val sourcesDir = File(project.projectDir, "/src/main/kotlin")
    val jacocoSourcesDir = File(project.buildDir, "/jacoco/sources")
    doFirst {
        FileUtils.listFiles(sourcesDir, arrayOf("kt", "java"), true).forEach { sourceFile ->
            // Find the line in the code that has the package name and convert that to a folder then copy the file.
            FileUtils.readLines(sourceFile, "UTF8").firstOrNull { it.contains("package") }?.let {
                val packageDir = it.split(" ").last().replace(".", "/")
                FileUtils.copyFile(
                    sourceFile,
                    File(jacocoSourcesDir, "$packageDir/${FilenameUtils.getName(sourceFile.absolutePath)}")
                )
            }
        }
    }
    additionalSourceDirs(jacocoSourcesDir)
    reports.xml.required.set(true)
    // Remove the exclusions, so they do not appear in the report
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it).matching {
                    exclude(coverageExcludedClasses)
                }
            }
        )
    )
}

testlogger {
    if (project.hasProperty("showtests")) {
        showPassed = true
        showSkipped = true
    } else {
        showPassed = false
        showSkipped = false
    }
}

// Add the testIntegration tests
sourceSets.create("testIntegration") {
    java.srcDir("src/testIntegration/kotlin")
    compileClasspath += sourceSets["main"].output
    runtimeClasspath += sourceSets["main"].output
}

val compileTestIntegrationKotlin: KotlinCompile by tasks
compileTestIntegrationKotlin.kotlinOptions.jvmTarget = "11"

val testIntegrationImplementation: Configuration by configurations.getting {
    extendsFrom(configurations["testImplementation"])
}

configurations["testIntegrationRuntimeOnly"].extendsFrom(configurations["runtimeOnly"])

tasks.register<Test>("testIntegration") {
    useJUnitPlatform()
    dependsOn("compile")
    dependsOn("compileTestIntegrationKotlin")
    dependsOn("compileTestIntegrationJava")
    shouldRunAfter("test")

    // Set the environment to local for the tests
    environment["PRIME_ENVIRONMENT"] = "local"
    environment["POSTGRES_URL"] = dbUrl
    environment["POSTGRES_USER"] = dbUser
    environment["POSTGRES_PASSWORD"] = dbPassword

    testClassesDirs = sourceSets["testIntegration"].output.classesDirs
    classpath = sourceSets["testIntegration"].runtimeClasspath
    // Run the test task if specified configuration files are changed
    inputs.files(
        fileTree("./") {
            include("settings/**/*.yml")
            include("metadata/**/*")
            include("src/testIntergation/resources/datatests/**/*")
        }
    )
    outputs.upToDateWhen {
        // Call gradle with the -Pforcetest option will force the unit tests to run
        if (project.hasProperty("forcetest")) {
            println("Rerunning unit tests...")
            false
        } else {
            true
        }
    }
}

tasks.check {
    dependsOn("testIntegration")
}

tasks.withType<Test>().configureEach {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}

tasks.processResources {
    // Set the proper build values in the build.properties file
    filesMatching("build.properties") {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val formattedDate = LocalDateTime.now().format(dateFormatter)
        val tokens = mapOf("version" to version, "timestamp" to formattedDate)
        filter(ReplaceTokens::class, mapOf("beginToken" to "@", "endToken" to "@", "tokens" to tokens))
    }
}

tasks.jar {
    manifest {
        /* We put the CLI main class in the manifest at this step as a convenience to allow this jar to be
        run by the ./prime script. It will be overwritten by the Azure host or the CLI fat jar package. */
        attributes("Main-Class" to primeMainClass)
        attributes("Multi-Release" to true)
    }
}

tasks.shadowJar {
    // our fat jar is getting fat! Or over 65K files in this case
    isZip64 = true
}

// Just a nicer name to create the fat jar
tasks.register("fatJar") {
    group = rootProject.description ?: ""
    description = "Generate the fat jar used to run the prime tool"
    dependsOn("shadowJar")
}

tasks.ktlintCheck {
    // DB tasks are not needed by ktlint, but gradle adds them by automatic configuration
    tasks["generateJooq"].enabled = false
    tasks["migrate"].enabled = false
    tasks["flywayMigrate"].enabled = false
}

/**
 * PRIME CLI tasks
 */
tasks.register<JavaExec>("primeCLI") {
    group = rootProject.description ?: ""
    description = "Run the Prime CLI tool.  Specify arguments with --args='<args>'"
    mainClass.set(primeMainClass)
    classpath = sourceSets["main"].runtimeClasspath
    standardInput = System.`in`

    // Default arguments is to display the help
    environment["POSTGRES_URL"] = dbUrl
    environment["POSTGRES_USER"] = dbUser
    environment["POSTGRES_PASSWORD"] = dbPassword
    environment[KEY_PRIME_RS_API_ENDPOINT_HOST] = reportsApiEndpointHost
    addVaultValuesToEnv(environment)

    // Use arguments passed by another task in the project.extra["cliArgs"] property.
    doFirst {
        if (project.extra.has("cliArgs")) {
            args = project.extra["cliArgs"] as MutableList<String>
        } else if (args.isNullOrEmpty()) {
            args = listOf("-h")
            println("primeCLI Gradle task usage: gradle primeCLI --args='<args>'")
            println(
                "Usage example: gradle primeCLI --args=\"data --input-fake 50 " +
                    "--input-schema waters/waters-covid-19 --output-dir ./ --target-states CA " +
                    "--target-counties 'Santa Clara' --output-format CSV\""
            )
        }
    }
}

tasks.register("testSmoke") {
    group = rootProject.description ?: ""
    description = "Run the smoke tests"
    project.extra["cliArgs"] = listOf("test")
    finalizedBy("primeCLI")
}

tasks.register("testEnd2End") {
    group = rootProject.description ?: ""
    description = "Run the end to end tests.  Requires running a Docker instance"
    project.extra["cliArgs"] = listOf("test", "--run", "end2end")
    finalizedBy("primeCLI")
}

tasks.register("generateDocs") {
    group = rootProject.description ?: ""
    description = "Generate the schema documentation in markup format"
    project.extra["cliArgs"] = listOf("generate-docs")
    finalizedBy("primeCLI")
}

tasks.register("reloadSettings") {
    group = rootProject.description ?: ""
    description = "Reload the settings database table"
    project.extra["cliArgs"] = listOf("multiple-settings", "set", "-i", "./settings/organizations.yml")
    finalizedBy("primeCLI")
}

tasks.register("reloadTables") {
    group = rootProject.description ?: ""
    description = "Load the latest test lookup tables to the database"
    project.extra["cliArgs"] = listOf("lookuptables", "loadall")
    finalizedBy("primeCLI")
}

/**
 * Packaging and running related tasks
 */
tasks.azureFunctionsPackage {
    dependsOn("test")
}

val azureResourcesTmpDir = File(rootProject.buildDir.path, "$azureFunctionsDir-resources/$azureAppName")
val azureResourcesFinalDir = File(rootProject.buildDir.path, "$azureFunctionsDir/$azureAppName")
tasks.register<Copy>("gatherAzureResources") {
    from("./")
    into(azureResourcesTmpDir)
    include("metadata/**/*.yml")
    include("metadata/**/*.schema")
    include("metadata/**/*.valuesets")
    include("metadata/**/*.csv")
    include("settings/**/*.yml")
    include("assets/**/*__inline.html")
}

tasks.register("copyAzureResources") {
    dependsOn("gatherAzureResources")
    doLast {
        // We need to use a regular copy, so Gradle does not delete the existing folder
        org.apache.commons.io.FileUtils.copyDirectory(azureResourcesTmpDir, azureResourcesFinalDir)
    }
}

val azureScriptsTmpDir = File(rootProject.buildDir.path, "$azureFunctionsDir-scripts/$azureAppName")
val azureScriptsFinalDir = rootProject.buildDir
val primeScriptName = "prime"
val startFuncScriptName = "start_func.sh"
tasks.register<Copy>("gatherAzureScripts") {
    from("./")
    into(azureScriptsTmpDir)
    include(primeScriptName)
    include(startFuncScriptName)
}

tasks.register("copyAzureScripts") {
    dependsOn("gatherAzureScripts")
    doLast {
        // We need to use a regular copy, so Gradle does not delete the existing folder
        org.apache.commons.io.FileUtils.copyDirectory(azureScriptsTmpDir, azureScriptsFinalDir)
        File(azureScriptsFinalDir.path, primeScriptName).setExecutable(true)
        File(azureScriptsFinalDir.path, startFuncScriptName).setExecutable(true)
    }
}

tasks.azureFunctionsPackage {
    finalizedBy("copyAzureResources")
    finalizedBy("copyAzureScripts")
}

tasks.register("package") {
    group = rootProject.description ?: ""
    description = "Package the code and necessary files to run the Azure functions"
    dependsOn("azureFunctionsPackage")
    dependsOn("fatJar")
}

tasks.register("quickPackage") {
    // Quick package for development purposes.  Use with caution.
    dependsOn("azureFunctionsPackage")
    dependsOn("copyAzureResources")
    dependsOn("copyAzureScripts")
    tasks["test"].enabled = false
    tasks["jacocoTestReport"].enabled = false
    tasks["compileTestKotlin"].enabled = false
    tasks["migrate"].enabled = false
    tasks["flywayMigrate"].enabled = false
}

tasks.azureFunctionsRun {
    // This storage account key is not a secret, just a dummy value.
    val devAzureConnectString =
        "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=" +
            "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=" +
            "http://localhost:10000/devstoreaccount1;QueueEndpoint=http://localhost:10001/devstoreaccount1;"

    val env = mutableMapOf<String, Any>(
        "AzureWebJobsStorage" to devAzureConnectString,
        "PartnerStorage" to devAzureConnectString,
        "POSTGRES_USER" to dbUser,
        "POSTGRES_PASSWORD" to dbPassword,
        "POSTGRES_URL" to dbUrl,
        "PRIME_ENVIRONMENT" to "local",
        "VAULT_API_ADDR" to "http://localhost:8200",
        "SFTP_HOST_OVERRIDE" to "localhost",
        "SFTP_PORT_OVERRIDE" to "2222",
        "REDOX_URL_OVERRIDE" to "http://localhost:1080"
    )

    // Load the vault variables
    addVaultValuesToEnv(env)

    environment(env)
    azurefunctions.localDebug = "transport=dt_socket,server=y,suspend=n,address=5005"
}

tasks.register("run") {
    group = rootProject.description ?: ""
    description = "Run the Azure functions locally.  Note this needs the required services running as well"
    dependsOn("azureFunctionsRun")
}

tasks.register("quickRun") {
    dependsOn("azureFunctionsRun")
    tasks["test"].enabled = false
    tasks["jacocoTestReport"].enabled = false
    tasks["compileTestKotlin"].enabled = false
    tasks["migrate"].enabled = false
    tasks["flywayMigrate"].enabled = false
}

/**
 * Database related configuration and tasks
 */
// Configuration for Flyway migration tool
flyway {
    url = dbUrl
    user = dbUser
    password = dbPassword
}

// Database code generation configuration
jooq {
    configurations {
        create("main") { // name of the jOOQ configuration
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.INFO
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = dbUrl
                    user = dbUser
                    password = dbPassword
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        includes = ".*"
                    }
                    generate.apply {
                        isImmutablePojos = false
                        isFluentSetters = true
                        isPojos = true
                        isPojosEqualsAndHashCode = true
                        isPojosToString = true
                        isJavaTimeTypes = true
                    }
                    target.apply {
                        packageName = jooqPackageName
                        directory = jooqSourceDir
                    }
                }
            }
        }
    }
}

// Set jOOQ task to participate in Gradle's incremental build feature
tasks.named<nu.studer.gradle.jooq.JooqGenerate>("generateJooq") {
    dependsOn("migrate")
    allInputsDeclared.set(true)
    inputs.files(project.fileTree("src/main/resources/db/migration")).withPropertyName("migrations")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

/**
 * Convenience tasks
 */
// Convenience tasks
tasks.register("compile") {
    group = rootProject.description ?: ""
    description = "Compile the code"
    dependsOn("compileKotlin")
}

tasks.register("migrate") {
    group = rootProject.description ?: ""
    description = "Load the database with the latest schema"
    dependsOn("flywayMigrate")
}

tasks.register("resetDB") {
    group = rootProject.description ?: ""
    description = "Delete all tables in the database and recreate from the latest schema"
    dependsOn("flywayClean")
    dependsOn("flywayMigrate")
}

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://jitpack.io")
    }
}

// Prevent logback from being used by slf4j, no matter who declares it. Otherwise slf4j can pickup logback instead of
// log4j from the classpath and our configuration gets ignored.
configurations {
    implementation {
        exclude(group = "ch.qos.logback")
    }
}

dependencies {
    jooqGenerator("org.postgresql:postgresql:42.3.1")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("com.microsoft.azure.functions:azure-functions-java-library:1.4.2")
    implementation("com.azure:azure-core:1.23.1")
    implementation("com.azure:azure-core-http-netty:1.11.4")
    implementation("com.azure:azure-storage-blob:12.14.1") {
        exclude(group = "com.azure", module = "azure-core")
    }
    implementation("com.azure:azure-storage-queue:12.11.2") {
        exclude(group = "com.azure", module = "azure-core")
    }
    implementation("com.azure:azure-security-keyvault-secrets:4.3.5") {
        exclude(group = "com.azure", module = "azure-core")
        exclude(group = "com.azure", module = "azure-core-http-netty")
    }
    implementation("com.azure:azure-identity:1.4.2") {
        exclude(group = "com.azure", module = "azure-core")
        exclude(group = "com.azure", module = "azure-core-http-netty")
    }
    implementation("org.apache.logging.log4j:log4j-api:[2.17.1,)")
    implementation("org.apache.logging.log4j:log4j-core:[2.17.1,)")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:[2.17.1,)")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.1.0")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.2.0")
    implementation("tech.tablesaw:tablesaw-core:0.42.0")
    implementation("com.github.ajalt.clikt:clikt-jvm:3.3.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.0")
    implementation("com.github.javafaker:javafaker:1.0.2")
    implementation("ca.uhn.hapi:hapi-base:2.3")
    implementation("ca.uhn.hapi:hapi-structures-v251:2.3")
    implementation("com.googlecode.libphonenumber:libphonenumber:8.12.39")
    implementation("org.thymeleaf:thymeleaf:3.0.14.RELEASE")
    implementation("com.sendgrid:sendgrid-java:4.8.1")
    implementation("com.okta.jwt:okta-jwt-verifier:0.5.1")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1") {
        exclude(group = "org.json", module = "json")
    }
    implementation("com.github.kittinunf.fuel:fuel-json:2.3.1")
    implementation("org.json:json:20211205")
    // DO NOT INCREMENT SSHJ to a newer version without first thoroughly testing it locally.
    implementation("com.hierynomus:sshj:0.31.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("com.jcraft:jsch:0.1.55")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("commons-codec:commons-codec:1.15")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.postgresql:postgresql:42.3.0")
    implementation("com.zaxxer:HikariCP:5.0.0")
    implementation("org.flywaydb:flyway-core:8.3.0")
    implementation("org.commonmark:commonmark:0.18.1")
    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("com.helger.as2:as2-lib:4.9.1")
    // Prevent mixed versions of these libs based on different versions being included by different packages
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("org.bouncycastle:bcmail-jdk15on:1.70")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    implementation("commons-net:commons-net:3.8.0")
    implementation("com.cronutils:cron-utils:9.1.5")
    implementation("khttp:khttp:1.0.0")
    implementation("com.auth0:java-jwt:3.18.2")
    implementation("io.jsonwebtoken:jjwt-api:0.11.2")
    implementation("de.m3y.kformat:kformat:0.9")
    implementation("io.github.java-diff-utils:java-diff-utils:4.11")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("it.skrape:skrapeit-html-parser:1.1.6")
    implementation("it.skrape:skrapeit-http-fetcher:1.1.6")

    runtimeOnly("com.okta.jwt:okta-jwt-verifier-impl:0.5.1")
    runtimeOnly("com.github.kittinunf.fuel:fuel-jackson:2.3.1")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.2")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.2")

    testImplementation(kotlin("test-junit5"))
    testImplementation("com.github.KennethWussmann:mock-fuel:1.3.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "com.github.kittinunf.fuel", module = "fuel")
    }
    // kotlinx-coroutines-core is needed by mock-fuel
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    testImplementation("com.github.KennethWussmann:mock-fuel:1.3.0")
    testImplementation("io.mockk:mockk:1.12.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}