import java.net.URI

pluginManagement {
    repositories {
        maven { url = uri("https://repo.spring.io/snapshot") }
        gradlePluginPortal()
    }
}

sourceControl {
    gitRepository(URI("https://github.com/CDCgov/hl7v2-fhir-converter.git")) {
        producesModule("io.github.linuxforhealth:hl7v2-fhir-converter")
    }
}
rootProject.name = "prime-reportstream"
include("shared", "submissions", "prime-router", "auth")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("hapi-structures_caffeine_client", "7.2.2")
            version("hapi-utilities_fhir-r4", "6.3.24")
            version("hapi-base_structures_v", "2.5.1")

            library("hapi.fhir.structures.r4", "ca.uhn.hapi.fhir", "hapi-fhir-structures-r4").versionRef("hapi-structures_caffeine_client")
            library("hapi.fhir.caching.caffeine", "ca.uhn.hapi.fhir", "hapi-fhir-caching-caffeine").versionRef("hapi-structures_caffeine_client")
            library("hapi.fhir.client", "ca.uhn.hapi.fhir", "hapi-fhir-client").versionRef("hapi-structures_caffeine_client")

            library("org.hl7.fhir.utilities", "ca.uhn.hapi.fhir", "org.hl7.fhir.utilities").versionRef("hapi-utilities_fhir-r4")
            library("org.hl7.fhir.r4", "ca.uhn.hapi.fhir", "org.hl7.fhir.r4").versionRef("hapi-utilities_fhir-r4")

            library("hapi.base", "ca.uhn.hapi", "hapi-base").versionRef("hapi-base_structures_v")
            library("hapi.structures.v251", "ca.uhn.hapi", "hapi-structures-v251").versionRef("hapi-base_structures_v")
            library("hapi.structures.v27", "ca.uhn.hapi", "hapi-structures-v27").versionRef("hapi-base_structures_v")

            // pin xalan for CVE-2022-34169 via gov.nist:hl7-v2-validation@1.6.4
            library("xalan", "xalan", "xalan").version("2.7.3")

            library("hl7.v2.validation", "gov.nist", "hl7-v2-validation").version("1.6.5")

            library("commons.lang3", "org.apache.commons", "commons-lang3").version("3.15.0")
        }
    }
}


