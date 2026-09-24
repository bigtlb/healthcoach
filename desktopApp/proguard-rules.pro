# Disable bytecode optimization to prevent corruption of Kotlin inline LocalVariableTable attributes
-dontoptimize

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
