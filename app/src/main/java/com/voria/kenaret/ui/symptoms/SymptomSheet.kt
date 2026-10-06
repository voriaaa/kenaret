package com.voria.kenaret.ui.symptoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voria.kenaret.R
import com.voria.kenaret.database.SymptomRecord
import com.voria.kenaret.domain.Severity
import com.voria.kenaret.domain.SymptomType
import com.voria.kenaret.ui.common.Fmt
import com.voria.kenaret.ui.common.NoteText
import com.voria.kenaret.ui.common.isPersian
import java.time.LocalDate

fun symptomLabelRes(type: SymptomType): Int = when (type) {
    SymptomType.CRAMPS -> R.string.symptom_cramps
    SymptomType.HEADACHE -> R.string.symptom_headache
    SymptomType.BACK_PAIN -> R.string.symptom_back_pain
    SymptomType.FATIGUE -> R.string.symptom_fatigue
    SymptomType.BLOATING -> R.string.symptom_bloating
    SymptomType.BREAST_TENDERNESS -> R.string.symptom_breast_tenderness
    SymptomType.APPETITE -> R.string.symptom_appetite
    SymptomType.SLEEP -> R.string.symptom_sleep
    SymptomType.MOOD -> R.string.symptom_mood
    SymptomType.OTHER -> R.string.symptom_other
}

fun severityLabelRes(severity: Severity): Int = when (severity) {
    Severity.NONE -> R.string.severity_none
    Severity.MILD -> R.string.severity_mild
    Severity.MODERATE -> R.string.severity_moderate
    Severity.SEVERE -> R.string.severity_severe
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomSheet(
    date: LocalDate,
    existing: List<SymptomRecord>,
    onDismiss: () -> Unit,
    onSave: (Map<SymptomType, Severity>, String?) -> Unit,
) {
    val persian = isPersian()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val values = remember(date) {
        mutableStateMapOf<SymptomType, Severity>().apply {
            SymptomType.entries.forEach { put(it, Severity.NONE) }
            existing.forEach { record ->
                SymptomType.from(record.type)?.let { put(it, Severity.fromLevel(record.severity)) }
            }
        }
    }
    var note by remember(date) {
        mutableStateOf(existing.firstOrNull { it.type == SymptomRecord.TYPE_NOTE }?.note.orEmpty())
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column {
                    Text(stringResource(R.string.symptoms_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        Fmt.fullDate(date, persian),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(SymptomType.entries) { type ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(symptomLabelRes(type)), style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Severity.entries.forEachIndexed { index, severity ->
                            SegmentedButton(
                                selected = values[type] == severity,
                                onClick = { values[type] = severity },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = Severity.entries.size),
                                icon = {},
                            ) {
                                Text(
                                    stringResource(severityLabelRes(severity)),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(1000) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    label = { Text(stringResource(R.string.symptoms_note_label)) },
                )
            }
            item {
                NoteText(stringResource(R.string.symptoms_private_note))
            }
            item {
                Button(
                    onClick = { onSave(values.toMap(), note) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(52.dp),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
