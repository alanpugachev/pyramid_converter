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
    implementation("org.apache.pdfbox:pdfbox:3.0.8")
    implementation("com.positiondev.epublib:epublib-core:3.1")
    implementation("net.sf.kxml:kxml2:2.3.0")
    implementation("xmlpull:xmlpull:1.1.3.4d_b4_min")

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
