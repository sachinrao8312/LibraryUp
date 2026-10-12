// Pure Kotlin business rules for LibraryUp: billing periods, dues, seat/shift
// conflicts, receipts, reminder templates, CSV. No Android dependencies, so the
// rules are unit-tested on the JVM and shared with any future sibling app.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    testImplementation(libs.junit)
}
