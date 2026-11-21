@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package io.github.mecheng70.beetimer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.scheduler.Event
import io.github.mecheng70.beetimer.scheduler.Hive
import io.github.mecheng70.beetimer.scheduler.Presets
import io.github.mecheng70.beetimer.scheduler.StepKind
import io.github.mecheng70.beetimer.scheduler.Template
import io.github.mecheng70.beetimer.scheduler.TemplateError
import io.github.mecheng70.beetimer.scheduler.TemplateValidator
import io.github.mecheng70.beetimer.data.Repository
import io.github.mecheng70.beetimer.ui.theme.AppIcons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import io.github.mecheng70.beetimer.DevTools
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import io.github.mecheng70.beetimer.ui.theme.AppType
import io.github.mecheng70.beetimer.ui.theme.BeeTheme
import io.github.mecheng70.beetimer.ui.theme.Dimens
import io.github.mecheng70.beetimer.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, onAlerts: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(R.string.settings_title), onBack = onBack, backDescription = stringResource(R.string.action_back))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.PageMargin).padding(bottom = Dimens.PageMargin),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
        ) {
            StepsCard()
            HivesCard()
            DevTools.DeveloperCard()
            DisplayCard()
            AppCard(Modifier.fillMaxWidth()) {
                SectionTitle(stringResource(R.string.settings_alerts))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.perms_title), style = AppType.body, color = BeeTheme.colors.inkMuted, modifier = Modifier.weight(1f))
                    AppButton(stringResource(R.string.action_open), onAlerts)
                }
            }
            SessionCard()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = AppType.section, color = BeeTheme.colors.ink, modifier = Modifier.padding(bottom = Dimens.Gap))
}

/**
 * Template editor (mockup 4): preset toggle; per step, tap to edit name and duration, tap the
 * chip to switch device and passive, arrows to reorder. Saved only while valid; a split
 * device block shows inline and is not saved. Active runs keep their snapshot.
 */
@Composable
private fun StepsCard() {
    val app = LocalContext.current.app
    val c = BeeTheme.colors
    val templates by app.repo.templates.collectAsStateWithLifecycle(emptyList())
    val active = templates.firstOrNull { it.isActive } ?: templates.firstOrNull() ?: return
    var draft by remember(active.template) { mutableStateOf(active.template) }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    val issues = TemplateValidator.validate(draft)
    // The sealed-hold choice is read from the step list's shape, so reordering by hand into
    // either default shape selects it.
    val hold = Presets.panWandHoldOf(draft)
    var confirmHold by remember { mutableStateOf<Presets.PanWandHold?>(null) }

    fun persist(t: Template, makeActive: Boolean = false) {
        app.scope.launch {
            app.repo.saveTemplate(t)
            if (makeActive) app.repo.setActiveTemplate(t.id)
            app.engine.dispatch(Event.TemplateChanged(t))
        }
    }

    fun edit(t: Template) {
        draft = t
        if (TemplateValidator.isValid(t)) persist(t)
    }

    AppCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Gap), modifier = Modifier.padding(bottom = Dimens.Gap)) {
            templates.sortedBy { st -> Presets.all.indexOfFirst { it.id == st.template.id } }.forEach { stored ->
                AppButton(
                    text = presetLabel(stored.template.id, stored.template.name),
                    onClick = { persist(stored.template, makeActive = true) },
                    modifier = Modifier.weight(1f).testTag("preset-${stored.template.id}"),
                    selected = stored.template.id == active.template.id,
                )
            }
        }
        if (active.template.id == Presets.PAN_WAND_ID) {
            Text(stringResource(R.string.hold_title), style = AppType.label, color = c.inkMuted, modifier = Modifier.padding(top = Dimens.ChipPaddingV))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Gap), modifier = Modifier.padding(vertical = Dimens.Gap)) {
                listOf(
                    Presets.PanWandHold.STAYS_IN to R.string.hold_stays_in,
                    Presets.PanWandHold.COMES_OUT to R.string.hold_comes_out,
                ).forEach { (choice, label) ->
                    AppButton(
                        text = stringResource(label),
                        onClick = {
                            if (choice != hold) {
                                // Confirm only when the steps are not exactly a default list.
                                val unedited = hold != null && draft == Presets.panWand(hold)
                                if (unedited) {
                                    draft = Presets.panWand(choice)
                                    persist(draft)
                                } else {
                                    confirmHold = choice
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("hold-${choice.name.lowercase()}"),
                        selected = choice == hold,
                    )
                }
            }
        }
        val steps = draft.steps
        steps.forEachIndexed { i, step ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f).testTag("step-$i").clickable { editIndex = i }.padding(vertical = Dimens.RowPadding),
                ) {
                    Text(step.name, style = AppType.body, color = c.ink)
                    val kind = if (step.kind == StepKind.TIMED) stringResource(R.string.kind_timed, formatDuration(step.durationSeconds ?: 0))
                    else stringResource(R.string.kind_manual)
                    val marker = when {
                        step.takes -> stringResource(R.string.marker_takes_short)
                        step.releases -> stringResource(R.string.marker_releases_short)
                        else -> null
                    }
                    Text(
                        if (marker != null) stringResource(R.string.step_sub_marker, kind, marker) else kind,
                        style = AppType.label,
                        color = c.inkMuted,
                    )
                }
                // Display only: Device/Passive comes from the take and release markers.
                Chip(
                    text = stringResource(if (step.deviceBound) R.string.pill_device else R.string.pill_passive),
                    tone = if (step.deviceBound) Tone.DEVICE else Tone.PASSIVE,
                    modifier = Modifier.testTag("step-chip-$i"),
                )
                IconAction(AppIcons.ArrowUp, stringResource(R.string.action_move_up, step.name), { edit(draft.moveStep(i, i - 1)) }, enabled = i > 0)
                IconAction(AppIcons.ArrowDown, stringResource(R.string.action_move_down, step.name), { edit(draft.moveStep(i, i + 1)) }, enabled = i < steps.lastIndex)
            }
            if (step.releases) {
                Row(Modifier.fillMaxWidth().padding(vertical = Dimens.ChipPaddingV), verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f), thickness = Dimens.SelectedBorder, color = c.deviceStrong)
                    Text(
                        stringResource(R.string.vaporizer_frees_here),
                        style = AppType.label,
                        color = c.deviceFg,
                        modifier = Modifier.padding(horizontal = Dimens.Gap).testTag("frees-here"),
                    )
                    HorizontalDivider(Modifier.weight(1f), thickness = Dimens.SelectedBorder, color = c.deviceStrong)
                }
            } else if (i < steps.lastIndex) {
                RowDivider()
            }
        }
        if (issues.any { it.error == TemplateError.RELEASE_BEFORE_TAKE }) {
            Row(Modifier.padding(top = Dimens.Gap), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.template_release_error), style = AppType.label, color = c.danger, modifier = Modifier.weight(1f).testTag("release-error"))
                TextAction(stringResource(R.string.action_undo), { draft = active.template })
            }
        }
        Presets.byId(active.template.id, hold ?: Presets.PanWandHold.STAYS_IN)?.let { preset ->
            VSpace(Dimens.Gap)
            AppButton(stringResource(R.string.template_reset), { draft = preset; persist(preset) }, Modifier.fillMaxWidth())
        }
        VSpace(Dimens.RowPadding)
        Text(stringResource(R.string.template_footer), style = AppType.label, color = c.inkMuted)
    }

    confirmHold?.let { choice ->
        AlertDialog(
            onDismissRequest = { confirmHold = null },
            title = { Text(stringResource(R.string.hold_confirm_title), style = AppType.title) },
            text = { Text(stringResource(R.string.hold_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    draft = Presets.panWand(choice)
                    persist(Presets.panWand(choice))
                    confirmHold = null
                }) { Text(stringResource(R.string.hold_confirm_ok)) }
            },
            dismissButton = { TextButton(onClick = { confirmHold = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    editIndex?.let { i ->
        val step = draft.steps.getOrNull(i) ?: return@let
        var name by remember(i, draft) { mutableStateOf(step.name) }
        var secondsText by remember(i, draft) { mutableStateOf((step.durationSeconds ?: 0).toString()) }
        val timed = step.kind == StepKind.TIMED
        val seconds = secondsText.toIntOrNull()
        val durationOk = !timed || (seconds != null && seconds in TemplateValidator.MIN_DURATION_S..TemplateValidator.MAX_DURATION_S)
        val valid = name.isNotBlank() && durationOk
        AlertDialog(
            onDismissRequest = { editIndex = null },
            title = { Text(stringResource(R.string.step_edit_title), style = AppType.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.step_name)) }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
                        AppButton(
                            stringResource(R.string.marker_takes),
                            { edit(draft.takeAt(i)); editIndex = null },
                            Modifier.weight(1f).testTag("marker-takes"),
                            selected = step.takes,
                        )
                        AppButton(
                            stringResource(R.string.marker_releases),
                            { edit(draft.releaseAt(i)); editIndex = null },
                            Modifier.weight(1f).testTag("marker-releases"),
                            selected = step.releases,
                        )
                    }
                    if (timed) {
                        OutlinedTextField(
                            value = secondsText,
                            onValueChange = { v -> secondsText = v.filter { it.isDigit() }.take(DIGITS) },
                            label = { Text(stringResource(R.string.duration_seconds)) },
                            singleLine = true,
                            isError = !durationOk,
                            supportingText = { if (!durationOk) Text(stringResource(R.string.duration_invalid)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = {
                    edit(draft.updateStep(i, step.copy(name = name.trim(), durationSeconds = if (timed) seconds else step.durationSeconds)))
                    editIndex = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { editIndex = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Short toggle labels from the mockup ("Pan wand", "Gun type"); custom templates keep their name. */
@Composable
private fun presetLabel(id: String, name: String): String = when (id) {
    Presets.PAN_WAND_ID -> stringResource(R.string.preset_short_pan_wand)
    Presets.GUN_TYPE_ID -> stringResource(R.string.preset_short_gun_type)
    else -> name
}

/** Hive list: drag handle, tap to rename, trash with confirm, "+ Add hive" up to [Repository.MAX_HIVES]. */
@Composable
private fun HivesCard() {
    val app = LocalContext.current.app
    val c = BeeTheme.colors
    val hives by app.repo.hives.collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<Hive?>(null) }

    AppCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.settings_hives))
        ReorderableColumn(
            items = hives,
            key = { it.id },
            onMove = { moved -> app.scope.launch { app.repo.reorderHives(moved.map { it.id }) } },
        ) { hive, handle, _ ->
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DragHandle(handle, stringResource(R.string.action_reorder, hive.label))
                    Column(Modifier.weight(1f).testTag("hive-row-${hive.label}").clickable { editing = hive.id }.padding(vertical = Dimens.RowPadding)) {
                        Text(hive.label, style = AppType.hiveName, color = c.ink)
                        hive.note?.let { Text(it, style = AppType.label, color = c.inkMuted) }
                    }
                    IconAction(AppIcons.Delete, stringResource(R.string.action_delete_hive, hive.label), { deleting = hive }, tint = c.danger)
                }
                if (hive != hives.last()) RowDivider()
            }
        }
        VSpace(Dimens.Gap)
        if (hives.size < Repository.MAX_HIVES) {
            AppButton(stringResource(R.string.hive_add), {
                app.scope.launch { app.repo.addHive()?.let { editing = it } }
            }, Modifier.testTag("add-hive"))
        } else {
            Text(pluralStringResource(R.plurals.hive_max, Repository.MAX_HIVES, Repository.MAX_HIVES), style = AppType.label, color = c.inkMuted)
        }
    }

    hives.firstOrNull { it.id == editing }?.let { hive ->
        var name by remember(hive.id) { mutableStateOf(hive.label) }
        var note by remember(hive.id) { mutableStateOf(hive.note.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.hive_edit_title), style = AppType.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.hive_name)) }, singleLine = true)
                    OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.hive_note)) })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    app.scope.launch { app.repo.updateHive(hive.id, name, note) }
                    editing = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    deleting?.let { hive ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.hive_delete_title, hive.label), style = AppType.title) },
            text = { Text(stringResource(R.string.hive_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    app.scope.launch {
                        app.engine.dispatch(Event.Cancel(hive.id))
                        app.repo.deleteHive(hive.id)
                    }
                    deleting = null
                }) { Text(stringResource(R.string.action_delete), color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun DisplayCard() {
    val app = LocalContext.current.app
    val c = BeeTheme.colors
    val theme by app.prefs.theme.collectAsStateWithLifecycle()
    val keepOn by app.prefs.keepScreenOn.collectAsStateWithLifecycle()
    AppCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.settings_display))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
            listOf(
                ThemeMode.SYSTEM to R.string.theme_system,
                ThemeMode.LIGHT to R.string.theme_light,
                ThemeMode.DARK to R.string.theme_dark,
            ).forEach { (mode, label) ->
                AppButton(stringResource(label), { app.prefs.setTheme(mode) }, Modifier.weight(1f), selected = theme == mode)
            }
        }
        Row(Modifier.padding(top = Dimens.Gap), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.keep_screen_on), style = AppType.body, color = c.ink, modifier = Modifier.weight(1f))
            Switch(
                checked = keepOn,
                onCheckedChange = { app.prefs.setKeepScreenOn(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = c.ink, checkedThumbColor = c.card),
            )
        }
    }
}

@Composable
private fun SessionCard() {
    val app = LocalContext.current.app
    val c = BeeTheme.colors
    var confirm by remember { mutableStateOf(false) }
    AppCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.settings_session))
        AppButton(stringResource(R.string.new_session), { confirm = true }, Modifier.testTag("new-session"), kind = ButtonKind.DANGER)
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.new_session_title), style = AppType.title) },
            text = { Text(stringResource(R.string.new_session_text)) },
            confirmButton = {
                TextButton(onClick = {
                    app.send(Event.NewSession)
                    confirm = false
                }, modifier = Modifier.semantics { testTagsAsResourceId = true }.testTag("confirm-new-session")) { Text(stringResource(R.string.new_session), color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private const val DIGITS = 5
