import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

group = "gov.cdc.prime.reportstream"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

val majorJavaVersion = 17
java {
    sourceCompatibility = JavaVersion.toVersion(majorJavaVersion)
    targetCompatibility = JavaVersion.toVersion(majorJavaVersion)
    toolchain {
        languageVersion = JavaLanguageVersion.of(majorJavaVersion)
    }
}
val compileKotlin: KotlinCompile by tasks
val compileTestKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "$majorJavaVersion"
compileKotlin.kotlinOptions.allWarningsAsErrors = true
compileTestKotlin.kotlinOptions.jvmTarget = "$majorJavaVersion"
compileTestKotlin.kotlinOptions.allWarningsAsErrors = true

configure<KtlintExtension> {
    // See ktlint versions at https://github.com/pinterest/ktlint/releases
    version.set("1.1.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val jacksonVersion = "2.17.1"
dependencies {

    // Common dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.azure:azure-core:1.49.0")
    implementation("com.azure:azure-core-http-netty:1.15.0")
    implementation("com.azure:azure-data-tables:12.2.0")
    implementation("com.azure:azure-storage-queue:12.21.0") {
        exclude(group = "com.azure", module = "azure-core")
    }
    implementation("com.azure:azure-storage-blob:12.26.0") {
        exclude(group = "com.azure", module = "azure-core")
    }
    implementation("com.microsoft.azure:applicationinsights-core:3.5.3")

    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")
    implementation("org.apache.logging.log4j:log4j-layout-template-json:2.23.1")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.4.0")
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.9")

    // Common test dependencies
    testImplementation(kotlin("test-junit5"))
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
    testImplementation("org.testcontainers:postgresql:1.19.8")
}