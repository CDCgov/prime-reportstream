plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(gradleApi())
    implementation("com.github.ntrrgc:ts-generator:1.1.1")
    implementation("io.github.classgraph:classgraph:4.8.153")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.1")
}


// Set the compiler JVM target
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}