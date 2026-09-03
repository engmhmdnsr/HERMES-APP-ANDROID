# ProGuard / R8 rules for Hermes Control

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# Hermes Data Models and Serialization
-keep class ee.oversight.hermes.model.** { *; }
-keep class ee.oversight.hermes.data.** { *; }

# AndroidX Security Crypto & Tink
-dontwarn androidx.security.crypto.**
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.errorprone.annotations.**
