import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {
    @Test
    fun firstSignedBuildIs100001() = assertEquals(100001, AppVersion.code("0.1.0-alpha.1"))

    @Test
    fun stages() {
        assertEquals(100002, AppVersion.code("0.1.0-alpha.2"))
        assertEquals(100301, AppVersion.code("0.1.0-beta.1"))
        assertEquals(100601, AppVersion.code("0.1.0-rc.1"))
        assertEquals(100999, AppVersion.code("0.1.0"))
        assertEquals(10203999, AppVersion.code("1.2.3"))
        assertEquals(10203599, AppVersion.code("1.2.3-beta.299"))
    }

    @Test
    fun ordersAlphaBetaRcFinalThenNextPatch() {
        val names = listOf(
            "0.1.0-alpha.1", "0.1.0-alpha.299", "0.1.0-beta.1", "0.1.0-beta.299", "0.1.0-rc.1", "0.1.0-rc.299",
            "0.1.0", "0.1.1-alpha.1", "0.2.0-alpha.1", "1.0.0-alpha.1", "1.0.0",
        )
        val codes = names.map(AppVersion::code)
        assertTrue("$codes", codes.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun rejectsBadNames() {
        listOf("0.1.0-alpha", "0.1.0-alpha.0", "0.1.0-alpha.300", "0.1", "0.100.0", "0.1.0-dev.1", "v0.1.0").forEach {
            assertThrows(it, IllegalArgumentException::class.java) { AppVersion.code(it) }
        }
    }
}
