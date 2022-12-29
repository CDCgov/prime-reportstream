plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.7.22"
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven {
        url = uri("https://jitpack.io")
    }
}

group = "gov.cdc.prime"
version = "0.2-SNAPSHOT"

dependencies {
    implementation("com.github.ntrrgc:ts-generator:1.1.2")
    implementation("io.github.classgraph:classgraph:4.8.153")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.14.1")
    api("gov.cdc.prime:tsGenerateLibrary")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
}

tasks.test {
    useJUnitPlatform()
}

ktlint {
    // See ktlint versions at https://github.com/pinterest/ktlint/releases
    version.set("0.43.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

gradlePlugin {
    plugins {
        create("tsGeneratePlugin") {
            id = "gov.cdc.prime.tsGeneratePlugin"
            implementationClass = "gov.cdc.prime.tsGeneratePlugin.TypescriptGeneratorPlugin"
        }
    }
}