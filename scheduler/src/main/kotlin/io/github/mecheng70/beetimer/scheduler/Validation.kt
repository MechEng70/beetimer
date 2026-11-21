package io.github.mecheng70.beetimer.scheduler

enum class TemplateError {
    /** A template needs at least one step. */
    NO_STEPS,

    /** A TIMED step needs a duration from 1 s to 24 h. */
    DURATION_OUT_OF_RANGE,

    /** Device steps must be next to each other, or another hive could take the vaporizer mid-treatment. */
    SPLIT_DEVICE_BLOCK,
}

data class ValidationIssue(val error: TemplateError, val stepIndex: Int? = null)

object TemplateValidator {
    const val MIN_DURATION_S = 1
    const val MAX_DURATION_S = 24 * 60 * 60

    fun validate(template: Template): List<ValidationIssue> {
        val steps = template.steps
        if (steps.isEmpty()) return listOf(ValidationIssue(TemplateError.NO_STEPS))
        val issues = mutableListOf<ValidationIssue>()
        steps.forEachIndexed { i, step ->
            val d = step.durationSeconds
            if (step.kind == StepKind.TIMED && (d == null || d !in MIN_DURATION_S..MAX_DURATION_S)) {
                issues += ValidationIssue(TemplateError.DURATION_OUT_OF_RANGE, i)
            }
        }
        template.deviceBlock?.let { block ->
            val gap = block.firstOrNull { !steps[it].deviceBound }
            if (gap != null) issues += ValidationIssue(TemplateError.SPLIT_DEVICE_BLOCK, gap)
        }
        return issues
    }

    fun isValid(template: Template): Boolean = validate(template).isEmpty()
}
