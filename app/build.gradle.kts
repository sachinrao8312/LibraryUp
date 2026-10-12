plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp) // Kotlin Symbol Processing
    alias(libs.plugins.hilt.android) // Hilt plugin
}

android {
    namespace = "ai.sach.libraryup"
    compileSdk = 35

    defaultConfig {
        applicationId = "ai.sach.libraryup"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Add build config fields for performance optimization
        buildConfigField("boolean", "ENABLE_PERFORMANCE_MONITORING", "false")
        buildConfigField("boolean", "ENABLE_MEMORY_LEAK_DETECTION", "false")
        buildConfigField("boolean", "ENABLE_TEST_FEATURES", "false")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Add additional release optimizations
            buildConfigField("boolean", "ENABLE_PERFORMANCE_MONITORING", "false")
            buildConfigField("boolean", "ENABLE_MEMORY_LEAK_DETECTION", "false")
            buildConfigField("boolean", "ENABLE_TEST_FEATURES", "false")
            }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            // Enable debug tools only in debug builds
            buildConfigField("boolean", "ENABLE_PERFORMANCE_MONITORING", "true")
            buildConfigField("boolean", "ENABLE_MEMORY_LEAK_DETECTION", "true")
            buildConfigField("boolean", "ENABLE_TEST_FEATURES", "true")
            }
    }
    
    compileOptions {
        // java.time on API 24-25 (the domain module uses it throughout).
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11" // Ensure consistent JVM target
        // Enable more strict Kotlin compiler options (optional, but recommended)
        freeCompilerArgs = listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xallow-kotlin-package",
            "-Xjvm-default=all", // Enable JVM default methods for better performance (ADDED)
            "-Xno-param-assertions", // Disable parameter assertions for better performance
            "-Xno-call-assertions", // Disable call assertions for better performance
            "-Xno-receiver-assertions" // Disable receiver assertions for better performance
        )
    }
    
    testOptions {
        // Robolectric tests (repository, backup) need merged resources and the manifest.
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "META-INF/androidx.core.graphics.drawable.IconCompatParcelizer"
            excludes += "META-INF/android.support.v4.graphics.drawable.IconCompatParcelizer"
            pickFirsts += "META-INF/DEPENDENCIES"
        }
    }
}

ksp {
    // Exported Room schemas are what migration tests run against. Commit app/schemas/.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":domain"))

    // Firebase - using the Bill of Materials (BoM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx) {
        exclude(group = "androidx.legacy", module = "legacy-support-core-utils")
    }
    implementation(libs.firebase.firestore.ktx) {
        exclude(group = "androidx.legacy", module = "legacy-support-core-utils")
    }
    implementation(libs.firebase.crashlytics.ktx) {
        exclude(group = "androidx.legacy", module = "legacy-support-core-utils")
    }
    implementation(libs.firebase.analytics.ktx) {
        exclude(group = "androidx.legacy", module = "legacy-support-core-utils")
    }

    // Google Services & APIs
    implementation(libs.play.services.auth) {
        exclude(group = "androidx.legacy", module = "legacy-support-core-utils")
    }
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.play.services)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Networking
    implementation(libs.gson) // backup envelope
    implementation(libs.okhttp) // Google Drive REST client
    implementation(libs.libphonenumber)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.lifecycle.process)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Compose Bill of Materials (BoM)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.constraintlayout.compose.android)

    // AndroidX Core & Utilities
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.material)
    implementation(libs.androidx.work.runtime.ktx)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Security
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    
    // Joda Time (required by Google Crypto Tink)
    implementation(libs.joda.time)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}