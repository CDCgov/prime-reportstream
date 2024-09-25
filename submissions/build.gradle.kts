apply(from = rootProject.file("buildSrc/shared.gradle.kts"))

plugins {
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    id("reportstream.project-conventions")
    kotlin("plugin.spring") version "2.0.20"
}

group = "gov.cdc.prime"
version = "0.0.1-SNAPSHOT"

extra["springCloudAzureVersion"] = "5.14.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose:6.3.3")

    implementation("com.azure.spring:spring-cloud-azure-starter-storage")
    implementation("com.microsoft.azure:applicationinsights-runtime-attach:3.5.4")
    implementation("com.microsoft.azure:applicationinsights-web:3.5.4")
    implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.xmlunit:xmlunit-core:2.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.apache.commons:commons-compress:1.27.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")
    implementation(project(":shared"))
}

// There is a conflict in logging implementations. Excluded these in favor of using log4j-slf4j2-impl
configurations.all {
    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    exclude(group = "ch.qos.logback")
}

dependencyManagement {
    imports {
        mavenBom("com.azure.spring:spring-cloud-azure-dependencies:${property("springCloudAzureVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}