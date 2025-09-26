import java.net.URI

pluginManagement {
    repositories {
        maven { url = uri("https://repo.spring.io/snapshot") }
        gradlePluginPortal()
    }
}

rootProject.name = "prime-reportstream"
include("shared", "submissions", "prime-router", "auth")
