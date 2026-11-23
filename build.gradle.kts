plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

// Format and lint (R11 T-006): ktlint and detekt run as their CLIs, so their own Kotlin parsers never clash with
// AGP's built-in Kotlin. Config: .editorconfig (ktlint) and config/detekt/detekt.yml. No baseline files.
val ktlint: Configuration = configurations.create("ktlint")

// ktlint 1.8.0 bundles logback 1.3.16 (Snyk high). Upgrade it on the ktlint classpath only (R11 T-007).
ktlint.resolutionStrategy.eachDependency {
    if (requested.group == "ch.qos.logback") {
        useVersion("1.5.36")
        because("Snyk high in logback-core 1.3.16")
    }
}
val detekt: Configuration = configurations.create("detekt")

dependencies {
    ktlint(libs.ktlint.cli) {
        attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL)) }
    }
    detekt(libs.detekt.cli)
}

val kotlinSources = listOf("app/src", "scheduler/src", "buildSrc/src")
val ktlintPatterns = listOf(
    "app/src/**/*.kt",
    "scheduler/src/**/*.kt",
    "buildSrc/src/**/*.kt",
    "*.kts",
    "app/*.kts",
    "scheduler/*.kts",
    "buildSrc/*.kts",
)

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Checks Kotlin formatting with ktlint."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args(ktlintPatterns)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Fixes Kotlin formatting with ktlint."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args(listOf("-F") + ktlintPatterns)
}

tasks.register<JavaExec>("detekt") {
    group = "verification"
    description = "Runs detekt on all Kotlin sources. Fails on any finding."
    classpath = detekt
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    args(
        "--input",
        kotlinSources.joinToString(","),
        "--config",
        "config/detekt/detekt.yml",
        "--build-upon-default-config",
        "--report",
        "txt:build/reports/detekt/detekt.txt",
    )
}
