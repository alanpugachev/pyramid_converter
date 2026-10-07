import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.3.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21"
    id("org.jetbrains.compose") version "1.11.1"
}

group = "alanpugachev"
version = "0.1.0"
description = "pyramid_converter"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

compose.desktop {
    application {
        mainClass = "alanpugachev.pyramid_converter.PyramidConverterApplicationKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Pyramid Converter"
            packageVersion = "0.1.0"

            macOS {
                bundleID = "alanpugachev.pyramidconverter"
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
