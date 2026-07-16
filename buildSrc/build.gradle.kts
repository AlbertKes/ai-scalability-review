plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "2.2.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.2.5")
    implementation("org.flywaydb:flyway-gradle-plugin:12.4.0")
    runtimeOnly("org.flywaydb:flyway-mysql:12.4.0")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:12.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}
