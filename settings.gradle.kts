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

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            //version("org.springframework.boot", "3.3.3")
            plugin("springBoot", "org.springframework.boot").version("3.3.3")
//            library("groovy-core", "org.codehaus.groovy:groovy:3.0.5")
//            library("groovy-json", "org.codehaus.groovy:groovy-json:3.0.5")
//            library("groovy-nio", "org.codehaus.groovy:groovy-nio:3.0.5")
//            library("commons-lang3", "org.apache.commons", "commons-lang3").version {
//                strictly("[3.8, 4.0[")
//                prefer("3.9")
//            }
        }
    }
}

rootProject.name = "prime-reportstream"
include("shared", "submissions", "prime-router")
