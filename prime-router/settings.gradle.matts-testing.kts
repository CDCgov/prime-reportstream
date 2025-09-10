@file:Suppress("UnusedImport")
import java.net.URI

pluginManagement {
    repositories {
        maven { url = uri("https://repo.spring.io/snapshot") }
        gradlePluginPortal()
    }
}

sourceControl {
    gitRepository(uri("https://github.com/CDCgov/hl7v2-fhir-converter.git")) {
        producesModule("io.github.linuxforhealth:hl7v2-fhir-converter")
    }
}
// Single-project build for rs-prime-router
rootProject.name = "rs-prime-router"
rootProject.buildFileName = "build.gradle.matts-testing.kts"

