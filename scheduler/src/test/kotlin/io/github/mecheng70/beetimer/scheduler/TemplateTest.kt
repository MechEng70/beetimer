package io.github.mecheng70.beetimer.scheduler

import io.github.mecheng70.beetimer.scheduler.Presets.PanWandHold
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateTest {
    private fun names(t: Template) = t.steps.map { it.name }
    private fun device(t: Template) = t.steps.map { it.deviceBound }

    @Test
    fun presetsAreValidWithOneTakeAndOneRelease() {
        listOf(Presets.panWandStaysIn, Presets.panWandComesOut, Presets.gunType).forEach {
            assertTrue(it.name, TemplateValidator.isValid(it))
            assertEquals(1, it.steps.count { s -> s.takes })
            assertEquals(1, it.steps.count { s -> s.releases })
        }
        assertEquals("OAV pan wand", Presets.panWand.name)
        assertEquals("OAV gun type", Presets.gunType.name)
    }

    @Test
    fun panWandStaysIn_matchesSpec() {
        val t = Presets.panWand(PanWandHold.STAYS_IN)
        assertEquals(listOf("Seal hive", "Load dose", "Heat and vaporize", "Cool in hive", "Keep sealed", "Remove wand", "Unseal"), names(t))
        assertEquals(1, t.takeIndex)
        assertEquals(5, t.releaseIndex)
        assertEquals(listOf(false, true, true, true, true, true, false), device(t))
        assertEquals(listOf(150, 120, 600), t.steps.mapNotNull { it.durationSeconds })
        assertEquals("Wand stays in. Keep hive sealed.", t.steps[4].instruction)
        assertEquals(Presets.panWand, t)
    }

    @Test
    fun panWandComesOut_matchesSpec() {
        val t = Presets.panWand(PanWandHold.COMES_OUT)
        assertEquals(listOf("Seal hive", "Load dose", "Heat and vaporize", "Cool in hive", "Remove wand", "Keep sealed", "Unseal"), names(t))
        assertEquals(1, t.takeIndex)
        assertEquals(4, t.releaseIndex)
        assertEquals(listOf(false, true, true, true, true, false, false), device(t))
        assertEquals("Pull the wand and close the entrance.", t.steps[4].instruction)
        assertEquals("Wand is out. Keep hive sealed.", t.steps[5].instruction)
    }

    @Test
    fun gunType_matchesSpec() {
        val t = Presets.gunType
        assertEquals(listOf("Seal hive", "Load cap and inject", "Vaporize", "Withdraw", "Keep sealed", "Unseal"), names(t))
        assertEquals(listOf(false, true, true, true, false, false), device(t))
        assertEquals(listOf(20, 600), t.steps.mapNotNull { it.durationSeconds })
    }

    @Test
    fun deviceIsDerivedFromTakeThroughRelease() {
        val t = template(manual("A"), manual("B"), timed("C", 10), manual("D"), manual("E")).takeAt(1).releaseAt(3)
        assertEquals(listOf(false, true, true, true, false), device(t))
        assertEquals(listOf(false, false, false, true, false), device(t.takeAt(3)))
        assertEquals("take and release on one step", listOf(false, false, false, true, false), device(t.takeAt(3).releaseAt(3)))
    }

    @Test
    fun swappingKeepSealedAndRemoveWand_flipsKeepSealedAndTheHoldChoice() {
        val stays = Presets.panWandStaysIn
        assertEquals(PanWandHold.STAYS_IN, Presets.panWandHoldOf(stays))
        val swapped = stays.moveStep(5, 4) // Remove wand above Keep sealed
        val keep = swapped.steps.first { it.name == "Keep sealed" }
        assertEquals(false, keep.deviceBound)
        assertEquals("durations never change on a move", 600, keep.durationSeconds)
        assertEquals(PanWandHold.COMES_OUT, Presets.panWandHoldOf(swapped))
        val back = swapped.moveStep(4, 5)
        assertEquals(true, back.steps.first { it.name == "Keep sealed" }.deviceBound)
        assertEquals(PanWandHold.STAYS_IN, Presets.panWandHoldOf(back))
    }

    @Test
    fun renamingAStepKeepsItsDurationAndMarkers() {
        val t = Presets.panWandStaysIn
        val renamed = t.updateStep(4, t.steps[4].copy(name = "Hold"))
        assertEquals(600, renamed.steps[4].durationSeconds)
        assertEquals(device(t), device(renamed))
        assertNull("not one of the two shapes any more", Presets.panWandHoldOf(renamed))
    }

    @Test
    fun releaseAboveTakeRejected() {
        val bad = Presets.panWandStaysIn.moveStep(5, 0) // Remove wand to the top
        assertEquals(listOf(ValidationIssue(TemplateError.RELEASE_BEFORE_TAKE, 0)), TemplateValidator.validate(bad))
        assertTrue(device(bad).none { it })
    }

    @Test
    fun missingMarkerRejected() {
        val none = template(manual("Seal"), timed("Wait", 30))
        assertEquals(listOf(ValidationIssue(TemplateError.MISSING_MARKER)), TemplateValidator.validate(none))
        assertEquals(listOf(ValidationIssue(TemplateError.MISSING_MARKER)), TemplateValidator.validate(none.takeAt(0)))
    }

    @Test
    fun emptyTemplateRejected() {
        assertEquals(listOf(ValidationIssue(TemplateError.NO_STEPS)), TemplateValidator.validate(template()))
    }

    @Test
    fun durationBounds() {
        fun check(seconds: Int?) = TemplateValidator.validate(
            Template("t", "t", listOf(Step("t", StepKind.TIMED, seconds, takes = true, releases = true))).derived(),
        )
        assertTrue(check(1).isEmpty())
        assertTrue(check(86_400).isEmpty())
        assertEquals(listOf(ValidationIssue(TemplateError.DURATION_OUT_OF_RANGE, 0)), check(0))
        assertEquals(listOf(ValidationIssue(TemplateError.DURATION_OUT_OF_RANGE, 0)), check(86_401))
        assertEquals(listOf(ValidationIssue(TemplateError.DURATION_OUT_OF_RANGE, 0)), check(null))
    }

    @Test
    fun addRemoveMoveAndEditSteps() {
        val t = template(manual("A"), manual("B"))
        assertEquals(listOf("A", "X", "B"), names(t.addStep(manual("X"), 1)))
        assertEquals(listOf("B"), names(t.removeStep(0)))
        assertEquals(listOf("B", "A"), names(t.moveStep(0, 1)))
        val p = Presets.panWand.withDuration(2, 180)
        assertEquals(180, p.steps[2].durationSeconds)
        assertEquals(150, Presets.panWand.steps[2].durationSeconds)
    }
}
