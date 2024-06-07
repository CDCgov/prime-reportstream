import java.net.URI

pluginManagement {
    repositories {
        maven { url = uri("https://repo.spring.io/snapshot") }
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

sourceControl {
    gitRepository(URI("https://github.com/CDCgov/hl7v2-fhir-converter.git")) {
        producesModule("io.github.linuxforhealth:hl7v2-fhir-converter")
    }
}
rootProject.name = "prime-reportstream"
include("shared", "submissions", "prime-router")
