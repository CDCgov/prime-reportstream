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


    // Common test dependencies
    testImplementation(kotlin("test-junit5"))
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
    testImplementation("org.testcontainers:postgresql:1.19.8")
}