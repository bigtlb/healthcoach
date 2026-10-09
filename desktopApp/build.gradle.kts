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

            modules(
                "jdk.accessibility",
                "jdk.unsupported",
                "java.desktop",
                "java.sql",
                "java.management",
                "java.naming",
                "java.xml"
            )

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

tasks.register<Exec>("packageFlatpak") {
    group = "compose desktop"
    description = "Packages the application into a local Flatpak repository"
    dependsOn("createReleaseDistributable")
    doNotTrackState("Flatpak builder manages its own build cache and OSTree repository")

    val flatpakDir = layout.buildDirectory.dir("flatpak")
    val distDir = layout.buildDirectory.dir("compose/binaries/main-release/app")
    val manifestFile = rootProject.file("packaging/flatpak/com.lbthomas.healthcoach.local.yml")

    workingDir = rootProject.projectDir
    commandLine(
        "flatpak-builder",
        "--force-clean",
        "--repo=${flatpakDir.get().asFile.path}/repo",
        "${flatpakDir.get().asFile.path}/build",
        manifestFile.absolutePath
    )
}

tasks.register<Exec>("packageFlatpakBundle") {
    group = "compose desktop"
    description = "Creates a standalone .flatpak single-file bundle for direct distribution"
    dependsOn("packageFlatpak")
    doNotTrackState("Flatpak manages bundle creation from OSTree repository")

    val flatpakDir = layout.buildDirectory.dir("flatpak")
    val bundleFile = flatpakDir.get().asFile.resolve("HealthCoach-${appPackageVersion}.flatpak")

    workingDir = rootProject.projectDir
    commandLine(
        "flatpak",
        "build-bundle",
        "${flatpakDir.get().asFile.path}/repo",
        bundleFile.absolutePath,
        "com.lbthomas.healthcoach",
        "master"
    )
}
