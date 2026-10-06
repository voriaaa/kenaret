package com.voria.kenaret.ui.her

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.voria.kenaret.R
import com.voria.kenaret.content.AdviceCatalog
import com.voria.kenaret.cycle.CycleCalculator
import com.voria.kenaret.cycle.CycleStatus
import com.voria.kenaret.domain.Audience
import com.voria.kenaret.domain.CyclePhase
import com.voria.kenaret.domain.SymptomType
import com.voria.kenaret.database.SymptomRecord
import com.voria.kenaret.ui.KenaretUiState
import com.voria.kenaret.ui.MainViewModel
import com.voria.kenaret.ui.common.BulletLine
import com.voria.kenaret.ui.common.Fmt
import com.voria.kenaret.ui.common.KenaretDatePickerDialog
import com.voria.kenaret.ui.common.NoteText
import com.voria.kenaret.ui.common.PhaseChip
import com.voria.kenaret.ui.common.SectionCard
import com.voria.kenaret.ui.common.isPersian
import com.voria.kenaret.ui.symptoms.symptomLabelRes
import com.voria.kenaret.ui.symptoms.severityLabelRes
import com.voria.kenaret.ui.theme.LocalPhasePalette
import com.voria.kenaret.domain.Severity
import java.time.LocalDate

@Composable
fun HerHomeScreen(
    state: KenaretUiState,
    viewModel: MainViewModel,
    modifier: Modifier,
    onLogSymptoms: (LocalDate) -> Unit,
    showMessage: (String) -> Unit,
) {
    val persian = isPersian()
    val status = state.herStatus
    var showStartPicker by remember { mutableStateOf(false) }
    val loggedMessage = stringResource(R.string.period_logged)
    val overlapMessage = stringResource(R.string.period_log_overlap)
    val logStart: (LocalDate) -> Unit = { date ->
        viewModel.logPeriodStart(date) { ok -> showMessage(if (ok) loggedMessage else overlapMessage) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    Fmt.fullDate(state.today, persian),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.home_her_greeting), style = MaterialTheme.typography.headlineSmall)
            }
        }

        if (status == null) {
            item {
                SectionCard(title = stringResource(R.string.home_no_data_title)) {
                    Text(stringResource(R.string.home_no_data_body), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { showStartPicker = true }) { Text(stringResource(R.string.action_log_period)) }
                }
            }
        } else {
            item { CycleOverviewCard(status, persian) }
            item { PhaseCard(status, persian) }
            item {
                ActionsCard(
                    status = status,
                    canEndPeriod = status.isOnPeriod && state.lastPeriod?.endDate == null,
                    onPeriodStartedToday = { logStart(state.today) },
                    onPeriodStartedOtherDay = { showStartPicker = true },
                    onPeriodEnded = { state.lastPeriod?.let { viewModel.setPeriodEnd(it, state.today) } },
                    onLogSymptoms = { onLogSymptoms(state.today) },
                )
            }
            item { SelfCareCard(status.phase, state.today) }
        }

        val todaySymptoms = state.symptomsOn(state.today).filter { it.type != SymptomRecord.TYPE_NOTE }
        if (todaySymptoms.isNotEmpty()) {
            item { TodaySymptomsCard(todaySymptoms) }
        }

        item {
            NoteText(stringResource(R.string.disclaimer_short))
        }
    }

    if (showStartPicker) {
        KenaretDatePickerDialog(
            title = stringResource(R.string.action_log_period),
            initial = state.today,
            maxDate = state.today,
            onDismiss = { showStartPicker = false },
            onConfirm = {
                showStartPicker = false
                logStart(it)
            },
        )
    }
}

@Composable
private fun CycleOverviewCard(status: CycleStatus, persian: Boolean) {
    SectionCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CycleRing(status, Modifier.size(220.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.cycle_day_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    Fmt.number(status.cycleDay, persian),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.cycle_of_length, status.cycleLength),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            nextPeriodText(status),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.next_period_date, Fmt.dayMonth(status.nextPeriodStart, persian)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun nextPeriodText(status: CycleStatus): String = when {
    status.isOnPeriod -> stringResource(R.string.on_period_day, status.cycleDay)
    status.daysUntilNextPeriod > 1 -> stringResource(R.string.next_period_in_days, status.daysUntilNextPeriod)
    status.daysUntilNextPeriod == 1 -> stringResource(R.string.next_period_tomorrow)
    status.daysUntilNextPeriod == 0 -> stringResource(R.string.next_period_today)
    else -> stringResource(R.string.next_period_late, -status.daysUntilNextPeriod)
}

/** A ring split into estimated phases, with a marker for today. Drawn clockwise in LTR, mirrored in RTL. */
@Composable
fun CycleRing(status: CycleStatus, modifier: Modifier = Modifier) {
    val palette = LocalPhasePalette.current
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val marker = MaterialTheme.colorScheme.onSurface
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val ranges = CycleCalculator.phaseRanges(status.cycleLength, status.periodLength)
    val segments = listOf(
        ranges.menstrual to palette.menstrual,
        ranges.follicular to palette.follicular,
        ranges.ovulation to palette.ovulation,
        ranges.luteal to palette.luteal,
    ).filter { !it.first.isEmpty() }

    Canvas(modifier) {
        val strokeWidth = 18.dp.toPx()
        val inset = strokeWidth / 2 + 6.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        val total = status.cycleLength.toFloat()
        val direction = if (rtl) -1f else 1f
        val gap = 2.5f

        drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(strokeWidth))
        segments.forEach { (range, color) ->
            val start = -90f + direction * ((range.first - 1) / total * 360f + gap / 2)
            val sweep = direction * ((range.last - range.first + 1) / total * 360f - gap)
            drawArc(color.copy(alpha = 0.85f), start, sweep, false, topLeft, arcSize, style = Stroke(strokeWidth))
        }
        val day = status.cycleDay.coerceIn(1, status.cycleLength)
        val angle = Math.toRadians((-90f + direction * ((day - 0.5f) / total * 360f)).toDouble())
        val radius = arcSize.width / 2
        val center = Offset(size.width / 2, size.height / 2)
        val point = Offset(
            center.x + radius * kotlin.math.cos(angle).toFloat(),
            center.y + radius * kotlin.math.sin(angle).toFloat(),
        )
        drawCircle(marker, radius = strokeWidth * 0.55f, center = point)
        drawCircle(track, radius = strokeWidth * 0.3f, center = point)
    }
}

@Composable
private fun PhaseCard(status: CycleStatus, persian: Boolean) {
    SectionCard(title = stringResource(R.string.current_phase_title)) {
        PhaseChip(status.phase)
        Text(stringResource(herPhaseDescriptionRes(status.phase)), style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(
                R.string.ovulation_window_range,
                Fmt.dayMonth(status.ovulationWindowStart, persian),
                Fmt.dayMonth(status.ovulationWindowEnd, persian),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun herPhaseDescriptionRes(phase: CyclePhase): Int = when (phase) {
    CyclePhase.MENSTRUAL -> R.string.phase_desc_her_menstrual
    CyclePhase.FOLLICULAR -> R.string.phase_desc_her_follicular
    CyclePhase.OVULATION -> R.string.phase_desc_her_ovulation
    CyclePhase.LUTEAL -> R.string.phase_desc_her_luteal
}

@Composable
private fun ActionsCard(
    status: CycleStatus,
    canEndPeriod: Boolean,
    onPeriodStartedToday: () -> Unit,
    onPeriodStartedOtherDay: () -> Unit,
    onPeriodEnded: () -> Unit,
    onLogSymptoms: () -> Unit,
) {
    SectionCard(title = stringResource(R.string.actions_title)) {
        if (status.cycleDay > 3) {
            Button(onClick = onPeriodStartedToday, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_period_started_today))
            }
        }
        if (canEndPeriod) {
            OutlinedButton(onClick = onPeriodEnded, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_period_ended_today))
            }
        }
        FilledTonalButton(onClick = onLogSymptoms, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_log_symptoms_today))
        }
        TextButton(onClick = onPeriodStartedOtherDay, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_period_started_other_day))
        }
    }
}

@Composable
private fun SelfCareCard(phase: CyclePhase, today: LocalDate) {
    val care = stringArrayResource(R.array.her_daily_care)
    val advice = remember(phase, today) { AdviceCatalog.dailyPicks(Audience.HER, phase, today, count = 2) }
    SectionCard(
        title = stringResource(R.string.self_care_title),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
    ) {
        care.forEach { BulletLine(it) }
        advice.forEach { item ->
            Spacer(Modifier.height(4.dp))
            Text(stringResource(item.titleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(stringResource(item.bodyRes), style = MaterialTheme.typography.bodyMedium)
        }
        NoteText(stringResource(R.string.advice_general_note))
    }
}

@Composable
private fun TodaySymptomsCard(records: List<SymptomRecord>) {
    SectionCard(title = stringResource(R.string.today_symptoms_title)) {
        records.forEach { record ->
            val type = SymptomType.from(record.type) ?: return@forEach
            Row(Modifier.fillMaxWidth()) {
                Text(stringResource(symptomLabelRes(type)), modifier = Modifier.weight(1f))
                Text(
                    stringResource(severityLabelRes(Severity.fromLevel(record.severity))),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (records.any { it.severity >= Severity.SEVERE.level }) {
            NoteText(stringResource(R.string.disclaimer_severe))
        }
    }
}
