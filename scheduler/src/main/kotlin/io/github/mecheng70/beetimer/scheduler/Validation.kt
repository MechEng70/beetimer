package io.github.mecheng70.beetimer.scheduler

enum class TemplateError {
    /** A template needs at least one step. */
    NO_STEPS,

    /** A TIMED step needs a duration from 1 s to 24 h. */
    DURATION_OUT_OF_RANGE,

    /** The release step is above the take step. */
    RELEASE_BEFORE_TAKE,

    /** A template needs exactly one take step and one release step. */
    MISSING_MARKER,
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
        val takes = steps.count { it.takes }
        val releases = steps.count { it.releases }
        if (takes != 1 || releases != 1) {
            issues += ValidationIssue(TemplateError.MISSING_MARKER)
        } else if (template.releaseIndex < template.takeIndex) {
            issues += ValidationIssue(TemplateError.RELEASE_BEFORE_TAKE, template.releaseIndex)
        }
        // Device steps are derived from the markers, so a split block cannot happen.
        return issues
    }

    fun isValid(template: Template): Boolean = validate(template).isEmpty()
}
