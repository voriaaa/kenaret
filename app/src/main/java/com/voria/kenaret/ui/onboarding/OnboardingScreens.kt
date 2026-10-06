package com.voria.kenaret.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.voria.kenaret.R
import com.voria.kenaret.cycle.CycleCalculator
import com.voria.kenaret.notifications.NotificationHelper
import com.voria.kenaret.pairing.PairingCode
import com.voria.kenaret.pairing.PairingData
import com.voria.kenaret.settings.LocaleManager
import com.voria.kenaret.ui.changeLanguage
import com.voria.kenaret.ui.common.Fmt
import com.voria.kenaret.ui.common.KenaretDatePickerDialog
import com.voria.kenaret.ui.common.KenaretTopBar
import com.voria.kenaret.ui.common.NoteText
import com.voria.kenaret.ui.common.SectionCard
import com.voria.kenaret.ui.common.isPersian
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun AppLogo(size: Int = 112) {
    Box(
        Modifier
            .size(size.dp)
            .background(colorResource(R.color.icon_background), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(size.dp),
        )
    }
}

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val context = LocalContext.current
    val persian = isPersian()
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {
                changeLanguage(context, if (persian) LocaleManager.ENGLISH else LocaleManager.PERSIAN)
            }) {
                Text(stringResource(R.string.language_switch_short))
            }
        }
        Spacer(Modifier.weight(1f))
        AppLogo()
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        NoteText(stringResource(R.string.welcome_privacy_note))
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.welcome_start), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun RoleScreen(onBack: () -> Unit, onHer: () -> Unit, onPartner: () -> Unit) {
    Scaffold(topBar = { KenaretTopBar(title = "", onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.role_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.role_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            RoleCard(Icons.Filled.Person, stringResource(R.string.role_her), stringResource(R.string.role_her_hint), onHer)
            RoleCard(Icons.Filled.Favorite, stringResource(R.string.role_partner), stringResource(R.string.role_partner_hint), onPartner)
        }
    }
}

@Composable
private fun RoleCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Requests the notification permission (Android 13+) only when it is actually needed, then continues. */
@Composable
fun rememberNotificationPermissionThen(): (() -> Unit) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pending?.invoke()
        pending = null
    }
    return { next ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.hasPermission(context)) {
            pending = next
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            next()
        }
    }
}

@Composable
fun HerSetupScreen(
    today: LocalDate,
    onBack: () -> Unit,
    onFinish: (LocalDate, Int, Int, Boolean) -> Unit,
) {
    val persian = isPersian()
    var lastStart by rememberSaveable { mutableStateOf(today.minusDays(7).toEpochDay()) }
    var cycleLength by rememberSaveable { mutableFloatStateOf(CycleCalculator.DEFAULT_CYCLE.toFloat()) }
    var periodLength by rememberSaveable { mutableFloatStateOf(CycleCalculator.DEFAULT_PERIOD.toFloat()) }
    var unknown by rememberSaveable { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val requestPermissionThen = rememberNotificationPermissionThen()

    Scaffold(topBar = { KenaretTopBar(title = stringResource(R.string.setup_title), onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionCard(title = stringResource(R.string.setup_last_period)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showPicker = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(12.dp))
                    Text(
                        Fmt.fullDate(LocalDate.ofEpochDay(lastStart), persian),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(stringResource(R.string.action_change), color = MaterialTheme.colorScheme.primary)
                }
            }

            SectionCard(title = stringResource(R.string.setup_cycle_length)) {
                Text(
                    stringResource(R.string.days_count, if (unknown) CycleCalculator.DEFAULT_CYCLE else cycleLength.roundToInt()),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (unknown) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                )
                Slider(
                    value = cycleLength,
                    onValueChange = { cycleLength = it },
                    valueRange = CycleCalculator.MIN_CYCLE.toFloat()..CycleCalculator.MAX_CYCLE.toFloat(),
                    steps = CycleCalculator.MAX_CYCLE - CycleCalculator.MIN_CYCLE - 1,
                    enabled = !unknown,
                )
                Text(
                    stringResource(
                        R.string.setup_range_hint,
                        CycleCalculator.MIN_CYCLE,
                        CycleCalculator.MAX_CYCLE,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    Modifier.fillMaxWidth().clickable { unknown = !unknown },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = unknown, onCheckedChange = { unknown = it })
                    Text(stringResource(R.string.setup_unknown_length), style = MaterialTheme.typography.bodyMedium)
                }
                if (unknown) {
                    NoteText(stringResource(R.string.setup_unknown_explanation))
                }
            }

            SectionCard(title = stringResource(R.string.setup_period_length)) {
                Text(
                    stringResource(R.string.days_count, periodLength.roundToInt()),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Slider(
                    value = periodLength,
                    onValueChange = { periodLength = it },
                    valueRange = CycleCalculator.MIN_PERIOD.toFloat()..CycleCalculator.MAX_PERIOD.toFloat(),
                    steps = CycleCalculator.MAX_PERIOD - CycleCalculator.MIN_PERIOD - 1,
                )
                Text(
                    stringResource(R.string.setup_range_hint, CycleCalculator.MIN_PERIOD, CycleCalculator.MAX_PERIOD),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            NoteText(stringResource(R.string.disclaimer_short))
            NoteText(stringResource(R.string.setup_notification_note))

            Button(
                onClick = {
                    if (saving) return@Button
                    saving = true
                    requestPermissionThen {
                        onFinish(LocalDate.ofEpochDay(lastStart), cycleLength.roundToInt(), periodLength.roundToInt(), unknown)
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(stringResource(R.string.action_continue), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showPicker) {
        KenaretDatePickerDialog(
            title = stringResource(R.string.setup_last_period),
            initial = LocalDate.ofEpochDay(lastStart),
            maxDate = today,
            onDismiss = { showPicker = false },
            onConfirm = {
                lastStart = it.toEpochDay()
                showPicker = false
            },
        )
    }
}

@Composable
fun PartnerSetupScreen(onBack: () -> Unit, onFinish: (PairingData?) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val requestPermissionThen = rememberNotificationPermissionThen()

    Scaffold(topBar = { KenaretTopBar(title = stringResource(R.string.partner_setup_title), onBack = onBack) }) { padding ->
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
            Text(
                stringResource(R.string.partner_setup_intro),
                style = MaterialTheme.typography.bodyLarge,
            )
            SectionCard(title = stringResource(R.string.partner_connect_title)) {
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
                        } else if (!saving) {
                            saving = true
                            requestPermissionThen { onFinish(data) }
                        }
                    },
                    enabled = code.isNotBlank() && !saving,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.partner_connect_button)) }
            }
            NoteText(stringResource(R.string.partner_setup_privacy))
            TextButton(
                onClick = {
                    if (!saving) {
                        saving = true
                        onFinish(null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.partner_setup_later)) }
        }
    }
}

@Composable
fun PairingCodeField(code: String, error: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = code,
        onValueChange = { onChange(it.take(16)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(stringResource(R.string.partner_code_label)) },
        placeholder = { Text("KNT-XXXX-XXX") },
        isError = error,
        supportingText = if (error) {
            { Text(stringResource(R.string.partner_code_invalid)) }
        } else null,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
    )
}
