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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.scheduler.Event
import io.github.mecheng70.beetimer.scheduler.Hive
import io.github.mecheng70.beetimer.scheduler.Presets
import io.github.mecheng70.beetimer.scheduler.StepKind
import io.github.mecheng70.beetimer.scheduler.Template
import io.github.mecheng70.beetimer.scheduler.TemplateValidator
import io.github.mecheng70.beetimer.ui.theme.AppType
import io.github.mecheng70.beetimer.ui.theme.BeeTheme
import io.github.mecheng70.beetimer.ui.theme.Dimens
import io.github.mecheng70.beetimer.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, onAlerts: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.settings_title), onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.PageMargin).padding(bottom = Dimens.PageMargin),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
        ) {
            StepsCard()
            HivesCard()
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

/** Template editor (mockup 4). Alpha: preset toggle and TIMED durations only. */
@Composable
private fun StepsCard() {
    val app = LocalContext.current.app
    val c = BeeTheme.colors
    val templates by app.repo.templates.collectAsStateWithLifecycle(emptyList())
    val active = templates.firstOrNull { it.isActive } ?: templates.firstOrNull() ?: return
    var editIndex by remember { mutableStateOf<Int?>(null) }

    fun save(t: Template, makeActive: Boolean = false) {
        if (!TemplateValidator.isValid(t)) return
        app.scope.launch {
            app.repo.saveTemplate(t)
            if (makeActive) app.repo.setActiveTemplate(t.id)
            app.engine.dispatch(Event.TemplateChanged(t))
        }
    }

    AppCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Gap), modifier = Modifier.padding(bottom = Dimens.Gap)) {
            templates.sortedBy { st -> Presets.all.indexOfFirst { it.id == st.template.id } }.forEach { stored ->
                AppButton(
                    text = presetLabel(stored.template.id, stored.template.name),
                    onClick = { save(stored.template, makeActive = true) },
                    modifier = Modifier.weight(1f),
                    selected = stored.template.id == active.template.id,
                )
            }
        }
        val steps = active.template.steps
        steps.forEachIndexed { i, step ->
            Row(Modifier.fillMaxWidth().padding(vertical = Dimens.RowPadding), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(step.name, style = AppType.body, color = c.ink)
                    Text(
                        if (step.kind == StepKind.TIMED) stringResource(R.string.kind_timed, step.durationSeconds ?: 0)
                        else stringResource(R.string.kind_manual),
                        style = AppType.chip,
                        color = c.inkMuted,
                    )
                }
                if (step.deviceBound) Chip(stringResource(R.string.pill_device), Tone.DEVICE)
                else if (step.kind == StepKind.TIMED) Chip(stringResource(R.string.pill_passive), Tone.PASSIVE)
                if (step.kind == StepKind.TIMED) TextAction(stringResource(R.string.action_edit), { editIndex = i })
            }
            if (i < steps.lastIndex) RowDivider()
        }
        Presets.byId(active.template.id)?.let { preset ->
            VSpace(Dimens.Gap)
            AppButton(stringResource(R.string.template_reset), { save(preset) }, Modifier.fillMaxWidth())
        }
        VSpace(Dimens.RowPadding)
        Text(stringResource(R.string.template_footer), style = AppType.chip, color = c.inkMuted)
    }

    editIndex?.let { i ->
        val step = active.template.steps[i]
        var text by remember(i, active.template.id) { mutableStateOf((step.durationSeconds ?: 0).toString()) }
        val seconds = text.toIntOrNull()
        val valid = seconds != null && seconds in TemplateValidator.MIN_DURATION_S..TemplateValidator.MAX_DURATION_S
        AlertDialog(
            onDismissRequest = { editIndex = null },
            title = { Text(step.name, style = AppType.title) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() }.take(DIGITS) },
                    label = { Text(stringResource(R.string.duration_seconds)) },
                    singleLine = true,
                    isError = !valid,
                    supportingText = { if (!valid) Text(stringResource(R.string.duration_invalid)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = {
                    save(active.template.withDuration(i, seconds!!))
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

@Composable
private fun HivesCard() {
    val app = LocalContext.current.app
    val c = BeeTheme.colors
    val hives by app.repo.hives.collectAsStateWithLifecycle(emptyList())
    var addCount by remember { mutableIntStateOf(1) }
    var editing by remember { mutableStateOf<Hive?>(null) }
    var deleting by remember { mutableStateOf<Hive?>(null) }

    AppCard(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.settings_hives))
        hives.forEachIndexed { i, hive ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = Dimens.RowPadding)) {
                    Text(hive.label, style = AppType.hiveName, color = c.ink)
                    hive.note?.let { Text(it, style = AppType.label, color = c.inkMuted) }
                }
                TextAction(stringResource(R.string.move_up), { app.scope.launch { app.repo.moveHive(hive.id, -1) } }, enabled = i > 0)
                TextAction(stringResource(R.string.move_down), { app.scope.launch { app.repo.moveHive(hive.id, 1) } }, enabled = i < hives.lastIndex)
                TextAction(stringResource(R.string.action_edit), { editing = hive })
                TextAction(stringResource(R.string.action_delete), { deleting = hive }, color = c.danger)
            }
            RowDivider()
        }
        VSpace(Dimens.Gap)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.Gap)) {
            AppButton(stringResource(R.string.count_minus), { addCount = (addCount - 1).coerceAtLeast(1) })
            AppButton(stringResource(R.string.settings_add_hives, addCount), { app.scope.launch { app.repo.addHives(addCount) } })
            AppButton(stringResource(R.string.count_plus), { addCount = (addCount + 1).coerceAtMost(MAX_ADD) })
        }
    }

    editing?.let { hive ->
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
        AppButton(stringResource(R.string.new_session), { confirm = true }, kind = ButtonKind.DANGER)
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
                }) { Text(stringResource(R.string.new_session), color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private const val MAX_ADD = 20
private const val DIGITS = 5
