apply(from = rootProject.file("buildSrc/shared.gradle.kts"))

plugins {
    id("reportstream.project-conventions")
}

group = "gov.cdc.prime.reportstream"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
//    implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:7.2.2")
//    // https://mvnrepository.com/artifact/ca.uhn.hapi.fhir/hapi-fhir-caching-caffeine
//    implementation("ca.uhn.hapi.fhir:hapi-fhir-caching-caffeine:7.2.2")
//    implementation("ca.uhn.hapi.fhir:hapi-fhir-client:7.2.2")
    implementation(libs.hapi.fhir.structures.r4)
    implementation(libs.hapi.fhir.caching.caffeine)
    implementation(libs.hapi.fhir.client)

    // pin
//    implementation("ca.uhn.hapi.fhir:org.hl7.fhir.utilities:6.3.24")
//    implementation("ca.uhn.hapi.fhir:org.hl7.fhir.r4:6.3.24")
    implementation(libs.org.hl7.fhir.utilities)
    implementation(libs.org.hl7.fhir.r4)

//    implementation("ca.uhn.hapi:hapi-base:2.5.1")
//    implementation("ca.uhn.hapi:hapi-structures-v251:2.5.1")
//    implementation("ca.uhn.hapi:hapi-structures-v27:2.5.1")
    implementation(libs.hapi.base)
    implementation(libs.hapi.structures.v251)
    implementation(libs.hapi.structures.v27)

//    implementation("gov.nist:hl7-v2-validation:1.6.5") {
//        // These conflict with the javax.xml.transform package available in the base JDK and need to be excluded
//        exclude("xerces")
//        exclude("xml-apis")
//    }

    implementation(libs.hl7.v2.validation) {
        // These conflict with the javax.xml.transform package available in the base JDK and need to be excluded
        exclude("xerces")
        exclude("xml-apis")
    }

//    implementation("xalan:xalan:2.7.3")

    // pin xalan for CVE-2022-34169 via gov.nist:hl7-v2-validation@1.6.4
    implementation(libs.xalan)

//    implementation("org.apache.commons:commons-lang3:3.15.0")
    implementation(libs.commons.lang3)

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
    testImplementation("org.apache.commons:commons-compress:1.26.2")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}