package io.github.mecheng70.beetimer.data

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Shipped Room schemas and migrations are frozen (R11 T-009). A phone that installed an older version upgrades
 * through exactly these files, so changing one breaks real upgrades. Schema changes go in a new version with a
 * new migration; when it ships, add its checksums here.
 *
 * Migration sources are compared with all whitespace removed, so re-formatting passes and any real edit fails.
 * Note: MIGRATION_2_3 writes the current [io.github.mecheng70.beetimer.scheduler.Presets]; MigrationTest covers
 * what it writes.
 */
class FrozenSchemaTest {
    private val schemaDir = File("schemas/io.github.mecheng70.beetimer.data.BeeTimerDb")
    private val dbSource = File("src/main/java/io/github/mecheng70/beetimer/data/Db.kt")

    private val frozenSchemas = mapOf(
        1 to "143bc52d72accacfe438a760deb570289ffe4a21265c1023ea4739d744072c63",
        2 to "fd9a767092444f2120d15c186cb3a273838e94fcfa1fb03e074fe3aaf6b4a563",
        3 to "f2f5e1e57550fb9ebc2ecf927159994db92815de7dbc9074956c03543573f992",
        // v4 ships with 0.1.0-alpha.1 (R11 T-010).
        4 to "426baf6a5d1512903f16792bafc1978ffe4ebd98851dd1b81557d1ce22aebc1c",
    )

    private val frozenMigrations = mapOf(
        "MIGRATION_1_2" to "2cfc77df576b181aa7a3437c50f8d47037d7e52d521f10704a25a489b8ec2545",
        "MIGRATION_2_3" to "525a768d7caa15f18b2ec5fcff809129db21f990aaa1b60ad07dfecb71d98261",
        "MIGRATION_3_4" to "0a856c8b778e71ced8ac1eba37353cd52548110ef8d818dad8fe5225f710559c",
    )

    @Test
    fun shippedSchemaFilesAreUnchanged() {
        frozenSchemas.forEach { (version, expected) ->
            val file = File(schemaDir, "$version.json")
            assertTrue("schema $version.json is missing", file.isFile)
            val text = file.readText().replace("\r\n", "\n")
            assertEquals("schema $version.json changed; add a new version instead", expected, sha256(text))
        }
    }

    @Test
    fun shippedMigrationsAreUnchanged() {
        val source = dbSource.readText()
        frozenMigrations.forEach { (name, expected) ->
            val normalized = migrationBlock(source, name).replace(Regex("\\s+"), "")
            assertEquals("$name changed; add a new migration instead", expected, sha256(normalized))
        }
    }

    @Test
    fun everySchemaVersionHasItsMigrationAndTheDbIsOnTheLatest() {
        val versions = schemaDir.listFiles().orEmpty()
            .mapNotNull { it.name.removeSuffix(".json").toIntOrNull() }
            .sorted()
        assertEquals("schema versions must run 1..n with no gaps", (1..versions.last()).toList(), versions)
        val source = dbSource.readText()
        versions.drop(1).forEach { v ->
            assertTrue("no MIGRATION_${v - 1}_$v for schema $v", "val MIGRATION_${v - 1}_$v = " in source)
        }
        assertEquals("database version is not the newest schema", versions.last(), BeeTimerDb.VERSION)
        assertTrue("a frozen schema version was removed", versions.containsAll(frozenSchemas.keys))
    }

    /** The `val NAME = object ... { ... }` declaration, by brace matching. */
    private fun migrationBlock(source: String, name: String): String {
        val start = source.indexOf("val $name = object")
        assertTrue("$name not found in Db.kt", start >= 0)
        var depth = 0
        var i = source.indexOf('{', start)
        while (i < source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> if (--depth == 0) return source.substring(start, i + 1)
            }
            i++
        }
        error("$name: unbalanced braces")
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
