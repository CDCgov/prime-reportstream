apply(from = rootProject.file("buildSrc/shared.gradle.kts"))

plugins {
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.6"
    id("reportstream.project-conventions")
    kotlin("plugin.spring") version "2.0.0"
}

group = "gov.cdc.prime"
version = "0.0.1-SNAPSHOT"

val ktorVersion: String by project.properties
val springCloudVersion: String by project.properties
val springCloudAzureVersion: String by project.properties

dependencies {
    implementation(project(":shared"))

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.cloud:spring-cloud-gateway-webflux")

    implementation("com.microsoft.azure:applicationinsights-runtime-attach:3.5.4")
    implementation("com.microsoft.azure:applicationinsights-web:3.5.4")
    implementation("com.microsoft.azure:applicationinsights-logging-logback:2.6.4")

    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("io.arrow-kt:arrow-fx-coroutines:1.2.4")

    implementation("com.nimbusds:nimbus-jose-jwt:9.40")
    runtimeOnly("com.nimbusds:oauth2-oidc-sdk:11.18")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("org.springframework.boot:spring-boot-devtools")
}

// There is a conflict in logging implementations. Excluded these in favor of using log4j-slf4j2-impl
configurations.all {
    exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    exclude(group = "ch.qos.logback")
}

dependencyManagement {
    imports {
        mavenBom("com.azure.spring:spring-cloud-azure-dependencies:$springCloudAzureVersion")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}