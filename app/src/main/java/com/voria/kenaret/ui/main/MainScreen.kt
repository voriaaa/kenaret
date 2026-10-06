package com.voria.kenaret.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.voria.kenaret.R
import com.voria.kenaret.domain.Role
import com.voria.kenaret.domain.Severity
import com.voria.kenaret.ui.KenaretUiState
import com.voria.kenaret.ui.MainViewModel
import com.voria.kenaret.ui.calendar.CalendarScreen
import com.voria.kenaret.ui.her.HerHomeScreen
import com.voria.kenaret.ui.insights.InsightsScreen
import com.voria.kenaret.ui.learn.LearnScreen
import com.voria.kenaret.ui.partner.PartnerHomeScreen
import com.voria.kenaret.ui.settings.SettingsScreen
import com.voria.kenaret.ui.symptoms.SymptomSheet
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class Tab(val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.tab_home, Icons.Filled.Home),
    CALENDAR(R.string.tab_calendar, Icons.Filled.DateRange),
    INSIGHTS(R.string.tab_insights, Icons.Filled.Star),
    LEARN(R.string.tab_learn, Icons.AutoMirrored.Filled.List),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings),
}

@Composable
fun MainScreen(
    state: KenaretUiState,
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit,
    onDataDeleted: () -> Unit,
) {
    val role = state.role ?: Role.HER
    val tabs = if (role == Role.HER) {
        listOf(Tab.HOME, Tab.CALENDAR, Tab.INSIGHTS, Tab.SETTINGS)
    } else {
        listOf(Tab.HOME, Tab.LEARN, Tab.SETTINGS)
    }
    var selectedName by rememberSaveable { mutableStateOf(Tab.HOME.name) }
    val selected = tabs.firstOrNull { it.name == selectedName } ?: Tab.HOME

    var symptomDate by remember { mutableStateOf<LocalDate?>(null) }
    var showSevereAdvice by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selected,
                        onClick = { selectedName = tab.name },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (selected) {
            Tab.HOME -> if (role == Role.HER) {
                HerHomeScreen(
                    state = state,
                    viewModel = viewModel,
                    modifier = modifier,
                    onLogSymptoms = { symptomDate = it },
                    showMessage = showMessage,
                )
            } else {
                PartnerHomeScreen(state = state, modifier = modifier, onNavigate = onNavigate)
            }
            Tab.CALENDAR -> CalendarScreen(
                state = state,
                viewModel = viewModel,
                modifier = modifier,
                onLogSymptoms = { symptomDate = it },
                showMessage = showMessage,
            )
            Tab.INSIGHTS -> InsightsScreen(state = state, viewModel = viewModel, modifier = modifier, showMessage = showMessage)
            Tab.LEARN -> LearnScreen(modifier = modifier)
            Tab.SETTINGS -> SettingsScreen(
                state = state,
                viewModel = viewModel,
                modifier = modifier,
                onNavigate = onNavigate,
                onDataDeleted = onDataDeleted,
            )
        }
    }

    symptomDate?.let { date ->
        SymptomSheet(
            date = date,
            existing = state.symptomsOn(date),
            onDismiss = { symptomDate = null },
            onSave = { values, note ->
                viewModel.saveSymptoms(date, values, note)
                symptomDate = null
                if (values.values.any { it == Severity.SEVERE }) showSevereAdvice = true
            },
        )
    }

    if (showSevereAdvice) {
        AlertDialog(
            onDismissRequest = { showSevereAdvice = false },
            title = { Text(stringResource(R.string.severe_title)) },
            text = { Text(stringResource(R.string.disclaimer_severe)) },
            confirmButton = {
                TextButton(onClick = { showSevereAdvice = false }) { Text(stringResource(R.string.action_ok)) }
            },
        )
    }
}
