/*
Build script for Prime Router.

Properties that can be overridden using the Gradle -P arguments:
  DB_USER - Postgres database username (defaults to prime)
  DB_PASSWORD - Postgres database password (defaults to changeIT!)
  DB_URL - Postgres database URL (defaults to jdbc:postgresql://localhost:5432/prime_data_hub)

  E.g. ./gradlew clean package -Pg.user=myuser -Dpg.password=mypassword
 */

import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    kotlin("jvm") version "1.5.0"
    id("org.flywaydb.flyway") version "7.8.1"
    id("nu.studer.jooq") version "5.2"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("com.microsoft.azure.azurefunctions") version "1.5.1"
}

group = "gov.cdc.prime"
version = "0.1-SNAPSHOT"
description = "prime-router"
val azureAppName = "prime-data-hub-router"
val azureFunctionsDir = "azure-functions"
val primeMainClass = "gov.cdc.prime.router.cli.MainKt"

// Local database information
val dbUser = (project.properties["DB_USER"] ?: "prime") as String
val dbPassword = (project.properties["DB_PASSWORD"] ?: "changeIT!") as String
val dbUrl = (project.properties["DB_URL"] ?: "jdbc:postgresql://localhost:5432/prime_data_hub") as String
val jooqSourceDir = "build/generated-src/jooq/src/main/java"
val jooqPackageName = "gov.cdc.prime.router.azure.db"

val kotlinVersion = "1.5.0"

defaultTasks("package")

// Set the compiler JVM target
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val compileKotlin: KotlinCompile by tasks
val compileTestKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "11"
compileTestKotlin.kotlinOptions.jvmTarget = "11"

tasks.clean {
    // Delete the old Maven build folder
    delete("target")
}

tasks.test {
    // Use JUnit 5 for running tests
    useJUnitPlatform()
    dependsOn("compileKotlin")
    // Run the test task if specified configuration files are changed
    inputs.files(fileTree("./") {
        include("settings/**/*.yml")
        include("metadata/**/*")
    })
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

// Just a nicer name to create the fat jar
tasks.register("fatJar") {
    group = rootProject.description ?: ""
    description = "Generate the fat jar used to run the prime tool"
    dependsOn("shadowJar")
}

tasks.register<JavaExec>("primeCLI") {
    group = rootProject.description ?: ""
    description = "Run the Prime CLI tool.  Specify arguments with --args='<args>'"
    main = primeMainClass
    classpath = sourceSets["main"].runtimeClasspath
    // Default arguments is to display the help
    args = listOf("-h")
    environment = mapOf("POSTGRES_URL" to dbUrl, "POSTGRES_USER" to dbUser, "POSTGRES_PASSWORD" to dbPassword)
    doFirst {
        println("primeCLI Gradle task usage: gradle primeCLI --args='<args>'")
        println(
            "Usage example: gradle primeCLI --args=\"data --input-fake 50 " +
                "--input-schema waters/waters-covid-19 --output-dir ./ --target-states CA " +
                "--target-counties 'Santa Clara' --output-format CSV\""
        )
    }
}

tasks.register<JavaExec>("testEnd2End") {
    group = rootProject.description ?: ""
    description = "Run the end to end tests.  Requires running a Docker instance"
    main = primeMainClass
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("test", "--run", "end2end")
    environment = mapOf("POSTGRES_URL" to dbUrl, "POSTGRES_USER" to dbUser, "POSTGRES_PASSWORD" to dbPassword)
}

tasks.register<JavaExec>("generateDocs") {
    group = rootProject.description ?: ""
    description = "Generate the schema documentation in markup format"
    main = primeMainClass
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("generate-docs")
}

azurefunctions {
    appName = azureAppName
    setAppSettings(
        closureOf<MutableMap<String, String>> {
            this["WEBSITE_RUN_FROM_PACKAGE"] = "1"
            this["FUNCTIONS_EXTENSION_VERSION"] = "3"
            this["FUNCTIONS_WORKER_RUNTIME"] = "java"
        }
    )
}

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
}

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

tasks.register("package") {
    group = rootProject.description ?: ""
    description = "Package the code and necessary files to run the Azure functions"
    dependsOn("azureFunctionsPackage")
    dependsOn("copyAzureResources")
    dependsOn("copyAzureScripts")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    jooqGenerator("org.postgresql:postgresql:42.2.20")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.microsoft.azure.functions:azure-functions-java-library:1.4.2")
    implementation("com.azure:azure-core:1.15.0")
    implementation("com.azure:azure-core-http-netty:1.9.1")
    implementation("com.azure:azure-storage-blob:12.10.2") {
        exclude(group = "com.azure", module = "azure-core")
    }
    implementation("com.azure:azure-storage-queue:12.8.0") {
        exclude(group = "com.azure", module = "azure-core")
    }
    implementation("com.azure:azure-security-keyvault-secrets:4.2.7") {
        exclude(group = "com.azure", module = "azure-core")
        exclude(group = "com.azure", module = "azure-core-http-netty")
    }
    implementation("com.azure:azure-identity:1.2.5") {
        exclude(group = "com.azure", module = "azure-core")
        exclude(group = "com.azure", module = "azure-core-http-netty")
    }
    implementation("org.apache.logging.log4j:log4j-api:[2.13.2,)")
    implementation("org.apache.logging.log4j:log4j-core:[2.13.2,)")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:[2.13.2,)")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.0.0")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:0.15.2")
    implementation("tech.tablesaw:tablesaw-core:0.38.2")
    implementation("com.github.ajalt.clikt:clikt-jvm:3.1.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.3")
    implementation("com.github.javafaker:javafaker:1.0.2")
    implementation("ca.uhn.hapi:hapi-base:2.3")
    implementation("ca.uhn.hapi:hapi-structures-v251:2.3")
    implementation("com.googlecode.libphonenumber:libphonenumber:8.12.21")
    implementation("org.thymeleaf:thymeleaf:3.0.12.RELEASE")
    implementation("com.sendgrid:sendgrid-java:4.7.2")
    implementation("com.okta.jwt:okta-jwt-verifier:0.5.1")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1") {
        exclude(group = "org.json", module = "json")
    }
    implementation("com.github.kittinunf.fuel:fuel-json:2.3.1")
    implementation("org.json:json:20210307")
    implementation("com.hierynomus:sshj:0.31.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.68")
    implementation("com.jcraft:jsch:0.1.55")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("commons-codec:commons-codec:1.15")
    implementation("commons-io:commons-io:2.8.0")
    implementation("org.postgresql:postgresql:42.2.20")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.flywaydb:flyway-core:7.8.2")
    implementation("com.github.kayr:fuzzy-csv:1.6.48")
    implementation("org.commonmark:commonmark:0.17.1")

    runtimeOnly("com.okta.jwt:okta-jwt-verifier-impl:0.5.1")
    runtimeOnly("com.github.kittinunf.fuel:fuel-jackson:2.3.1")

    testImplementation(kotlin("test-junit5"))
    testImplementation("com.github.KennethWussmann:mock-fuel:1.3.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "com.github.kittinunf.fuel", module = "fuel")
    }
    // kotlinx-coroutines-core is needed by mock-fuel
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3-native-mt")
    testImplementation("com.github.KennethWussmann:mock-fuel:1.3.0")
    testImplementation("io.mockk:mockk:1.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
}