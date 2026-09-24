import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.components.resources)
    implementation(libs.slf4j)
    implementation(libs.koin.core)
}

compose.desktop {
    application {
        mainClass = "com.lbthomas.healthcoach.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "HealthCoach"
            packageVersion = libs.versions.app.version.get()
            description = "Health and wellness tracking application"
            vendor = "LBThomas"

            modules("java.sql", "jdk.unsupported", "java.naming", "java.management")

            linux {
                // Debian package names must be lowercase, numbers, plus, minus, and dots
                packageName = "healthcoach"
                iconFile.set(project.file("src/main/resources/scales.png"))
                appCategory = "Utility;MedicalSoftware;"
                menuGroup = "Utility"
            }
        }
    }
}