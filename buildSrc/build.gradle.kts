// Build logic shared by the build scripts (R11 T-014): the version scheme. Plain Kotlin with its own tests;
// scripts/check.sh runs them (./gradlew -p buildSrc test).
plugins {
    `kotlin-dsl`
}

dependencies {
    testImplementation(libs.junit)
}
