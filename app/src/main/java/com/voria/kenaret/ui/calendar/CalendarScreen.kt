package com.voria.kenaret.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voria.kenaret.R
import com.voria.kenaret.cycle.DayInfo
import com.voria.kenaret.database.SymptomRecord
import com.voria.kenaret.domain.CyclePhase
import com.voria.kenaret.domain.Severity
import com.voria.kenaret.domain.SymptomType
import com.voria.kenaret.ui.KenaretUiState
import com.voria.kenaret.ui.MainViewModel
import com.voria.kenaret.ui.common.Fmt
import com.voria.kenaret.ui.common.MonthGrid
import com.voria.kenaret.ui.common.MonthHeader
import com.voria.kenaret.ui.common.MonthKey
import com.voria.kenaret.ui.common.NoteText
import com.voria.kenaret.ui.common.PhaseChip
import com.voria.kenaret.ui.common.SectionCard
import com.voria.kenaret.ui.common.isPersian
import com.voria.kenaret.ui.symptoms.severityLabelRes
import com.voria.kenaret.ui.symptoms.symptomLabelRes
import com.voria.kenaret.ui.theme.phaseColor
import java.time.LocalDate

@Composable
fun CalendarScreen(
    state: KenaretUiState,
    viewModel: MainViewModel,
    modifier: Modifier,
    onLogSymptoms: (LocalDate) -> Unit,
    showMessage: (String) -> Unit,
) {
    val persian = isPersian()
    var monthOffset by rememberSaveable { mutableStateOf(0) }
    var selectedEpoch by rememberSaveable { mutableStateOf(state.today.toEpochDay()) }
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    val month = MonthKey.of(state.today, persian).plus(monthOffset)
    val symptomDates = remember(state.symptoms) { state.symptoms.map { it.date }.toSet() }
    var confirmDelete by remember { mutableStateOf(false) }
    val loggedMessage = stringResource(R.string.period_logged)
    val overlapMessage = stringResource(R.string.period_log_overlap)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(stringResource(R.string.calendar_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 4.dp))
        }
        item {
            SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                MonthHeader(
                    month = month,
                    onPrevious = { monthOffset -= 1 },
                    onNext = { monthOffset += 1 },
                )
                MonthGrid(month) { date ->
                    DayCell(
                        date = date,
                        label = Fmt.dayOfMonth(date, month.jalali, persian),
                        info = state.dayInfo(date),
                        isToday = date == state.today,
                        isSelected = date == selected,
                        hasSymptoms = date in symptomDates,
                        onClick = { selectedEpoch = date.toEpochDay() },
                    )
                }
                if (monthOffset != 0) {
                    TextButton(onClick = {
                        monthOffset = 0
                        selectedEpoch = state.today.toEpochDay()
                    }) { Text(stringResource(R.string.calendar_go_today)) }
                }
            }
        }
        item { Legend() }
        item {
            val record = state.periods.firstOrNull { it.startDate == selected }
            DayDetails(
                date = selected,
                info = state.dayInfo(selected),
                symptoms = state.symptomsOn(selected),
                isFuture = selected.isAfter(state.today),
                isRecordedStart = record != null,
                onLogSymptoms = { onLogSymptoms(selected) },
                onStartPeriod = {
                    viewModel.logPeriodStart(selected) { ok -> showMessage(if (ok) loggedMessage else overlapMessage) }
                },
                onDeleteStart = { confirmDelete = true },
            )
        }
        item { NoteText(stringResource(R.string.calendar_estimate_note)) }
    }

    if (confirmDelete) {
        val record = state.periods.firstOrNull { it.startDate == selected }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.period_delete_title)) },
            text = { Text(stringResource(R.string.period_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    record?.let { viewModel.deletePeriod(it) }
                    confirmDelete = false
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    label: String,
    info: DayInfo?,
    isToday: Boolean,
    isSelected: Boolean,
    hasSymptoms: Boolean,
    onClick: () -> Unit,
) {
    val phase = info?.phase
    val menstrual = phaseColor(CyclePhase.MENSTRUAL)
    val background: Color = when {
        info == null -> Color.Transparent
        info.isPeriod && !info.isPredicted -> menstrual
        phase != null -> phaseColor(phase).copy(alpha = if (info.isPredicted) 0.10f else 0.18f)
        else -> Color.Transparent
    }
    val textColor = when {
        info?.isPeriod == true && !info.isPredicted -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    var modifier = Modifier
        .padding(2.dp)
        .fillMaxSize()
        .clip(CircleShape)
        .background(background)
    if (info?.isPeriod == true && info.isPredicted) {
        modifier = modifier.border(1.5.dp, menstrual, CircleShape)
    }
    if (isToday) {
        modifier = modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    }
    if (isSelected) {
        modifier = modifier.border(if (isToday) 3.dp else 2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
    }

    Box(modifier.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
        if (hasSymptoms) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(5.dp)
                    .background(if (info?.isPeriod == true && !info.isPredicted) Color.White else MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
private fun Legend() {
    val items = listOf(
        CyclePhase.MENSTRUAL to R.string.legend_period,
        CyclePhase.FOLLICULAR to R.string.phase_follicular,
        CyclePhase.OVULATION to R.string.legend_ovulation,
        CyclePhase.LUTEAL to R.string.phase_luteal,
    )
    SectionCard {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { (phase, label) ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(phaseColor(phase), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).border(1.5.dp, phaseColor(CyclePhase.MENSTRUAL), CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.legend_predicted_period), style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Spacer(Modifier.width(11.dp))
            Text(stringResource(R.string.legend_symptoms), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DayDetails(
    date: LocalDate,
    info: DayInfo?,
    symptoms: List<SymptomRecord>,
    isFuture: Boolean,
    isRecordedStart: Boolean,
    onLogSymptoms: () -> Unit,
    onStartPeriod: () -> Unit,
    onDeleteStart: () -> Unit,
) {
    val persian = isPersian()
    SectionCard(title = Fmt.fullDate(date, persian)) {
        if (info != null) {
            Text(
                stringResource(R.string.cycle_day_n, info.cycleDay),
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhaseChip(info.phase)
                if (info.isPredicted) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.calendar_predicted),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (info.isPeriod) {
                Text(
                    stringResource(if (info.isPredicted) R.string.calendar_period_predicted else R.string.calendar_period_day),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (info.isLate) {
                Text(stringResource(R.string.calendar_late), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Text(stringResource(R.string.calendar_no_info), style = MaterialTheme.typography.bodyMedium)
        }

        val logged = symptoms.filter { it.type != SymptomRecord.TYPE_NOTE }
        val note = symptoms.firstOrNull { it.type == SymptomRecord.TYPE_NOTE }?.note
        if (logged.isNotEmpty() || note != null) {
            Spacer(Modifier.size(4.dp))
            Text(stringResource(R.string.today_symptoms_title), style = MaterialTheme.typography.titleSmall)
            logged.forEach { record ->
                val type = SymptomType.from(record.type) ?: return@forEach
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(symptomLabelRes(type)), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(severityLabelRes(Severity.fromLevel(record.severity))),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (note != null) {
                Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (!isFuture) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onLogSymptoms, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_log_symptoms))
                }
                if (isRecordedStart) {
                    OutlinedButton(onClick = onDeleteStart, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_delete_period_start))
                    }
                } else {
                    OutlinedButton(onClick = onStartPeriod, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_period_started_this_day))
                    }
                }
            }
        }
    }
}
