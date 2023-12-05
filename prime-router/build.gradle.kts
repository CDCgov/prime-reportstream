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

import io.swagger.v3.plugins.gradle.tasks.ResolveTask
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jooq.meta.jaxb.ForcedType
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    val kotlinVersion by System.getProperties()
    kotlin("jvm") version "$kotlinVersion"
    id("org.flywaydb.flyway") version "9.22.3"
    id("nu.studer.jooq") version "8.2.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.microsoft.azure.azurefunctions") version "1.14.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("com.adarshr.test-logger") version "4.0.0"
    id("jacoco")
    id("org.jetbrains.dokka") version "1.9.10"
    id("com.avast.gradle.docker-compose") version "0.17.5"
    id("org.jetbrains.kotlin.plugin.serialization") version "$kotlinVersion"
    id("com.nocwriter.runsql") version ("1.0.3")
    id("io.swagger.core.v3.swagger-gradle-plugin") version "2.2.19"
}

group = "gov.cdc.prime"
version = "0.2-SNAPSHOT"
description = "prime-router"
val azureAppName = "prime-data-hub-router"
val azureFunctionsDir = "azure-functions"
val primeMainClass = "gov.cdc.prime.router.cli.MainKt"
val defaultDuplicateStrategy = DuplicatesStrategy.WARN
azurefunctions.appName = azureAppName
val appJvmTarget = "17"
val javaVersion = when (appJvmTarget) {
    "17" -> JavaVersion.VERSION_17
    "19" -> JavaVersion.VERSION_19
    "21" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}
val ktorVersion = "2.3.6"
val kotlinVersion by System.getProperties()
val jacksonVersion = "2.15.3"
jacoco.toolVersion = "0.8.10"

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

val buildDir = project.layout.buildDirectory.asFile.get()

/**
 * Add the `VAULT_TOKEN` in the local vault to the [env] map
 */
fun addVaultValuesToEnv(env: MutableMap<String, Any>) {
    val vaultFile = File(project.projectDir, ".vault/env/.env.local")
    if (!vaultFile.exists()) {
        vaultFile.createNewFile()
        throw GradleException("Your vault configuration has not been initialized. Start/Restart your vault container.")
    }
    val prop = Properties()
    FileInputStream(vaultFile).use { prop.load(it) }
    prop.forEach { key, value -> env[key.toString()] = value.toString().replace("\"", "") }
    if (!env.contains("CREDENTIAL_STORAGE_METHOD") || env["CREDENTIAL_STORAGE_METHOD"] != "HASHICORP_VAULT") {
        throw GradleException("Your vault configuration is incorrect.  Check your ${vaultFile.absolutePath} file.")
    }
}

defaultTasks("package")

// Set the compiler JVM target
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

val compileKotlin: KotlinCompile by tasks
val compileTestKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = appJvmTarget
compileKotlin.kotlinOptions.allWarningsAsErrors = true
compileTestKotlin.kotlinOptions.jvmTarget = appJvmTarget
compileTestKotlin.kotlinOptions.allWarningsAsErrors = true

tasks.clean {
    group = rootProject.description ?: ""
    description = "Clean the build artifacts"
    // Delete the old Maven build folder
    dependsOn("composeDownForced")
    delete("target")
    // clean up all the old event files in the SOAP set up
    doLast {
        val eventsDir = File("../.environment/soap_service/soap/event/v1")
        if (eventsDir.exists() && eventsDir.isDirectory && (eventsDir.listFiles()?.isNotEmpty() == true)) {
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
    group = rootProject.description ?: ""
    description = "Run the unit tests"
    // Use JUnit 5 for running tests
    useJUnitPlatform()

    // Set the environment to local for the tests
    environment["PRIME_ENVIRONMENT"] = "local"
    environment["POSTGRES_URL"] = dbUrl
    environment["POSTGRES_USER"] = dbUser
    environment["POSTGRES_PASSWORD"] = dbPassword

    // Set max parellel forks as recommended in https://docs.gradle.org/current/userguide/performance.html
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    minHeapSize = "2g"
    maxHeapSize = "3g"
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

tasks.javadoc.configure {
    actions.clear()
    dependsOn(tasks.dokkaHtml)
}

tasks.dokkaHtml.configure {
    val docsDir = File(buildDir, "/docs/dokka")
    outputDirectory.set(docsDir)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    // Jacoco wants the source file directory structure to match the package name like in Java, so
    // move the source files to a temp location with that structure.
    val sourcesDir = File(project.projectDir, "/src/main/kotlin")
    val jacocoSourcesDir = File(buildDir, "/jacoco/sources")
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
compileTestIntegrationKotlin.kotlinOptions.jvmTarget = appJvmTarget

val testIntegrationImplementation: Configuration by configurations.getting {
    extendsFrom(configurations["testImplementation"])
}

configurations["testIntegrationRuntimeOnly"].extendsFrom(configurations["runtimeOnly"])

tasks.register<Test>("testIntegration") {
    group = rootProject.description ?: ""
    description = "Run the integration tests"
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

val apiDocsBaseDir = File(project.projectDir, "/docs/api/")
val apiDocsSpecDir = File(apiDocsBaseDir, "generated")
val apiDocsSwaggerUIDir = File(apiDocsBaseDir, "swagger-ui")
val buildSwaggerUIDir = File(buildDir, "/swagger-ui/")
tasks.register<ResolveTask>("generateOpenApi") {
    group = rootProject.description ?: ""
    description = "Generate OpenAPI spec for Report Stream APIs"
    outputFileName = "api"
    outputFormat = ResolveTask.Format.YAML
    prettyPrint = true
    classpath = sourceSets["main"].runtimeClasspath
    buildClasspath = classpath
    resourcePackages = setOf("gov.cdc.prime.router.azure")
    outputDir = apiDocsSpecDir
    dependsOn("compileKotlin")
}

tasks.register<Copy>("copyApiSwaggerUI") {
    // copy generated OpenAPI spec files
    // to folder /build/swagger-ui, in azure functions docker,
    // the api docs and swagger ui resources are upload to azure
    // blob container 'apidocs' - where the api docs is hosted.
    from(apiDocsSpecDir) {
        include("*.yaml")
    }
    from(apiDocsSwaggerUIDir)
    into(buildSwaggerUIDir)
    dependsOn("generateOpenApi")
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
    // getting an error about writing the same value out to the jar, so forcing it be a warning for now
    duplicatesStrategy = defaultDuplicateStrategy
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

configure<KtlintExtension> {
    // See ktlint versions at https://github.com/pinterest/ktlint/releases
    version.set("1.0.0")
}
tasks.ktlintCheck {
    // DB tasks are not needed by ktlint, but gradle adds them by automatic configuration
    tasks["generateJooq"].enabled = false
    tasks["migrate"].enabled = false
    tasks["flywayMigrate"].enabled = false
}
tasks.ktlintFormat {
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
        if (project.extra.has("cliArgs") && project.extra["cliArgs"] is List<*>) {
            args = (project.extra["cliArgs"] as List<*>).filterIsInstance(String::class.java)
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
    project.extra["cliArgs"] = listOf("multiple-settings", "set", "-i", "./settings/organizations.yml", "-s")
    finalizedBy("primeCLI")
}

tasks.register("reloadTables") {
    group = rootProject.description ?: ""
    description = "Load the latest test lookup tables to the database"
    project.extra["cliArgs"] = listOf("lookuptables", "loadall")
    finalizedBy("primeCLI")
}

tasks.register("reloadCredentials") {
    dependsOn("composeUp")
    group = rootProject.description ?: ""
    description = "Load the SFTP credentials used for local testing to the vault"
    project.extra["cliArgs"] = listOf(
        "create-credential",
        "--type=UserPass",
        "--persist=DEFAULT-SFTP",
        "--user",
        "foo",
        "--pass",
        "pass"
    )
    finalizedBy("primeCLI")
}

/**
 * Packaging and running related tasks
 */
tasks.azureFunctionsPackage {
    dependsOn("test")
}

val azureResourcesTmpDir = File(buildDir, "$azureFunctionsDir-resources/$azureAppName")
val azureResourcesFinalDir = File(buildDir, "$azureFunctionsDir/$azureAppName")
tasks.register<Copy>("gatherAzureResources") {
    from("./")
    into(azureResourcesTmpDir)
    include("metadata/**/*.yml")
    include("metadata/**/*.properties")
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
        FileUtils.copyDirectory(azureResourcesTmpDir, azureResourcesFinalDir)
    }
}

val azureScriptsTmpDir = File(buildDir, "$azureFunctionsDir-scripts/$azureAppName")
val azureScriptsFinalDir = rootProject.layout.buildDirectory.asFile.get()
val primeScriptName = "prime"
val startFuncScriptName = "start_func.sh"
val apiDocsSetupScriptName = "upload_swaggerui.sh"
tasks.register<Copy>("gatherAzureScripts") {
    from("./")
    into(azureScriptsTmpDir)
    include(primeScriptName)
    include(startFuncScriptName)
    include(apiDocsSetupScriptName)
}

tasks.register("copyAzureScripts") {
    dependsOn("gatherAzureScripts")
    doLast {
        // We need to use a regular copy, so Gradle does not delete the existing folder
        FileUtils.copyDirectory(azureScriptsTmpDir, azureScriptsFinalDir)
        File(azureScriptsFinalDir.path, primeScriptName).setExecutable(true)
        File(azureScriptsFinalDir.path, startFuncScriptName).setExecutable(true)
        File(azureScriptsFinalDir.path, apiDocsSetupScriptName).setExecutable(true)
    }
}

tasks.azureFunctionsPackage {
    finalizedBy("copyAzureResources")
    finalizedBy("copyAzureScripts")
}

tasks.register("package") {
    group = rootProject.description ?: ""
    description = "Package the code and necessary files to run the Azure functions"
    // copy the api docs swagger ui to the build location
    dependsOn("copyApiSwaggerUI")
    dependsOn("azureFunctionsPackage")
    dependsOn("fatJar").mustRunAfter("azureFunctionsPackage")
}

tasks.register("quickPackage") {
    group = rootProject.description ?: ""
    description = "Package the code and necessary files to run the Azure functions skipping unit tests and migration"
    // copy the api docs swagger ui to the build location
    dependsOn("copyApiSwaggerUI")
    // Quick package for development purposes.  Use with caution.
    dependsOn("azureFunctionsPackage")
    tasks["test"].enabled = false
    tasks["jacocoTestReport"].enabled = false
    tasks["compileTestKotlin"].enabled = false
    tasks["migrate"].enabled = false
    tasks["flywayMigrate"].enabled = false
    tasks["dokkaHtml"].enabled = false
}

/**
 * Docker services needed for running Dockerless
 */
dockerCompose {
//    projectName = "prime-router" // docker-composer has this setter broken as of 0.16.4
    setProjectName("prime-router") // this is a workaround for the broken setter for projectName
    useComposeFiles.addAll("docker-compose.yml")
    startedServices.addAll("sftp", "soap-webservice", "rest-webservice", "vault", "azurite")
    stopContainers.set(false)
    waitForTcpPorts.set(false)
    // Starting in version 0.17 the plugin changed the default to true, meaning our docker compose yaml files
    // get run with `docker compose` rather than `docker-compose`
    useDockerComposeV2.set(false)
}

tasks.azureFunctionsRun {
    dependsOn("composeUp")
    dependsOn("uploadSwaggerUI").mustRunAfter("composeUp")

    // This storage account key is not a secret, just a dummy value.
    val devAzureConnectString =
        "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=" +
            "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=" +
            "http://localhost:10000/devstoreaccount1;QueueEndpoint=http://localhost:10001/devstoreaccount1;"

    val env = mutableMapOf<String, Any>(
        "AzureWebJobsStorage" to devAzureConnectString,
        "AzureBlobDownloadRetryCount" to 5,
        "PartnerStorage" to devAzureConnectString,
        "POSTGRES_USER" to dbUser,
        "POSTGRES_PASSWORD" to dbPassword,
        "POSTGRES_URL" to dbUrl,
        "PRIME_ENVIRONMENT" to "local",
        "VAULT_API_ADDR" to "http://localhost:8200",
        "SFTP_HOST_OVERRIDE" to "localhost",
        "SFTP_PORT_OVERRIDE" to "2222",
        "RS_OKTA_baseUrl" to "reportstream.oktapreview.com"
    )

    // Load the vault variables
    addVaultValuesToEnv(env)

    environment(env)
    azurefunctions.localDebug = "transport=dt_socket,server=y,suspend=n,address=5005"
}

task<Exec>("uploadSwaggerUI") {
    dependsOn("copyApiSwaggerUI")
    group = rootProject.description ?: ""
    description = "Upload swagger ui and API docs to azure storage blob container"
    commandLine("./upload_swaggerui.sh")
}

tasks.register("killFunc") {
    exec {
        workingDir = project.rootDir
        val processName = "func"
        commandLine = listOf("sh", "-c", "pkill -9 $processName || true")
    }
}

tasks.register("run") {
    group = rootProject.description ?: ""
    description = "Run the Azure functions locally.  Note this needs the required services running as well"
    dependsOn("killFunc", "azureFunctionsRun")
}

tasks.register("quickRun") {
    group = rootProject.description ?: ""
    description = "Run the Azure functions locally skipping tests and migration"
    dependsOn("killFunc", "azureFunctionsRun")
    tasks["test"].enabled = false
    tasks["jacocoTestReport"].enabled = false
    tasks["compileTestKotlin"].enabled = false
    tasks["migrate"].enabled = false
    tasks["flywayMigrate"].enabled = false
    tasks["dokkaHtml"].enabled = false
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
    version.set("3.18.6")
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
                        forcedTypes.addAll(
                            arrayOf(
                                ForcedType()
                                    // Specify the Java type of your custom type. This corresponds to the Binding's <U> type.
                                    .withUserType("gov.cdc.prime.router.ActionLogDetail")
                                    // Associate that custom type with your binding.
                                    .withBinding("gov.cdc.prime.router.ActionLogDetailBinding")
                                    // A Java regex matching fully-qualified columns, attributes, parameters. Use the pipe to separate several expressions.
                                    // If provided, both "includeExpressions" and "includeTypes" must match.
                                    .withIncludeExpression("action_log.detail")
                                    .withIncludeTypes("JSONB"),
                                ForcedType()
                                    // Specify the Java type of your custom type. This corresponds to the Binding's <U> type.
                                    .withUserType("gov.cdc.prime.router.Topic")
                                    // Associate that custom type with your binding.
                                    .withBinding("gov.cdc.prime.router.TopicBinding")
                                    // A Java regex matching fully-qualified columns, attributes, parameters. Use the pipe to separate several expressions.
                                    // If provided, both "includeExpressions" and "includeTypes" must match.
                                    .withIncludeExpression("report_file.schema_topic")
                                    .withIncludeTypes("VARCHAR")
                            )
                        )
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
    flyway.cleanDisabled = false
    dependsOn("flywayClean")
    dependsOn("flywayMigrate")
}

task<RunSQL>("clearDB") {
    group = rootProject.description ?: ""
    description = "Truncate/empty all tables in the database that hold report and related data, and leave settings"
    config {
        username = dbUser
        password = dbPassword
        url = dbUrl
        driverClassName = "org.postgresql.Driver"
        script = """
            TRUNCATE TABLE public.action CASCADE;
            TRUNCATE TABLE public.action_log CASCADE;
            TRUNCATE TABLE public.covid_result_metadata CASCADE;
            TRUNCATE TABLE public.elr_result_metadata CASCADE;
            TRUNCATE TABLE public.item_lineage CASCADE;
            TRUNCATE TABLE public.jti_cache CASCADE;
            TRUNCATE TABLE public.receiver_connection_check_results CASCADE;
            TRUNCATE TABLE public.report_file CASCADE;
            TRUNCATE TABLE public.report_lineage CASCADE;
            TRUNCATE TABLE public.task CASCADE;
        """.trimIndent()
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
        content {
            includeModule("com.github.KennethWussmann", "mock-fuel")
        }
    }
}

buildscript {
    configurations {
        classpath {
            /*
             * Need to exclude this library due to the following dependency chain having an issue with the json-smart
             * library version.
             *   com.microsoft.azure.azurefunctions:com.microsoft.azure.azurefunctions.gradle.plugin:1.8.2 >
             *   com.microsoft.azure:azure-functions-gradle-plugin:1.8.2 >
             *   com.microsoft.azure:azure-toolkit-common-lib:0.12.3 >
             *   com.microsoft.azure:adal4j:1.6.7 > com.nimbusds:oauth2-oidc-sdk:9.15
             * Looks like com.nimbusds:oauth2-oidc-sdk:9.15 has an invalid dependency version of [1.3.2,2.4.2]
             * This will need to be removed once this issue is resolved in Maven.
             */
            exclude("net.minidev", "json-smart")
        }
    }
    dependencies {
        // Now force the gradle build script to get the proper library for com.nimbusds:oauth2-oidc-sdk:9.15.  This
        // will need to be removed once this issue is resolved in Maven.
        classpath("net.minidev:json-smart:2.5.0")
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
    jooqGenerator("org.postgresql:postgresql:42.6.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.microsoft.azure.functions:azure-functions-java-library:3.0.0")
    implementation("com.azure:azure-core:1.45.0")
    implementation("com.azure:azure-core-http-netty:1.13.10")
    // pin io.projectreactor.netty:reactor-netty-http@1.0.39
    implementation("io.projectreactor.netty:reactor-netty-http:1.0.39")
    implementation("com.azure:azure-storage-blob:12.25.0") {
        exclude(group = "com.azure", module = "azure-core")
    }
    implementation("com.azure:azure-storage-queue:12.20.0") {
        exclude(group = "com.azure", module = "azure-core")
    }
    implementation("com.azure:azure-security-keyvault-secrets:4.7.1") {
        exclude(group = "com.azure", module = "azure-core")
        exclude(group = "com.azure", module = "azure-core-http-netty")
    }
    implementation("com.azure:azure-identity:1.11.0") {
        exclude(group = "com.azure", module = "azure-core")
        exclude(group = "com.azure", module = "azure-core-http-netty")
    }
    implementation("org.apache.logging.log4j:log4j-api:[2.17.1,)")
    implementation("org.apache.logging.log4j:log4j-core:[2.17.1,)")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:[2.17.1,)")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.3.0")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.2")
    implementation("tech.tablesaw:tablesaw-core:0.43.1")
    implementation("com.github.ajalt.clikt:clikt-jvm:3.5.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.github.javafaker:javafaker:1.0.2") {
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    implementation("org.yaml:snakeyaml:2.2")
    implementation("io.github.linuxforhealth:hl7v2-fhir-converter") {
        version {
            branch = "master"
        }
    }
    implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:6.10.0")
    // https://mvnrepository.com/artifact/ca.uhn.hapi.fhir/hapi-fhir-caching-caffeine
    implementation("ca.uhn.hapi.fhir:hapi-fhir-caching-caffeine:6.10.0")
    implementation("ca.uhn.hapi:hapi-base:2.5.1")
    implementation("ca.uhn.hapi:hapi-structures-v251:2.5.1")
    implementation("ca.uhn.hapi:hapi-structures-v27:2.5.1")
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.25")
    implementation("org.thymeleaf:thymeleaf:3.1.2.RELEASE")
    implementation("com.sendgrid:sendgrid-java:4.10.1")
    implementation("com.okta.jwt:okta-jwt-verifier:0.5.7")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1") {
        exclude(group = "org.json", module = "json")
    }
    implementation("com.github.kittinunf.fuel:fuel-json:2.3.1")
    implementation("org.json:json:20231013")
    // DO NOT INCREMENT SSHJ to a newer version without first thoroughly testing it locally.
    implementation("com.hierynomus:sshj:0.37.0")
    implementation("com.jcraft:jsch:0.1.55")
    implementation("org.apache.poi:poi:5.2.4")
    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("org.apache.commons:commons-text:1.11.0")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("commons-io:commons-io:2.15.0")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-core:9.22.3")
    implementation("org.commonmark:commonmark:0.21.0")
    implementation("com.google.guava:guava:32.1.3-jre")
    implementation("com.helger.as2:as2-lib:5.1.1")
    implementation("org.bouncycastle:bcprov-jdk15to18:1.76")
    implementation("org.bouncycastle:bcprov-jdk18on:1.76")
    implementation("org.bouncycastle:bcmail-jdk15to18:1.76")

    implementation("commons-net:commons-net:3.10.0")
    implementation("com.cronutils:cron-utils:9.2.1")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    implementation("de.m3y.kformat:kformat:0.10")
    implementation("io.github.java-diff-utils:java-diff-utils:4.11")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("it.skrape:skrapeit-html-parser:1.3.0-alpha.1")
    implementation("it.skrape:skrapeit-http-fetcher:1.3.0-alpha.1")
    implementation("org.apache.poi:poi:5.2.4")
    implementation("org.apache.poi:poi-ooxml:5.2.4")
    implementation("commons-io:commons-io: 2.15.0")
    implementation("com.anyascii:anyascii:0.3.2")
// force jsoup since skrapeit-html-parser@1.2.1+ has not updated
    implementation("org.jsoup:jsoup:1.16.2")
    // https://mvnrepository.com/artifact/io.swagger/swagger-annotations
    implementation("io.swagger:swagger-annotations:1.6.12")
    implementation("io.swagger.core.v3:swagger-jaxrs2:2.2.19")
    // https://mvnrepository.com/artifact/javax.ws.rs/javax.ws.rs-api
    implementation("javax.ws.rs:javax.ws.rs-api:2.1.1")
    // https://mvnrepository.com/artifact/javax.servlet/javax.servlet-api
    implementation("javax.servlet:javax.servlet-api:4.0.1")
    // https://mvnrepository.com/artifact/javax.annotation/javax.annotation-api
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    // TODO: move this to a test dependency when CompareFhirData lives under src/test
    implementation("com.flipkart.zjsonpatch:zjsonpatch:0.4.14")

    runtimeOnly("com.okta.jwt:okta-jwt-verifier-impl:0.5.7")
    // pin com.squareup.okio:okio@3.4.0
    runtimeOnly("com.squareup.okio:okio:3.4.0")
    runtimeOnly("com.github.kittinunf.fuel:fuel-jackson:2.3.1")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    testImplementation(kotlin("test-junit5"))
    testImplementation("com.github.KennethWussmann:mock-fuel:1.3.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "com.github.kittinunf.fuel", module = "fuel")
    }
    // kotlinx-coroutines-core is needed by mock-fuel
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation("com.github.KennethWussmann:mock-fuel:1.3.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.27.0")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.testcontainers:testcontainers:1.19.1")
    testImplementation("org.testcontainers:junit-jupiter:1.19.1")
    testImplementation("org.testcontainers:postgresql:1.19.1")

    implementation(kotlin("script-runtime"))
}