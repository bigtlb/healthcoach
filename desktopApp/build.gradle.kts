import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.components.resources)
    implementation(libs.slf4j)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
}

val appVersionName = (rootProject.extra["appVersionName"] as? String) ?: "0.0.1"
val appPackageVersion = appVersionName.substringBefore('-')

compose.desktop {
    application {
        mainClass = "com.lbthomas.healthcoach.MainKt"
        jvmArgs += listOf("-Dskiko.vsync.enabled=false")

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "HealthCoach"
            packageVersion = appPackageVersion
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

            windows {
                packageName = "HealthCoach"
                iconFile.set(project.file("src/main/resources/scales.ico"))
                menuGroup = "HealthCoach"
                shortcut = true
                dirChooser = true
                perUserInstall = true
            }
        }
    }
}