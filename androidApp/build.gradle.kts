import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

val appVersionName = (rootProject.extra["appVersionName"] as? String) ?: "0.0.1"
val appVersionCode = (rootProject.extra["appVersionCode"] as? Int) ?: 1

android {
    namespace = "com.lbthomas.healthcoach"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    val releaseKeystorePath = (findProperty("RELEASE_KEYSTORE_FILE") as? String)
        ?: System.getenv("RELEASE_KEYSTORE_FILE")
        ?: "release.keystore"
    val releaseKeystoreFile = rootProject.file(releaseKeystorePath)
    val hasReleaseKeystore = releaseKeystoreFile.exists()

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = (findProperty("RELEASE_KEYSTORE_PASSWORD") as? String)
                    ?: System.getenv("RELEASE_KEYSTORE_PASSWORD")
                    ?: ""
                keyAlias = (findProperty("RELEASE_KEY_ALIAS") as? String)
                    ?: System.getenv("RELEASE_KEY_ALIAS")
                    ?: "healthcoach"
                keyPassword = (findProperty("RELEASE_KEY_PASSWORD") as? String)
                    ?: System.getenv("RELEASE_KEY_PASSWORD")
                    ?: storePassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.lbthomas.healthcoach"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}