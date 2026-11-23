package io.github.mecheng70.beetimer.scheduler

/**
 * Seed templates. Times are reference defaults from manufacturer and extension
 * material; the product label governs. All of it is editable.
 */
object Presets {
    const val PAN_WAND_ID = "oav-pan-wand"
    const val GUN_TYPE_ID = "oav-gun-type"

    // Reference default durations in seconds; the product label governs. Sources: SPEC section 5.

    /** Pan wand heat and vaporize. Citation needed (Jason). */
    const val PAN_HEAT_S = 150

    /** Pan wand cool in hive. Citation needed (Jason). */
    const val PAN_COOL_S = 120

    /** Gun type vaporize. Citation needed (Jason). */
    const val GUN_VAPORIZE_S = 20

    /** Keep sealed after vaporizing: 10 min, from a ProVap 110 field study (SPEC section 5). */
    const val SEALED_HOLD_S = 600

    /** What happens to the pan wand during the sealed hold (Settings choice; read from the step list). */
    enum class PanWandHold { STAYS_IN, COMES_OUT }

    /** The wand stays in the entrance through the sealed hold, so the hold keeps the vaporizer. */
    val panWandStaysIn: Template = Template(
        id = PAN_WAND_ID,
        name = "OAV pan wand",
        steps = listOf(
            Step("Seal hive", StepKind.MANUAL, instruction = "Close entrance and cracks, insert wand."),
            Step("Load dose", StepKind.MANUAL, instruction = "Dose per label into the cold pan.", takes = true),
            Step("Heat and vaporize", StepKind.TIMED, PAN_HEAT_S, instruction = "Power on. Wand in entrance."),
            Step("Cool in hive", StepKind.TIMED, PAN_COOL_S, instruction = "Power off. Leave the wand in."),
            Step("Keep sealed", StepKind.TIMED, SEALED_HOLD_S, instruction = "Wand stays in. Keep hive sealed."),
            Step(
                "Remove wand",
                StepKind.MANUAL,
                instruction = "Pull the wand and close the entrance.",
                releases = true,
            ),
            Step("Unseal", StepKind.MANUAL, instruction = "Open the entrance."),
        ),
    ).derived()

    /** The wand comes out before the sealed hold, so the hold is passive and the next hive can start. */
    val panWandComesOut: Template = Template(
        id = PAN_WAND_ID,
        name = "OAV pan wand",
        steps = listOf(
            Step("Seal hive", StepKind.MANUAL, instruction = "Close entrance and cracks, insert wand."),
            Step("Load dose", StepKind.MANUAL, instruction = "Dose per label into the cold pan.", takes = true),
            Step("Heat and vaporize", StepKind.TIMED, PAN_HEAT_S, instruction = "Power on. Wand in entrance."),
            Step("Cool in hive", StepKind.TIMED, PAN_COOL_S, instruction = "Power off. Leave the wand in."),
            Step(
                "Remove wand",
                StepKind.MANUAL,
                instruction = "Pull the wand and close the entrance.",
                releases = true,
            ),
            Step("Keep sealed", StepKind.TIMED, SEALED_HOLD_S, instruction = "Wand is out. Keep hive sealed."),
            Step("Unseal", StepKind.MANUAL, instruction = "Open the entrance."),
        ),
    ).derived()

    fun panWand(hold: PanWandHold): Template = when (hold) {
        PanWandHold.STAYS_IN -> panWandStaysIn
        PanWandHold.COMES_OUT -> panWandComesOut
    }

    /** The default pan wand (wand stays in). */
    val panWand: Template = panWandStaysIn

    /**
     * Which sealed-hold choice a pan wand step list matches, by shape (step names, kinds and
     * markers in order; durations and instructions ignored), or null for neither.
     */
    fun panWandHoldOf(template: Template): PanWandHold? =
        PanWandHold.entries.firstOrNull { shape(panWand(it)) == shape(template) }

    private fun shape(t: Template) = t.steps.map { listOf(it.name, it.kind, it.takes, it.releases) }

    val gunType: Template = Template(
        id = GUN_TYPE_ID,
        name = "OAV gun type",
        steps = listOf(
            Step("Seal hive", StepKind.MANUAL, instruction = "Close entrance and cracks."),
            Step(
                "Load cap and inject",
                StepKind.MANUAL,
                instruction = "Load the dose per label and insert the nozzle.",
                takes = true,
            ),
            Step("Vaporize", StepKind.TIMED, GUN_VAPORIZE_S, instruction = "Vaporize the dose."),
            Step("Withdraw", StepKind.MANUAL, instruction = "Withdraw the nozzle and reseal.", releases = true),
            Step("Keep sealed", StepKind.TIMED, SEALED_HOLD_S, instruction = "Leave the hive sealed."),
            Step("Unseal", StepKind.MANUAL, instruction = "Open the entrance."),
        ),
    ).derived()

    val all: List<Template> = listOf(panWand, gunType)

    /** Defaults for a preset id; the pan wand follows the sealed-hold choice. */
    fun byId(id: String, hold: PanWandHold = PanWandHold.STAYS_IN): Template? = when (id) {
        PAN_WAND_ID -> panWand(hold)
        GUN_TYPE_ID -> gunType
        else -> null
    }
}
