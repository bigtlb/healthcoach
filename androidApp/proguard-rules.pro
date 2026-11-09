# ============================================================================
# HealthCoach Android ProGuard Rules
# ============================================================================
# Keeps all application code and dependencies that must not be obfuscated.
# Adapted from desktopApp/proguard-rules.pro with Android Compose additions.
# ============================================================================

# Keep attributes needed for reflection and serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# ============================================================================
# Compose Multiplatform UI
# ============================================================================
# Keep all Compose runtime classes and annotations
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.animation.** { *; }

# Keep @Composable functions and their metadata
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable <methods>;
}
-keepclassmembers class ** {
    @androidx.compose.runtime.Stable <methods>;
}
-keepclassmembers class ** {
    @androidx.compose.runtime.Immutable <methods>;
}

# Keep Compose preview classes
-keepclassmembers class ** {
    @androidx.compose.ui.tooling.preview.Preview <methods>;
}

# ============================================================================
# Coroutines
# ============================================================================
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ============================================================================
# SQLDelight
# ============================================================================
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# ============================================================================
# Kermit Logging
# ============================================================================
-keep class co.touchlab.kermit.** { *; }
-dontwarn co.touchlab.kermit.**

# ============================================================================
# Koin DI
# ============================================================================
-keep class io.insert.koin.** { *; }
-dontwarn io.insert.koin.**

# ============================================================================
# Application code - all HealthCoach classes must be preserved
# ============================================================================
-keep class com.lbthomas.healthcoach.** { *; }

# ============================================================================
# Vico Charts
# ============================================================================
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# ============================================================================
# Ktor Network, Server & Client
# ============================================================================
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.atomicfu.**
-dontwarn com.typesafe.config.**
-dontwarn org.fusesource.jansi.**

# ============================================================================
# JmDNS / Zeroconf
# ============================================================================
-keep class javax.jmdns.** { *; }
-dontwarn javax.jmdns.**

# ============================================================================
# kotlinx.serialization
# ============================================================================
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# ============================================================================
# AndroidX and Android framework
# ============================================================================
-keep class androidx.** { *; }
-dontwarn androidx.**

# ============================================================================
# OkHttp (used by Ktor on Android)
# ============================================================================
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**