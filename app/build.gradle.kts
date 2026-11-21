plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "io.github.mecheng70.beetimer"
    // compileSdk 36 per SPEC. The newest Compose (1.12+), core-ktx (1.19+) and lifecycle (2.11+)
    // need compileSdk 37, so the catalog pins the last versions that build against 36.
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.mecheng70.beetimer"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-alpha"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // Exported Room schemas feed MigrationTestHelper. Robolectric reads the variant's merged
    // assets, not test assets, so they go in debug assets (a few KB; release unaffected).
    sourceSets.getByName("debug").assets.directories.add("$projectDir/schemas")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":scheduler"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.room.testing)
}
