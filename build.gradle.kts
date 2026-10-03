plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false

    alias(libs.plugins.sqldelight) apply false
}

fun resolveAppVersion(): String {
    val propVersion = findProperty("app.version") as? String
        ?: findProperty("appVersion") as? String
    if (!propVersion.isNullOrBlank()) return propVersion.trim()

    return try {
        val gitDescribeProvider = providers.exec {
            commandLine("git", "describe", "--tags", "--match", "v*", "--always")
            isIgnoreExitValue = true
        }.standardOutput.asText

        val output = gitDescribeProvider.orNull?.trim().orEmpty()
        if (output.isNotEmpty() && (output.startsWith("v") || output.startsWith("V"))) {
            output.substring(1)
        } else if (output.isNotEmpty() && !output.matches(Regex("^[0-9a-f]{7,40}$"))) {
            output
        } else {
            "0.0.1"
        }
    } catch (_: Exception) {
        "0.0.1"
    }
}

fun resolveAppVersionCode(): Int {
    val propCode = (findProperty("app.versionCode") as? String)
        ?: (findProperty("appVersionCode") as? String)
    if (!propCode.isNullOrBlank()) return propCode.toIntOrNull() ?: 1

    return try {
        val gitRevListProvider = providers.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText

        val output = gitRevListProvider.orNull?.trim().orEmpty()
        output.toIntOrNull() ?: 1
    } catch (_: Exception) {
        1
    }
}

extra["appVersionName"] = resolveAppVersion()
extra["appVersionCode"] = resolveAppVersionCode()