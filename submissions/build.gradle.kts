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
    implementation("com.azure.spring:spring-cloud-azure-starter-storage")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
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


