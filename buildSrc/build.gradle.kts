plugins {
    `kotlin-dsl`
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"

}

group = "gov.cdc.prime.reportstream"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/snapshot") }
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val kotlinVersion = System.getProperty("kotlinVersion") ?:"1.9.23"
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
    implementation("org.jlleitschuh.gradle.ktlint:org.jlleitschuh.gradle.ktlint.gradle.plugin:12.1.1")
}