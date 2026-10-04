# Disable bytecode optimization to prevent corruption of Kotlin inline LocalVariableTable attributes
-dontoptimize
-dontnote

# SQLite JDBC Driver
-keep class org.sqlite.** { *; }
-dontwarn org.sqlite.**

# SQLDelight
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# SLF4J
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**
-keep class org.slf4j.simple.** { *; }
-dontwarn org.slf4j.simple.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kermit Logging
-keep class co.touchlab.kermit.** { *; }
-dontwarn co.touchlab.kermit.**

# Koin
-keep class io.insert.koin.** { *; }
-dontwarn io.insert.koin.**

# Application models & database
-keep class com.lbthomas.healthcoach.** { *; }

# Vico Charts
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# Ktor Network, Server & Client
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.atomicfu.**
-dontwarn com.typesafe.config.**
-dontwarn org.fusesource.jansi.**

# JmDNS / Zeroconf
-keep class javax.jmdns.** { *; }
-dontwarn javax.jmdns.**
