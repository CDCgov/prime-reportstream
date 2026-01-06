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
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("com.nimbusds:nimbus-jose-jwt:10.6")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
    testImplementation("org.apache.commons:commons-compress:1.28.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}