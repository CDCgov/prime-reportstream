apply(from = rootProject.file("buildSrc/shared.gradle.kts"))

plugins {
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    id("reportstream.project-conventions")
    kotlin("plugin.spring") version "2.0.21"
}

group = "gov.cdc.prime"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":shared"))

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")

    /**
     * Spring WebFlux was chosen for this project to be able to better handle periods of high traffic
     */
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    runtimeOnly("com.nimbusds:oauth2-oidc-sdk:11.20.1")

    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.6.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

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
        mavenBom("com.azure.spring:spring-cloud-azure-dependencies:5.18.0")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
    }
}

kotlin {
    compilerOptions {
        // https://docs.spring.io/spring-boot/docs/2.0.x/reference/html/boot-features-kotlin.html#boot-features-kotlin-null-safety
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}