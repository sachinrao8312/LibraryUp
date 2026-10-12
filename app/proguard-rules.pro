# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile
#
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Google Sign-In & Play Services Auth
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.auth.api.identity.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# Google API Client (for Drive functionality)
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.http.client.** { *; }

# Kotlin Coroutines (important for async auth)
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Androidx Credentials
-keep class androidx.credentials.** { *; }
-keep class androidx.credentials.playservices.** { *; }

# JSON serialization (for user data)
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Keep authentication data classes
-keep class ai.sach.rentup.ui.screens.authentication.** { *; }

# Security and biometric classes
-keep class androidx.biometric.** { *; }
-keep class androidx.security.crypto.** { *; }

# Reflection and annotations needed for auth
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# OkHttp and networking (used by Firebase)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ========== GOOGLE API CLIENT ANDROID FIXES ==========
# These classes are not available on Android - suppress warnings
-dontwarn com.sun.net.httpserver.**
-dontwarn java.awt.**
-dontwarn com.google.api.client.extensions.jetty.**
-dontwarn com.google.api.client.extensions.java6.**

# Keep Google API client classes that ARE available on Android
-keep class com.google.api.client.googleapis.** { *; }
-keep class com.google.api.client.http.** { *; }
-keep class com.google.api.client.json.** { *; }
-keep class com.google.api.client.util.** { *; }

# ========== HILT DEPENDENCY INJECTION RULES ==========
# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-keep class **_HiltModules* { *; }
-keep class **_Provide* { *; }
-keep class **_Factory* { *; }

# Keep injected classes
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @javax.inject.Singleton class * { *; }

# ========== CLASSLOADER OPTIMIZATION RULES ==========
# Prevent ClassLoader context mismatches
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Signature

# Keep classes that are accessed via reflection
-keep class ai.sach.rentup.data.local.entity.** { *; }
-keep class ai.sach.rentup.data.repository.** { *; }
# Backup envelope and Drive API models are (de)serialized by Gson by field name. Renamed
# fields would make a backup written by one release unreadable by the next.
-keep class ai.sach.rentup.data.backup.** { *; }
# Retrofit request/response models are serialized by Gson by field name.
-keep class ai.sach.rentup.data.api.** { *; }
# Retrofit 2.9 predates its own R8 full-mode rules; without these, a suspend API method
# fails in release builds with "Class cannot be cast to ParameterizedType".
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Fix ClassLoader context size mismatches
-keep class dalvik.system.** { *; }
-dontwarn dalvik.system.**

# Optimize duplicate class handling
-dontwarn android.support.v4.**
-keep class androidx.** { *; }

# Fix Google Play Services ClassLoader issues
-keep class com.google.android.gms.dynamite.** { *; }
-dontwarn com.google.android.gms.dynamite.**

# EXCLUDE old support library classes - they cause duplicate class issues
-dontwarn android.support.v4.graphics.drawable.IconCompatParcelizer
-keep class !android.support.v4.graphics.drawable.IconCompatParcelizer

# ========== RESOURCE MANAGEMENT RULES ==========
# Keep resource management utilities
-keep class ai.sach.rentup.utils.ResourceManager { *; }

# Keep classes that implement Closeable
-keep class * implements java.io.Closeable { *; }

# ========== CRITICAL FIXES FOR CLASSLOADER ISSUES ==========
# Fix ClassLoader context size mismatches
-keep class * extends android.app.Application { *; }
-keep class * extends androidx.multidex.MultiDexApplication { *; }

# Prevent duplicate class loading
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Fix resource package ID conflicts
-keep class android.content.res.** { *; }
-keep class android.util.** { *; }

# Optimize memory usage for large composables
-keep class ai.sach.rentup.ui.screens.homescreen.composable.** { *; }
-keep class ai.sach.rentup.ui.screens.properties.** { *; }

# Prevent ClassLoader context size mismatches during compilation
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Fix duplicate class detection issues
-dontwarn android.support.v4.graphics.drawable.**
-keep class androidx.core.graphics.drawable.** { *; }

# ========== ENHANCED CLASSLOADER OPTIMIZATION RULES ==========
# Fix ClassLoader context size mismatches
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Signature
-keepattributes Exceptions

# Keep classes that are accessed via reflection
-keep class ai.sach.rentup.data.local.entity.** { *; }
-keep class ai.sach.rentup.data.repository.** { *; }

# Fix ClassLoader context size mismatches
-keep class dalvik.system.** { *; }
-dontwarn dalvik.system.**

# Optimize duplicate class handling
-dontwarn android.support.v4.**
-keep class androidx.** { *; }

# Fix Google Play Services ClassLoader issues
-keep class com.google.android.gms.dynamite.** { *; }
-dontwarn com.google.android.gms.dynamite.**

# EXCLUDE old support library classes - they cause duplicate class issues
-dontwarn android.support.v4.graphics.drawable.IconCompatParcelizer
-keep class !android.support.v4.graphics.drawable.IconCompatParcelizer

# ========== RESOURCE MANAGEMENT RULES ==========
# Keep resource management utilities
-keep class ai.sach.rentup.utils.ResourceManager { *; }

# Keep classes that implement Closeable
-keep class * implements java.io.Closeable { *; }

# ========== ENHANCED DUPLICATE CLASS FIXES ==========
# Fix duplicate class detection for support libraries
-dontwarn android.support.v4.**
-dontwarn android.support.v7.**
-dontwarn android.support.design.**

# Keep AndroidX equivalents
-keep class androidx.core.** { *; }
-keep class androidx.appcompat.** { *; }
-keep class androidx.fragment.** { *; }
-keep class androidx.activity.** { *; }

# Fix resource package ID errors
-keep class android.content.res.Resources { *; }
-keep class android.content.res.AssetManager { *; }
-keep class android.content.res.Configuration { *; }

# Fix ClassLoader context size mismatches
-keep class * extends android.content.Context { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends androidx.fragment.app.Fragment { *; }

# ========== KOTLIN COROUTINES OPTIMIZATION ==========
# Fix Kotlin coroutines ClassLoader issues
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep coroutine dispatchers
-keep class kotlinx.coroutines.Dispatchers { *; }
-keep class kotlinx.coroutines.CoroutineScope { *; }

# ========== COMPOSE OPTIMIZATION ==========
# Fix Compose ClassLoader issues
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# ========== ROOM DATABASE OPTIMIZATION ==========
# Fix Room ClassLoader issues
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Keep Room entities and DAOs
-keep class ai.sach.rentup.data.local.entity.** { *; }
-keep class ai.sach.rentup.data.local.dao.** { *; }

# ========== FIREBASE OPTIMIZATION ==========
# Fix Firebase ClassLoader issues
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Firebase Auth classes
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.firestore.** { *; }

# ========== GOOGLE PLAY SERVICES OPTIMIZATION ==========
# Fix Google Play Services ClassLoader issues
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep Google Play Services Auth
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }

# ========== JODA TIME FIXES ==========
# Fix missing Joda Time classes (used by Google Crypto Tink)
-dontwarn org.joda.time.Instant
-dontwarn org.joda.time.**
-keep class org.joda.time.** { *; }

# ========== LOGGING LIBRARY FIXES ==========
# Suppress warnings for Apache Commons Logging and Log4J
-dontwarn org.apache.commons.logging.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-keep class org.apache.commons.logging.** { *; }
-keep class org.apache.log4j.** { *; }
-keep class org.apache.logging.log4j.** { *; }

# ========== PERFORMANCE OPTIMIZATIONS ==========
# Enable aggressive optimizations for better performance
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Remove unused code and optimize method calls
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimize string operations
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-optimizationpasses 5

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# ========== HIDDEN API REDUCTION ==========
# Remove reflection usage that causes hidden API warnings
-dontwarn sun.misc.**
-dontwarn java.lang.reflect.**
-dontwarn dalvik.system.**
-dontwarn android.app.ApplicationLoaders
-dontwarn android.app.LoadedApk
-dontwarn com.android.internal.os.ClassLoaderFactory
-dontwarn libcore.util.EmptyArray
-dontwarn libcore.io.Libcore
-dontwarn libcore.io.Os
-dontwarn android.system.StructStat

# Remove hidden API access warnings
-dontwarn android.app.ApplicationLoaders
-dontwarn android.content.pm.SharedLibraryInfo
-dontwarn android.os.Trace
-dontwarn com.android.internal.os.ClassLoaderFactory
-dontwarn dalvik.system.BaseDexClassLoader
-dontwarn dalvik.system.DexPathList
-dontwarn dalvik.system.DexFile
-dontwarn dalvik.system.VMStack
-dontwarn libcore.util.EmptyArray
-dontwarn libcore.io.Libcore
-dontwarn libcore.io.Os
-dontwarn android.system.StructStat
-dontwarn sun.nio.ch.Interruptible
-dontwarn dalvik.system.ThreadPrioritySetter
-dontwarn dalvik.system.RuntimeHooks

# ========== ADDITIONAL OPTIMIZATIONS ==========
# Remove debug information in release builds
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Optimize method calls
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-optimizationpasses 5

# Remove unused classes and methods
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# ========== FIREBASE RULES ==========