package com.voria.kenaret.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.voria.kenaret.R
import com.voria.kenaret.cycle.CycleCalculator
import com.voria.kenaret.database.PeriodRecord
import com.voria.kenaret.domain.CyclePhase
import com.voria.kenaret.ui.KenaretUiState
import com.voria.kenaret.ui.MainViewModel
import com.voria.kenaret.ui.common.Fmt
import com.voria.kenaret.ui.common.KenaretDatePickerDialog
import com.voria.kenaret.ui.common.NoteText
import com.voria.kenaret.ui.common.SectionCard
import com.voria.kenaret.ui.common.isPersian
import com.voria.kenaret.ui.theme.phaseColor
import java.time.LocalDate

@Composable
fun InsightsScreen(
    state: KenaretUiState,
    viewModel: MainViewModel,
    modifier: Modifier,
    showMessage: (String) -> Unit,
) {
    val persian = isPersian()
    val insights = state.insights
    var editing by remember { mutableStateOf<PeriodRecord?>(null) }
    var addPicker by remember { mutableStateOf(false) }
    val loggedMessage = stringResource(R.string.period_logged)
    val overlapMessage = stringResource(R.string.period_log_overlap)
    val conflictMessage = stringResource(R.string.period_update_conflict)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text(stringResource(R.string.insights_title), style = MaterialTheme.typography.headlineSmall) }

        item {
            SectionCard(title = stringResource(R.string.insights_summary_title)) {
                if (insights.completedCycles == 0) {
                    Text(stringResource(R.string.insights_not_enough), style = MaterialTheme.typography.bodyMedium)
                } else {
                    insights.averageLength?.let {
                        Text(stringResource(R.string.insights_average, it), style = MaterialTheme.typography.bodyLarge)
                    }
                    if (insights.recentCount >= 2 && insights.recentMin != null && insights.recentMax != null) {
                        val text = if (insights.recentMin == insights.recentMax) {
                            stringResource(R.string.insights_range_same, insights.recentCount, insights.recentMin)
                        } else {
                            stringResource(R.string.insights_range, insights.recentCount, insights.recentMin, insights.recentMax)
                        }
                        Text(text, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        stringResource(R.string.insights_cycles_recorded, insights.completedCycles),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                insights.averagePeriodLength?.let {
                    Text(stringResource(R.string.insights_period_average, it), style = MaterialTheme.typography.bodyMedium)
                }
                NoteText(stringResource(R.string.insights_statistical_note))
            }
        }

        if (insights.history.isNotEmpty()) {
            item {
                SectionCard(title = stringResource(R.string.insights_chart_title)) {
                    CycleBars(insights.history.takeLast(6), persian)
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.history_title)) {
                val sorted = state.periods.sortedByDescending { it.startDate }
                if (sorted.isEmpty()) {
                    Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyMedium)
                }
                sorted.forEachIndexed { index, record ->
                    if (index > 0) HorizontalDivider()
                    PeriodRow(record, persian, state.periodLength) { editing = record }
                }
                Button(onClick = { addPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_log_period))
                }
            }
        }
    }

    if (addPicker) {
        KenaretDatePickerDialog(
            title = stringResource(R.string.action_log_period),
            initial = state.today,
            maxDate = state.today,
            onDismiss = { addPicker = false },
            onConfirm = {
                addPicker = false
                viewModel.logPeriodStart(it) { ok -> showMessage(if (ok) loggedMessage else overlapMessage) }
            },
        )
    }

    editing?.let { record ->
        EditPeriodDialog(
            record = record,
            today = state.today,
            onDismiss = { editing = null },
            onSave = { start, end ->
                editing = null
                viewModel.updatePeriod(record, start, end) { ok -> if (!ok) showMessage(conflictMessage) }
            },
            onDelete = {
                editing = null
                viewModel.deletePeriod(record)
            },
        )
    }
}

@Composable
private fun CycleBars(history: List<Pair<LocalDate, Int>>, persian: Boolean) {
    val max = (history.maxOf { it.second }).coerceAtLeast(CycleCalculator.MAX_CYCLE)
    val color = phaseColor(CyclePhase.LUTEAL)
    Row(
        Modifier.fillMaxWidth().height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        history.forEach { (start, length) ->
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(Fmt.number(length, persian), style = MaterialTheme.typography.labelMedium)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(length / max.toFloat() * 0.75f)
                        .background(color.copy(alpha = 0.75f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                )
                Text(
                    Fmt.dayMonth(start, persian),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun PeriodRow(record: PeriodRecord, persian: Boolean, defaultPeriod: Int, onEdit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(Fmt.shortDate(record.startDate, persian), style = MaterialTheme.typography.bodyLarge)
            val end = record.endDate
            Text(
                if (end != null) {
                    stringResource(
                        R.string.history_period_length,
                        CycleCalculator.daysBetween(record.startDate, end) + 1,
                    )
                } else {
                    stringResource(R.string.history_end_unknown, defaultPeriod)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EditPeriodDialog(
    record: PeriodRecord,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalDate?) -> Unit,
    onDelete: () -> Unit,
) {
    val persian = isPersian()
    var start by remember(record.id) { mutableStateOf(record.startDate) }
    var end by remember(record.id) { mutableStateOf(record.endDate) }
    var picking by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.period_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.period_start_label), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { picking = "start" }) { Text(Fmt.fullDate(start, persian)) }
                Text(stringResource(R.string.period_end_label), style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { picking = "end" }) {
                        Text(end?.let { Fmt.fullDate(it, persian) } ?: stringResource(R.string.period_end_not_set))
                    }
                    if (end != null) {
                        TextButton(onClick = { end = null }) { Text(stringResource(R.string.action_clear)) }
                    }
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.action_delete_record), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(start, end?.takeIf { !it.isBefore(start) }) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    when (picking) {
        "start" -> KenaretDatePickerDialog(
            title = stringResource(R.string.period_start_label),
            initial = start,
            maxDate = today,
            onDismiss = { picking = null },
            onConfirm = {
                start = it
                picking = null
            },
        )
        "end" -> KenaretDatePickerDialog(
            title = stringResource(R.string.period_end_label),
            initial = end ?: start,
            maxDate = today,
            minDate = start,
            onDismiss = { picking = null },
            onConfirm = {
                end = it
                picking = null
            },
        )
    }
}
