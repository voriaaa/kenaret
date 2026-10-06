package com.voria.kenaret.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voria.kenaret.BuildConfig
import com.voria.kenaret.R
import com.voria.kenaret.cycle.CycleCalculator
import com.voria.kenaret.database.NotificationPreference
import com.voria.kenaret.database.isConnected
import com.voria.kenaret.domain.Role
import com.voria.kenaret.notifications.NotificationHelper
import com.voria.kenaret.pairing.PairingCode
import com.voria.kenaret.ui.KenaretUiState
import com.voria.kenaret.ui.MainViewModel
import com.voria.kenaret.ui.common.BulletLine
import com.voria.kenaret.ui.common.Fmt
import com.voria.kenaret.ui.common.KenaretTopBar
import com.voria.kenaret.ui.common.NoteText
import com.voria.kenaret.ui.common.SectionCard
import com.voria.kenaret.ui.common.isPersian
import com.voria.kenaret.ui.onboarding.AppLogo
import com.voria.kenaret.ui.onboarding.PairingCodeField
import com.voria.kenaret.ui.onboarding.rememberNotificationPermissionThen
import kotlin.math.roundToInt

@Composable
private fun DetailScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(topBar = { KenaretTopBar(title = title, onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

// ---------------------------------------------------------------- Cycle settings

@Composable
fun CycleSettingsScreen(state: KenaretUiState, onBack: () -> Unit, onSave: (Int, Int, Boolean) -> Unit) {
    val profile = state.profile
    var cycle by remember { mutableFloatStateOf((profile?.cycleLength ?: CycleCalculator.DEFAULT_CYCLE).toFloat()) }
    var period by remember { mutableFloatStateOf((profile?.periodLength ?: CycleCalculator.DEFAULT_PERIOD).toFloat()) }
    var unknown by remember { mutableStateOf(profile?.cycleLengthUnknown ?: false) }
    val insights = state.insights

    DetailScaffold(stringResource(R.string.settings_cycle), onBack) {
        SectionCard(title = stringResource(R.string.setup_cycle_length)) {
            Text(
                stringResource(R.string.days_count, if (unknown) CycleCalculator.DEFAULT_CYCLE else cycle.roundToInt()),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Slider(
                value = cycle,
                onValueChange = { cycle = it },
                valueRange = CycleCalculator.MIN_CYCLE.toFloat()..CycleCalculator.MAX_CYCLE.toFloat(),
                steps = CycleCalculator.MAX_CYCLE - CycleCalculator.MIN_CYCLE - 1,
                enabled = !unknown,
            )
            Row(Modifier.fillMaxWidth().clickable { unknown = !unknown }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = unknown, onCheckedChange = { unknown = it })
                Text(stringResource(R.string.setup_unknown_length))
            }
            if (insights.completedCycles > 0) {
                NoteText(stringResource(R.string.cycle_settings_estimate, state.effectiveCycleLength, insights.completedCycles))
            } else {
                NoteText(stringResource(R.string.setup_unknown_explanation))
            }
        }
        SectionCard(title = stringResource(R.string.setup_period_length)) {
            Text(
                stringResource(R.string.days_count, period.roundToInt()),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Slider(
                value = period,
                onValueChange = { period = it },
                valueRange = CycleCalculator.MIN_PERIOD.toFloat()..CycleCalculator.MAX_PERIOD.toFloat(),
                steps = CycleCalculator.MAX_PERIOD - CycleCalculator.MIN_PERIOD - 1,
            )
        }
        NoteText(stringResource(R.string.cycle_settings_note))
        Button(onClick = { onSave(cycle.roundToInt(), period.roundToInt(), unknown) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.action_save))
        }
    }
}

// ---------------------------------------------------------------- Notifications

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(state: KenaretUiState, onBack: () -> Unit, onChange: (NotificationPreference) -> Unit) {
    val context = LocalContext.current
    val persian = isPersian()
    val prefs = state.notificationPreference
    val isPartner = state.role == Role.PARTNER
    var showTimePicker by remember { mutableStateOf(false) }
    val requestPermissionThen = rememberNotificationPermissionThen()
    var permissionGranted by remember { mutableStateOf(NotificationHelper.hasPermission(context)) }

    DetailScaffold(stringResource(R.string.settings_notifications), onBack) {
        SectionCard {
            SwitchRow(
                title = stringResource(R.string.notif_enabled),
                checked = prefs.enabled,
                onChange = { enabled ->
                    if (enabled) {
                        requestPermissionThen {
                            permissionGranted = NotificationHelper.hasPermission(context)
                            onChange(prefs.copy(enabled = true))
                        }
                    } else {
                        onChange(prefs.copy(enabled = false))
                    }
                },
            )
            if (prefs.enabled && !permissionGranted) {
                NoteText(stringResource(R.string.notif_permission_missing))
                TextButton(onClick = {
                    requestPermissionThen { permissionGranted = NotificationHelper.hasPermission(context) }
                }) { Text(stringResource(R.string.notif_permission_allow)) }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = prefs.enabled) { showTimePicker = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.notif_time), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    Fmt.time(prefs.hour, prefs.minute, persian),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        SectionCard(title = stringResource(R.string.notif_types_title)) {
            SwitchRow(stringResource(R.string.notif_period), stringResource(R.string.notif_period_hint), prefs.periodReminders, prefs.enabled) {
                onChange(prefs.copy(periodReminders = it))
            }
            SwitchRow(stringResource(R.string.notif_phase), stringResource(R.string.notif_phase_hint), prefs.phaseReminders, prefs.enabled) {
                onChange(prefs.copy(phaseReminders = it))
            }
            if (isPartner) {
                SwitchRow(stringResource(R.string.notif_partner), stringResource(R.string.notif_partner_hint), prefs.partnerReminders, prefs.enabled) {
                    onChange(prefs.copy(partnerReminders = it))
                }
            }
            SwitchRow(stringResource(R.string.notif_educational), stringResource(R.string.notif_educational_hint), prefs.educationalReminders, prefs.enabled) {
                onChange(prefs.copy(educationalReminders = it))
            }
        }
        SectionCard(title = stringResource(R.string.settings_privacy)) {
            SwitchRow(stringResource(R.string.notif_hide_sensitive), stringResource(R.string.notif_hide_sensitive_hint), prefs.hideSensitive) {
                onChange(prefs.copy(hideSensitive = it))
            }
        }
        NoteText(stringResource(R.string.notif_note))
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = prefs.hour, initialMinute = prefs.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.notif_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    onChange(prefs.copy(hour = timeState.hour, minute = timeState.minute))
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

// ---------------------------------------------------------------- Partner

@Composable
fun PartnerSettingsScreen(state: KenaretUiState, viewModel: MainViewModel, onBack: () -> Unit) {
    DetailScaffold(stringResource(R.string.settings_partner), onBack) {
        if (state.role == Role.PARTNER) PartnerSideSettings(state, viewModel) else HerSideSettings(state, viewModel)
    }
}

@Composable
private fun HerSideSettings(state: KenaretUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val partner = state.partner
    var showCode by remember { mutableStateOf(false) }
    val code = state.pairingCode
    val privateItems = stringArrayResource(R.array.private_items)
    val shareText = code?.let { stringResource(R.string.pairing_share_message, it) }
    val chooserTitle = stringResource(R.string.pairing_share)

    Text(stringResource(R.string.partner_her_intro), style = MaterialTheme.typography.bodyMedium)

    SectionCard(title = stringResource(R.string.share_title)) {
        SwitchRow(stringResource(R.string.share_phase), checked = partner.sharePhase) {
            viewModel.updateSharing(it, partner.shareNextPeriodDate, partner.shareDaysUntil)
        }
        SwitchRow(stringResource(R.string.share_next_date), checked = partner.shareNextPeriodDate) {
            viewModel.updateSharing(partner.sharePhase, it, partner.shareDaysUntil)
        }
        SwitchRow(stringResource(R.string.share_days_until), checked = partner.shareDaysUntil) {
            viewModel.updateSharing(partner.sharePhase, partner.shareNextPeriodDate, it)
        }
    }

    SectionCard(title = stringResource(R.string.private_title)) {
        privateItems.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text(item, style = MaterialTheme.typography.bodyMedium)
            }
        }
        NoteText(stringResource(R.string.private_note))
    }

    if (code == null) {
        NoteText(stringResource(R.string.pairing_nothing_shared))
    } else if (!showCode) {
        Button(onClick = { showCode = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.partner_connect_button))
        }
    } else {
        SectionCard(title = stringResource(R.string.pairing_code_title), containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                code,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.pairing_code_how), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                    Text(stringResource(R.string.pairing_copy))
                }
                OutlinedButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, chooserTitle))
                }) { Text(stringResource(R.string.pairing_share)) }
            }
        }
        NoteText(stringResource(R.string.pairing_update_note))
    }
}

@Composable
private fun PartnerSideSettings(state: KenaretUiState, viewModel: MainViewModel) {
    val persian = isPersian()
    val partner = state.partner
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var confirmDisconnect by remember { mutableStateOf(false) }

    if (partner.isConnected()) {
        SectionCard(title = stringResource(R.string.partner_connected_title)) {
            partner.connectedOn?.let {
                Text(stringResource(R.string.partner_connected_since, Fmt.shortDate(it, persian)), style = MaterialTheme.typography.bodyMedium)
            }
            Text(stringResource(R.string.partner_shared_with_you), style = MaterialTheme.typography.titleSmall)
            if (partner.sharePhase) BulletLine(stringResource(R.string.share_phase))
            if (partner.shareNextPeriodDate) BulletLine(stringResource(R.string.share_next_date))
            if (partner.shareDaysUntil) BulletLine(stringResource(R.string.share_days_until))
        }
    } else {
        Text(stringResource(R.string.partner_not_connected_body), style = MaterialTheme.typography.bodyMedium)
    }

    SectionCard(title = stringResource(if (partner.isConnected()) R.string.partner_update_title else R.string.partner_connect_title)) {
        Text(
            stringResource(R.string.partner_connect_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PairingCodeField(code = code, error = error, onChange = {
            code = it
            error = false
        })
        Button(
            onClick = {
                val data = PairingCode.decode(code)
                if (data == null) {
                    error = true
                } else {
                    viewModel.connectPartner(data)
                    code = ""
                }
            },
            enabled = code.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.partner_connect_button)) }
    }

    if (partner.isConnected()) {
        OutlinedButton(onClick = { confirmDisconnect = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.partner_disconnect), color = MaterialTheme.colorScheme.error)
        }
    }
    NoteText(stringResource(R.string.partner_setup_privacy))

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text(stringResource(R.string.partner_disconnect)) },
            text = { Text(stringResource(R.string.partner_disconnect_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDisconnect = false
                    viewModel.disconnectPartner()
                }) { Text(stringResource(R.string.partner_disconnect), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisconnect = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

// ---------------------------------------------------------------- Privacy

@Composable
fun PrivacyScreen(state: KenaretUiState, viewModel: MainViewModel, onBack: () -> Unit, onDataDeleted: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    val points = stringArrayResource(R.array.privacy_points)
    val prefs = state.notificationPreference

    DetailScaffold(stringResource(R.string.settings_privacy), onBack) {
        SectionCard(title = stringResource(R.string.privacy_title)) {
            points.forEach { BulletLine(it) }
        }
        SectionCard {
            SwitchRow(stringResource(R.string.notif_hide_sensitive), stringResource(R.string.notif_hide_sensitive_hint), prefs.hideSensitive) {
                viewModel.updateNotifications(prefs.copy(hideSensitive = it))
            }
        }
        SectionCard(title = stringResource(R.string.settings_delete_all)) {
            Text(stringResource(R.string.delete_all_body), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.delete_all_confirm), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDelete) {
        DeleteAllDialog(
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                viewModel.deleteAllData(onDataDeleted)
            },
        )
    }
}

// ---------------------------------------------------------------- About & disclaimer

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val persian = isPersian()
    DetailScaffold(stringResource(R.string.settings_about), onBack) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            AppLogo(size = 88)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(R.string.about_version, Fmt.digits(BuildConfig.VERSION_NAME, persian)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionCard {
            Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyMedium)
        }
        SectionCard(title = stringResource(R.string.about_principle_title)) {
            Text(stringResource(R.string.about_principle_body), style = MaterialTheme.typography.bodyMedium)
        }
        NoteText(stringResource(R.string.about_font_credit))
    }
}

@Composable
fun DisclaimerScreen(onBack: () -> Unit) {
    DetailScaffold(stringResource(R.string.settings_disclaimer), onBack) {
        SectionCard {
            Text(stringResource(R.string.disclaimer_full), style = MaterialTheme.typography.bodyLarge)
        }
        SectionCard {
            Text(stringResource(R.string.disclaimer_severe), style = MaterialTheme.typography.bodyLarge)
        }
        SectionCard {
            Text(stringResource(R.string.disclaimer_no_medication), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
