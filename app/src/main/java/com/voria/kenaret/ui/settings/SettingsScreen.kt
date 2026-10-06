package com.voria.kenaret.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.voria.kenaret.R
import com.voria.kenaret.domain.Role
import com.voria.kenaret.domain.ThemeMode
import com.voria.kenaret.settings.LocaleManager
import com.voria.kenaret.ui.KenaretUiState
import com.voria.kenaret.ui.MainViewModel
import com.voria.kenaret.ui.Routes
import com.voria.kenaret.ui.changeLanguage
import com.voria.kenaret.ui.common.SettingsRow

@Composable
fun SettingsScreen(
    state: KenaretUiState,
    viewModel: MainViewModel,
    modifier: Modifier,
    onNavigate: (String) -> Unit,
    onDataDeleted: () -> Unit,
) {
    val context = LocalContext.current
    var showLanguage by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val isHer = state.role == Role.HER
    val language = LocaleManager.language(context)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                stringResource(R.string.tab_settings),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        item {
            SettingsGroup {
                if (isHer) {
                    SettingsRow(stringResource(R.string.settings_cycle), stringResource(R.string.settings_cycle_hint), Icons.Filled.DateRange) {
                        onNavigate(Routes.SETTINGS_CYCLE)
                    }
                }
                SettingsRow(stringResource(R.string.settings_notifications), stringResource(R.string.settings_notifications_hint), Icons.Filled.Notifications) {
                    onNavigate(Routes.SETTINGS_NOTIFICATIONS)
                }
                SettingsRow(
                    stringResource(R.string.settings_partner),
                    stringResource(if (isHer) R.string.settings_partner_hint_her else R.string.settings_partner_hint_partner),
                    Icons.Filled.Favorite,
                ) { onNavigate(Routes.SETTINGS_PARTNER) }
                SettingsRow(stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_hint), Icons.Filled.Lock) {
                    onNavigate(Routes.SETTINGS_PRIVACY)
                }
            }
        }
        item {
            SettingsGroup {
                SettingsRow(
                    stringResource(R.string.settings_language),
                    stringResource(if (language == LocaleManager.ENGLISH) R.string.language_english else R.string.language_persian),
                    Icons.Filled.Edit,
                ) { showLanguage = true }
                SettingsRow(
                    stringResource(R.string.settings_theme),
                    stringResource(themeLabelRes(state.themeMode)),
                    Icons.Filled.Star,
                ) { showTheme = true }
            }
        }
        item {
            SettingsGroup {
                SettingsRow(stringResource(R.string.settings_about), null, Icons.Filled.Info) { onNavigate(Routes.ABOUT) }
                SettingsRow(stringResource(R.string.settings_disclaimer), null, Icons.Filled.Warning) { onNavigate(Routes.DISCLAIMER) }
                SettingsRow(
                    stringResource(R.string.settings_delete_all),
                    stringResource(R.string.settings_delete_all_hint),
                    Icons.Filled.Delete,
                    titleColor = MaterialTheme.colorScheme.error,
                ) { showDelete = true }
            }
        }
    }

    if (showLanguage) {
        ChoiceDialog(
            title = stringResource(R.string.settings_language),
            options = listOf(LocaleManager.PERSIAN to stringResource(R.string.language_persian), LocaleManager.ENGLISH to stringResource(R.string.language_english)),
            selected = language,
            onDismiss = { showLanguage = false },
            onSelect = {
                showLanguage = false
                if (it != language) changeLanguage(context, it)
            },
        )
    }
    if (showTheme) {
        ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { it.name to stringResource(themeLabelRes(it)) },
            selected = state.themeMode.name,
            onDismiss = { showTheme = false },
            onSelect = {
                showTheme = false
                viewModel.setThemeMode(ThemeMode.from(it))
            },
        )
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

fun themeLabelRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Composable
fun SettingsGroup(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) { content() }
    }
}

@Composable
fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (key, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(key) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = key == selected, onClick = { onSelect(key) })
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
fun DeleteAllDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_all_title)) },
        text = { Text(stringResource(R.string.delete_all_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete_all_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
