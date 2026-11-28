import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(enforcedPlatform(libs.kotlinx.coroutines.bom))
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
    implementation(libs.kotlin.reflect)
}

val appVersionName = (rootProject.extra["appVersionName"] as? String) ?: "0.0.1"
val appPackageVersion = appVersionName.substringBefore('-')

compose.desktop {
    application {
        mainClass = "com.lbthomas.healthcoach.MainKt"

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
                "java.net.http",
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

            macOS {
                packageName = "HealthCoach"
                iconFile.set(project.file("src/main/resources/scales.png"))
            }
        }
    }
}

tasks.configureEach {
    if (name == "packageReleaseRpm") {
        dependsOn("createReleaseDistributable")
        notCompatibleWithConfigurationCache("Rebuilds the RPM with a custom jpackage spec after the standard package task")
        outputs.upToDateWhen { false }

        doLast {
            val appImageSource = layout.buildDirectory.dir("compose/binaries/main-release/app/HealthCoach").get().asFile
            val rpmAppImage = layout.buildDirectory.dir("compose/tmp/healthcoach-rpm-app/HealthCoach").get().asFile
            val appStreamSource = rootProject.file("packaging/flatpak/com.lbthomas.healthcoach.metainfo.xml")
            val flatpakDesktopSource = rootProject.file("packaging/flatpak/com.lbthomas.healthcoach.desktop")
            val appStreamInImage = rpmAppImage.resolve("lib/app/.healthcoach-appstream.xml")
            val rpmDesktopInImage = rpmAppImage.resolve("lib/app/.healthcoach-rpm.desktop")
            val rpmOutputDir = layout.buildDirectory.dir("compose/tmp/healthcoach-rpm-output").get().asFile

            delete(rpmAppImage.parentFile)
            rpmAppImage.mkdirs()
            val copyResult = ProcessBuilder("cp", "-a", "${appImageSource.absolutePath}/.", rpmAppImage.absolutePath)
                .inheritIO()
                .start()
                .waitFor()
            if (copyResult != 0) {
                throw GradleException("Could not stage the RPM application image (cp exited with $copyResult)")
            }

            appStreamInImage.writeText(appStreamSource.readText())
            val flatpakDesktopEntry = flatpakDesktopSource.readText()
            val flatpakExec = "Exec=HealthCoach"
            if (flatpakDesktopEntry.lines().count { it == flatpakExec } != 1) {
                throw GradleException("Expected exactly one '$flatpakExec' line in $flatpakDesktopSource")
            }
            rpmDesktopInImage.writeText(
                flatpakDesktopEntry.replace(flatpakExec, "Exec=/opt/healthcoach/bin/HealthCoach")
            )

            delete(rpmOutputDir)
            rpmOutputDir.mkdirs()
            val jpackageCommand = listOf(
                File(System.getProperty("java.home"), "bin/jpackage").absolutePath,
                "--type", "rpm",
                "--app-image", rpmAppImage.absolutePath,
                "--name", "HealthCoach",
                "--linux-package-name", "healthcoach",
                "--linux-app-category", "Utility;MedicalSoftware;",
                "--linux-menu-group", "Utility",
                "--icon", project.file("src/main/resources/scales.png").absolutePath,
                "--app-version", appPackageVersion,
                "--description", "Health and wellness tracking application",
                "--vendor", "Thomas Baker",
                "--linux-rpm-license-type", "Apache-2.0",
                "--resource-dir", rootProject.file("packaging/rpm").absolutePath,
                "--dest", rpmOutputDir.absolutePath
            )
            val jpackageResult = ProcessBuilder(jpackageCommand)
                .directory(rootProject.projectDir)
                .inheritIO()
                .start()
                .waitFor()
            if (jpackageResult != 0) {
                throw GradleException("jpackage could not rebuild the RPM with AppStream metadata (exit code $jpackageResult)")
            }

            val appStreamRpm = rpmOutputDir.listFiles()?.singleOrNull { it.extension == "rpm" }
                ?: throw GradleException("jpackage did not produce exactly one RPM in $rpmOutputDir")
            val rpmOutput = layout.buildDirectory.dir("compose/binaries/main-release/rpm").get().asFile
                .resolve(appStreamRpm.name)
            appStreamRpm.copyTo(rpmOutput, overwrite = true)
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
    val stateDir = flatpakDir.get().asFile.resolve("state")

    workingDir = rootProject.projectDir
    commandLine(
        "flatpak-builder",
        "--force-clean",
        "--state-dir=${stateDir.path}",
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
