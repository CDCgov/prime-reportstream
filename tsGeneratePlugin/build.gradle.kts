plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven {
        url = uri("https://jitpack.io")
    }
}

group = "gov.cdc.prime"
version = "0.2-SNAPSHOT"

dependencies {
    implementation("com.github.ntrrgc:ts-generator:1.1.2")
    implementation("io.github.classgraph:classgraph:4.8.153")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.1")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
    plugins {
        create("tsGeneratePlugin") {
            id = "gov.cdc.prime.tsGeneratePlugin"
            implementationClass = "gov.cdc.prime.tsGeneratePlugin.TypescriptGeneratorPlugin"
        }
    }
}
