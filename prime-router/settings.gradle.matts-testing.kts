pluginManagement {
    repositories {
        maven { url = java.net.URI("https://repo.spring.io/snapshot") }
        gradlePluginPortal()
    }
}

sourceControl {
    gitRepository(java.net.URI("https://github.com/CDCgov/hl7v2-fhir-converter.git")) {
        producesModule("io.github.linuxforhealth:hl7v2-fhir-converter")
    }
}
// Single-project build for rs-prime-router
rootProject.name = "rs-prime-router"
rootProject.buildFileName = "build.gradle.matts-testing.kts"
