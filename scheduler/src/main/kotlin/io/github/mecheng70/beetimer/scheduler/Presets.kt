package io.github.mecheng70.beetimer.scheduler

/**
 * Seed templates. Times are reference defaults from manufacturer and extension
 * material; the product label governs. All of it is editable.
 */
object Presets {
    const val PAN_WAND_ID = "oav-pan-wand"
    const val GUN_TYPE_ID = "oav-gun-type"

    /** The wand stays in the entrance through the sealed hold, so the hold keeps the device. */
    val panWand: Template = Template(
        id = PAN_WAND_ID,
        name = "OAV pan wand",
        steps = listOf(
            Step("Seal hive", StepKind.MANUAL, deviceBound = false, instruction = "Close entrance and cracks, insert wand."),
            Step("Load dose", StepKind.MANUAL, deviceBound = true, instruction = "Dose per label into the cold pan."),
            Step("Heat and vaporize", StepKind.TIMED, 150, deviceBound = true, instruction = "Power on. Wand in entrance."),
            Step("Cool in hive", StepKind.TIMED, 120, deviceBound = true, instruction = "Power off. Leave the wand in."),
            Step("Keep sealed", StepKind.TIMED, 600, deviceBound = true, instruction = "Wand stays in. Keep hive sealed."),
            Step("Remove wand and unseal", StepKind.MANUAL, deviceBound = true, instruction = "Remove the wand and open the entrance. Done frees the wand."),
        ),
    )

    val gunType: Template = Template(
        id = GUN_TYPE_ID,
        name = "OAV gun type",
        steps = listOf(
            Step("Seal hive", StepKind.MANUAL, deviceBound = false, instruction = "Close entrance and cracks."),
            Step("Load cap and inject", StepKind.MANUAL, deviceBound = true, instruction = "Load the dose per label and insert the nozzle."),
            Step("Vaporize", StepKind.TIMED, 20, deviceBound = true, instruction = "Vaporize the dose."),
            Step("Withdraw", StepKind.MANUAL, deviceBound = true, instruction = "Withdraw the nozzle and reseal."),
            Step("Keep sealed", StepKind.TIMED, 600, deviceBound = false, instruction = "Leave the hive sealed."),
            Step("Unseal", StepKind.MANUAL, deviceBound = false, instruction = "Open the entrance."),
        ),
    )

    val all: List<Template> = listOf(panWand, gunType)

    fun byId(id: String): Template? = all.firstOrNull { it.id == id }
}
