package com.voria.kenaret.ui.learn

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.voria.kenaret.R
import com.voria.kenaret.content.AdviceCatalog
import com.voria.kenaret.domain.AdviceCategory
import com.voria.kenaret.domain.Audience
import com.voria.kenaret.domain.CyclePhase
import com.voria.kenaret.ui.common.NoteText
import com.voria.kenaret.ui.common.PhaseChip
import com.voria.kenaret.ui.common.SectionCard
import com.voria.kenaret.ui.common.phaseNameRes

fun categoryLabelRes(category: AdviceCategory): Int = when (category) {
    AdviceCategory.SELF_CARE -> R.string.category_self_care
    AdviceCategory.EMOTIONAL_SUPPORT -> R.string.category_emotional
    AdviceCategory.PRACTICAL_SUPPORT -> R.string.category_practical
    AdviceCategory.COMMUNICATION -> R.string.category_communication
    AdviceCategory.EDUCATION -> R.string.category_education
}

/** Advice library for the partner, filterable by estimated phase. */
@Composable
fun LearnScreen(modifier: Modifier, audience: Audience = Audience.PARTNER) {
    var phaseName by rememberSaveable { mutableStateOf<String?>(null) }
    val phase = phaseName?.let { CyclePhase.valueOf(it) }
    val adviceList = AdviceCatalog.forPhase(audience, phase)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(stringResource(R.string.learn_title), style = MaterialTheme.typography.headlineSmall) }
        item {
            Text(
                stringResource(R.string.learn_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = phase == null, onClick = { phaseName = null }, label = { Text(stringResource(R.string.filter_all)) })
                CyclePhase.entries.forEach { p ->
                    FilterChip(
                        selected = phase == p,
                        onClick = { phaseName = p.name },
                        label = { Text(stringResource(phaseNameRes(p))) },
                    )
                }
            }
        }
        items(adviceList, key = { it.id }) { advice ->
            SectionCard {
                Text(
                    stringResource(categoryLabelRes(advice.category)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(advice.titleRes), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(advice.bodyRes), style = MaterialTheme.typography.bodyMedium)
                advice.phase?.let { PhaseChip(it) }
            }
        }
        item { NoteText(stringResource(R.string.experience_differs_note)) }
    }
}
