package io.github.mecheng70.beetimer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mecheng70.beetimer.R
import io.github.mecheng70.beetimer.data.StoredTemplate
import io.github.mecheng70.beetimer.scheduler.Event
import io.github.mecheng70.beetimer.scheduler.Hive
import io.github.mecheng70.beetimer.scheduler.Presets
import io.github.mecheng70.beetimer.scheduler.StepKind
import io.github.mecheng70.beetimer.scheduler.Template
import io.github.mecheng70.beetimer.scheduler.TemplateValidator
import io.github.mecheng70.beetimer.ui.theme.Dimens
import io.github.mecheng70.beetimer.ui.theme.LocalStatusColors
import io.github.mecheng70.beetimer.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onAlerts: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = Dimens.Gutter).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.Gap),
        ) {
            HivesSection()
            HorizontalDivider()
            StepsSection()
            HorizontalDivider()
            DisplaySection()
            HorizontalDivider()
            Text(stringResource(R.string.settings_alerts), style = MaterialTheme.typography.titleLarge)
            BigOutlinedButton(stringResource(R.string.perms_title), onAlerts, Modifier.fillMaxWidth())
            HorizontalDivider()
            SessionSection()
            Spacer(Modifier.height(Dimens.Gutter))
        }
    }
}

@Composable
private fun HivesSection() {
    val app = LocalContext.current.app
    val hives by app.repo.hives.collectAsStateWithLifecycle(emptyList())
    var addCount by remember { mutableIntStateOf(1) }
    var editing by remember { mutableStateOf<Hive?>(null) }
    var deleting by remember { mutableStateOf<Hive?>(null) }

    Text(stringResource(R.string.settings_hives), style = MaterialTheme.typography.titleLarge)
    hives.forEachIndexed { i, hive ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(Modifier.weight(1f)) {
                Text(hive.label, style = MaterialTheme.typography.titleMedium)
                hive.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            TextButton(onClick = { app.scope.launch { app.repo.moveHive(hive.id, -1) } }, enabled = i > 0) { Text(stringResource(R.string.move_up)) }
            TextButton(onClick = { app.scope.launch { app.repo.moveHive(hive.id, 1) } }, enabled = i < hives.lastIndex) { Text(stringResource(R.string.move_down)) }
            TextButton(onClick = { editing = hive }) { Text(stringResource(R.string.action_edit)) }
            TextButton(onClick = { deleting = hive }) { Text(stringResource(R.string.action_delete), color = LocalStatusColors.current.danger) }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BigOutlinedButton("-", { addCount = (addCount - 1).coerceAtLeast(1) })
        BigButton(stringResource(R.string.settings_add_hives, addCount), { app.scope.launch { app.repo.addHives(addCount) } }, Modifier.weight(1f))
        BigOutlinedButton("+", { addCount = (addCount + 1).coerceAtMost(MAX_ADD) })
    }

    editing?.let { hive ->
        var name by remember(hive.id) { mutableStateOf(hive.label) }
        var note by remember(hive.id) { mutableStateOf(hive.note.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.hive_edit_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            title = { Text(stringResource(R.string.hive_delete_title, hive.label)) },
            text = { Text(stringResource(R.string.hive_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    app.scope.launch {
                        app.engine.dispatch(Event.Cancel(hive.id))
                        app.repo.deleteHive(hive.id)
                    }
                    deleting = null
                }) { Text(stringResource(R.string.action_delete), color = LocalStatusColors.current.danger) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun StepsSection() {
    val app = LocalContext.current.app
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

    Text(stringResource(R.string.settings_steps), style = MaterialTheme.typography.titleLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        templates.sortedBy { Presets.all.indexOfFirst { p -> p.id == it.template.id } }.forEach { stored: StoredTemplate ->
            val selected = stored.template.id == active.template.id
            OutlinedButton(
                onClick = { save(stored.template, makeActive = true) },
                modifier = Modifier.weight(1f).height(Dimens.TouchMin),
                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            ) { Text(stored.template.name) }
        }
    }
    active.template.steps.forEachIndexed { i, step ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(step.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (step.kind == StepKind.TIMED) stringResource(R.string.kind_timed, step.durationSeconds ?: 0)
                    else stringResource(R.string.kind_manual),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (step.deviceBound) Pill(stringResource(R.string.pill_device), Tone.DEVICE)
            else if (step.kind == StepKind.TIMED) Pill(stringResource(R.string.pill_passive), Tone.PASSIVE)
            if (step.kind == StepKind.TIMED) TextButton(onClick = { editIndex = i }) { Text(stringResource(R.string.action_edit)) }
        }
    }
    Presets.byId(active.template.id)?.let { preset ->
        BigOutlinedButton(stringResource(R.string.template_reset), { save(preset) }, Modifier.fillMaxWidth())
    }
    Text(stringResource(R.string.template_footer), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

    editIndex?.let { i ->
        val step = active.template.steps[i]
        var text by remember(i, active.template.id) { mutableStateOf((step.durationSeconds ?: 0).toString()) }
        val seconds = text.toIntOrNull()
        val valid = seconds != null && seconds in TemplateValidator.MIN_DURATION_S..TemplateValidator.MAX_DURATION_S
        AlertDialog(
            onDismissRequest = { editIndex = null },
            title = { Text(stringResource(R.string.duration_title, step.name)) },
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

@Composable
private fun DisplaySection() {
    val app = LocalContext.current.app
    val theme by app.prefs.theme.collectAsStateWithLifecycle()
    val keepOn by app.prefs.keepScreenOn.collectAsStateWithLifecycle()
    Text(stringResource(R.string.settings_display), style = MaterialTheme.typography.titleLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            ThemeMode.SYSTEM to R.string.theme_system,
            ThemeMode.LIGHT to R.string.theme_light,
            ThemeMode.DARK to R.string.theme_dark,
        ).forEach { (mode, label) ->
            OutlinedButton(
                onClick = { app.prefs.setTheme(mode) },
                modifier = Modifier.weight(1f).height(Dimens.TouchMin),
                border = if (theme == mode) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            ) { Text(stringResource(label)) }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.keep_screen_on), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = keepOn, onCheckedChange = { app.prefs.setKeepScreenOn(it) })
    }
}

@Composable
private fun SessionSection() {
    val app = LocalContext.current.app
    var confirm by remember { mutableStateOf(false) }
    Text(stringResource(R.string.settings_session), style = MaterialTheme.typography.titleLarge)
    BigOutlinedButton(stringResource(R.string.new_session), { confirm = true }, Modifier.fillMaxWidth(), color = LocalStatusColors.current.danger)
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.new_session_title)) },
            text = { Text(stringResource(R.string.new_session_text)) },
            confirmButton = {
                TextButton(onClick = {
                    app.send(Event.NewSession)
                    confirm = false
                }) { Text(stringResource(R.string.new_session)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private const val MAX_ADD = 20
private const val DIGITS = 5
