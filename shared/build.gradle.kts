import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.lbthomas.healthcoach")
            dialect(libs.sqldelight.sqlite.dialect.get().toString())

            // output directory for generated migration schema
            schemaOutputDirectory = file("src/commonMain/sqldelight/migrations")
            // execute `gradlew generateCommonMainDatabaseSchema` to generate the current db schema, before making a migration
            verifyMigrations = true
        }
    }
}

kotlin {
    jvm()
    
    android {
       namespace = "com.lbthomas.healthcoach.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.slf4j)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)

            implementation(libs.sqldelight.android.driver)
            implementation(libs.koin.android)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.bundles.koin)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.kermit)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serialization)
            implementation(libs.sqldelight.coroutines.extensions)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    publicResClass = true
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}