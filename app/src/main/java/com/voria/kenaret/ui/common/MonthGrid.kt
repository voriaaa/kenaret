package com.voria.kenaret.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.voria.kenaret.R
import java.time.LocalDate

/** Month header with previous / next buttons. Arrows mirror automatically in RTL. */
@Composable
fun MonthHeader(month: MonthKey, onPrevious: () -> Unit, onNext: () -> Unit, nextEnabled: Boolean = true) {
    val persian = isPersian()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.action_previous_month))
        }
        Text(
            Fmt.monthTitle(month, persian),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, enabled = nextEnabled) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.action_next_month))
        }
    }
}

/**
 * A 7-column month grid. [dayContent] draws each day; empty cells are left blank.
 * Uses the Jalali calendar (week starts Saturday) for Persian and Gregorian for English.
 */
@Composable
fun MonthGrid(month: MonthKey, dayContent: @Composable (LocalDate) -> Unit) {
    val firstDayOfWeek = Fmt.firstDayOfWeek(month.jalali)
    val days = month.days()
    val offset = (days.first().dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val cells: List<LocalDate?> = List(offset) { null } + days
    val rows = cells.chunked(7)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Fmt.weekdayInitials(month.jalali).forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        rows.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val date = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (date != null) dayContent(date)
                    }
                }
            }
        }
    }
}

/**
 * Date picker that follows the app language (Jalali for Persian). Future dates after [maxDate] are disabled.
 */
@Composable
fun KenaretDatePickerDialog(
    title: String,
    initial: LocalDate,
    maxDate: LocalDate?,
    minDate: LocalDate? = null,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val persian = isPersian()
    var selected by remember { mutableStateOf(initial) }
    var month by remember { mutableStateOf(MonthKey.of(initial, persian)) }
    val maxMonth = maxDate?.let { MonthKey.of(it, persian) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    Fmt.fullDate(selected, persian),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                MonthHeader(
                    month = month,
                    onPrevious = { month = month.plus(-1) },
                    onNext = { month = month.plus(1) },
                    nextEnabled = maxMonth == null || month != maxMonth,
                )
                MonthGrid(month) { date ->
                    val enabled = (maxDate == null || !date.isAfter(maxDate)) && (minDate == null || !date.isBefore(minDate))
                    val isSelected = date == selected
                    Box(
                        Modifier
                            .padding(2.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier
                            )
                            .clickable(enabled = enabled) { selected = date },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            Fmt.dayOfMonth(date, month.jalali, persian),
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
