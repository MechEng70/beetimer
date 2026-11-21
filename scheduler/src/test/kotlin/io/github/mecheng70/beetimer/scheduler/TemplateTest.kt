package io.github.mecheng70.beetimer.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateTest {
    @Test
    fun presetsAreValid() {
        Presets.all.forEach { assertTrue(it.name, TemplateValidator.isValid(it)) }
        assertEquals("OAV pan wand", Presets.panWand.name)
        assertEquals("OAV gun type", Presets.gunType.name)
        assertEquals(1..4, Presets.panWand.deviceBlock)
        assertEquals(1..3, Presets.gunType.deviceBlock)
    }

    @Test
    fun presetTimesMatchSpec() {
        assertEquals(listOf(150, 120, 600), Presets.panWand.steps.mapNotNull { it.durationSeconds })
        assertEquals(listOf(20, 600), Presets.gunType.steps.mapNotNull { it.durationSeconds })
    }

    @Test
    fun emptyTemplateRejected() {
        val issues = TemplateValidator.validate(template())
        assertEquals(listOf(ValidationIssue(TemplateError.NO_STEPS)), issues)
    }

    @Test
    fun durationBounds() {
        fun check(seconds: Int?) = TemplateValidator.validate(template(Step("t", StepKind.TIMED, seconds)))
        assertTrue(check(1).isEmpty())
        assertTrue(check(86_400).isEmpty())
        assertEquals(listOf(ValidationIssue(TemplateError.DURATION_OUT_OF_RANGE, 0)), check(0))
        assertEquals(listOf(ValidationIssue(TemplateError.DURATION_OUT_OF_RANGE, 0)), check(86_401))
        assertEquals(listOf(ValidationIssue(TemplateError.DURATION_OUT_OF_RANGE, 0)), check(null))
    }

    @Test
    fun manualStepNeedsNoDuration() {
        assertTrue(TemplateValidator.isValid(template(manual("Seal"))))
    }

    @Test
    fun splitDeviceBlockRejected() {
        val split = template(
            manual("Seal"),
            manual("Load", device = true),
            timed("Wait", 30),
            timed("Heat", 60, device = true),
        )
        assertEquals(listOf(ValidationIssue(TemplateError.SPLIT_DEVICE_BLOCK, 2)), TemplateValidator.validate(split))
    }

    @Test
    fun noDeviceStepsAllowed() {
        val t = template(manual("Seal"), timed("Wait", 30))
        assertTrue(TemplateValidator.isValid(t))
        assertNull(t.deviceBlock)
    }

    @Test
    fun addRemoveMoveAndEditSteps() {
        val t = template(manual("A"), manual("B"))
        assertEquals(listOf("A", "X", "B"), t.addStep(manual("X"), 1).steps.map { it.name })
        assertEquals(listOf("A", "B", "X"), t.addStep(manual("X")).steps.map { it.name })
        assertEquals(listOf("B"), t.removeStep(0).steps.map { it.name })
        assertEquals(listOf("B", "A"), t.moveStep(0, 1).steps.map { it.name })
        val p = Presets.panWand.withDuration(2, 180)
        assertEquals(180, p.steps[2].durationSeconds)
        assertEquals(150, Presets.panWand.steps[2].durationSeconds)
    }

    @Test
    fun movingStepsCanSplitTheDeviceBlock() {
        val moved = Presets.panWand.moveStep(5, 2) // Keep sealed into the middle of the device block
        assertTrue(TemplateValidator.validate(moved).any { it.error == TemplateError.SPLIT_DEVICE_BLOCK })
    }
}
