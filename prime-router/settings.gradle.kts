import java.net.URI

/*
 * This file was generated by the Gradle 'init' task.
 */

rootProject.name = "prime-router"

// Make sure the gradle plugin manager is not using JCenter, which was shutdown
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

sourceControl {
    gitRepository(URI("https://github.com/CDCgov/hl7v2-fhir-converter.git")) {
        producesModule("io.github.linuxforhealth:hl7v2-fhir-converter")
    }
}