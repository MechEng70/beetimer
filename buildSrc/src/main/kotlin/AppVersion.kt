/**
 * Version scheme (playbook section 7, R11 T-014): versionCode = (major * 10000 + minor * 100 + patch) * 1000 +
 * stage, where alpha.N = N, beta.N = 300 + N, rc.N = 600 + N and a final release = 999. So every alpha sorts
 * before every beta, every beta before every rc, and the final release after all of them.
 * Example: 0.1.0-alpha.1 = 100001.
 */
object AppVersion {
    private val PATTERN =
        Regex("""(?<major>\d+)\.(?<minor>\d+)\.(?<patch>\d+)(?:-(?<stage>alpha|beta|rc)\.(?<number>\d+))?""")
    private const val MAJOR = 10_000
    private const val MINOR = 100
    private const val RELEASE = 1_000
    private const val BETA = 300
    private const val RC = 600
    private const val FINAL = 999
    private const val MAX_PART = 99
    private const val MAX_STAGE_NUMBER = 299

    /** The versionCode for [name], like "0.1.0-alpha.1" or "1.2.3". Throws on anything else. */
    fun code(name: String): Int {
        val groups = requireNotNull(PATTERN.matchEntire(name)) { "Not a version name: '$name'" }.groups
        fun part(key: String): String? = groups[key]?.value
        val (major, minor, patch) = listOf("major", "minor", "patch").map { part(it)!!.toInt() }
        require(minor <= MAX_PART && patch <= MAX_PART) { "Minor and patch must be 0 to 99: '$name'" }
        val base = (major * MAJOR + minor * MINOR + patch) * RELEASE
        val stage = part("stage") ?: return base + FINAL
        val n = part("number")!!.toInt()
        require(n in 1..MAX_STAGE_NUMBER) { "Stage number must be 1 to 299: '$name'" }
        val offset = when (stage) {
            "beta" -> BETA
            "rc" -> RC
            else -> 0
        }
        return base + offset + n
    }
}
