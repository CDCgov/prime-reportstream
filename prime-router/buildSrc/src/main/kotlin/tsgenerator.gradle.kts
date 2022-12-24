plugins {
    `java-library`
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("jvm"))
    implementation("com.github.ntrrgc", "ts-generator", "1.1.1")
    implementation("com.google.guava", "guava", "27.0.1-jre")
    implementation("org.jetbrains.kotlin","kotlin-reflect", "1.3.50")
}