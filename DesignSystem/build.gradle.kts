plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

group = "ru.reosfire.pixorio"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}