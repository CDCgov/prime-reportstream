apply(from = rootProject.file("buildSrc/shared.gradle.kts"))

plugins {
    id("org.springframework.boot") version "3.2.7-SNAPSHOT"
    id("io.spring.dependency-management") version "1.1.5"
    id("reportstream.project-conventions")
    kotlin("plugin.spring") version "1.9.24"
}

group = "gov.cdc.prime"
version = "0.0.1-SNAPSHOT"

extra["springCloudAzureVersion"] = "5.13.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.azure:azure-messaging-eventgrid:4.5.0")
    implementation("com.azure.spring:spring-cloud-azure-starter-storage")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "ch.qos.logback", module = "logback-classic")
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    }
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("com.azure:azure-messaging-eventgrid:4.5.0")
    testImplementation("com.azure:azure-storage-blob:12.14.0")
    testImplementation("com.azure:azure-storage-queue:12.11.2")
    testImplementation("com.azure:azure-data-tables:12.2.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")
    implementation(project(":shared"))
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