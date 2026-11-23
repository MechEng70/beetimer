import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Version (R11 T-014): the name lives in gradle.properties; the code follows the playbook scheme (AppVersion).
val appVersion: String = providers.gradleProperty("appVersion").get()
val appVersionCode: Int = AppVersion.code(appVersion)

// Release signing (R11 T-015): keystore.properties (storeFile, storePassword, keyAlias, keyPassword) in the repo
// root, git-ignored, or the file named by -PbeetimerKeystoreProperties or BEETIMER_KEYSTORE_PROPERTIES. Without
// it, release builds are unsigned and scripts/release.sh refuses to run. Values are never printed.
val keystorePropsPath: String = providers.gradleProperty("beetimerKeystoreProperties")
    .orElse(providers.environmentVariable("BEETIMER_KEYSTORE_PROPERTIES"))
    .getOrElse(rootProject.file("keystore.properties").absolutePath)
val keystoreProps = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file(keystorePropsPath)).asText.orNull
        ?.let { load(it.reader()) }
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
        versionCode = appVersionCode
        versionName = appVersion
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                // "~/" expands to the home folder, as in scripts/release.sh.
                val home = System.getProperty("user.home")
                storeFile = file(keystoreProps.getProperty("storeFile").replaceFirst(Regex("^~/"), "$home/"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
        }
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
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

// R11 T-007: Android lint and Room's KSP processor pull in library versions with known high and critical issues
// (Snyk). None of them ship in the APK (releaseRuntimeClasspath has none). Upgrade them wherever they're resolved.
val patchedToolLibraries = mapOf(
    "com.google.guava:guava" to "33.7.2-jre",
    "org.apache.commons:commons-lang3" to "3.18.0",
    "org.bouncycastle:bcprov-jdk18on" to "1.86",
    "org.bouncycastle:bcpkix-jdk18on" to "1.86",
    "org.bouncycastle:bcutil-jdk18on" to "1.86",
)
configurations.configureEach {
    resolutionStrategy.eachDependency {
        patchedToolLibraries["${requested.group}:${requested.name}"]?.let {
            useVersion(it)
            because("Snyk high or critical in the older version (R11 T-007)")
        }
    }
}

// Prints the versionCode, for scripts/release.sh.
tasks.register("printVersionCode") {
    val code = appVersionCode
    doLast { println(code) }
}
