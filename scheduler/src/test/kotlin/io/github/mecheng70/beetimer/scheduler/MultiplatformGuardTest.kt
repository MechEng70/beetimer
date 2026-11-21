package io.github.mecheng70.beetimer.scheduler

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps the scheduler ready for Kotlin Multiplatform (iOS later).
 * Main sources may use the Kotlin stdlib and kotlinx.serialization only.
 * This test itself runs on the JVM, so it may use java.io.
 */
class MultiplatformGuardTest {
    private val forbidden = listOf(
        Regex("""\bjava\."""),
        Regex("""\bjavax\."""),
        Regex("""\bandroid\."""),
        Regex("""\bandroidx\."""),
        Regex("""\bkotlin\.jvm\."""),
        Regex("""@Jvm\w+"""),
        Regex("""@Synchronized\b"""),
        Regex("""\bThread\b"""),
    )

    @Test
    fun schedulerMainSourcesUseOnlyMultiplatformApis() {
        val root = File("src/main")
        assertTrue("scheduler main sources not found at ${root.absolutePath}", root.isDirectory)
        val files = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("no Kotlin files found under ${root.absolutePath}", files.isNotEmpty())

        val violations = files.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, raw ->
                val line = raw.substringBefore("//").trim()
                if (line.startsWith("*") || line.startsWith("/*")) return@mapIndexedNotNull null
                forbidden.firstOrNull { it.containsMatchIn(line) }
                    ?.let { "${file.relativeTo(root)}:${i + 1}: '${it.pattern}' in: ${raw.trim()}" }
            }
        }
        assertTrue(
            "Scheduler must stay Kotlin stdlib only (KMP-ready). Violations:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }
}
