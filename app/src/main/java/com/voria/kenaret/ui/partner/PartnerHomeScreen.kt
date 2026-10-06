package com.voria.kenaret.ui.partner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voria.kenaret.R
import com.voria.kenaret.content.AdviceCatalog
import com.voria.kenaret.cycle.CycleStatus
import com.voria.kenaret.database.PartnerConnection
import com.voria.kenaret.domain.Audience
import com.voria.kenaret.domain.CyclePhase
import com.voria.kenaret.ui.KenaretUiState
import com.voria.kenaret.ui.Routes
import com.voria.kenaret.ui.common.Fmt
import com.voria.kenaret.ui.common.MarkedLine
import com.voria.kenaret.ui.common.NoteText
import com.voria.kenaret.ui.common.PhaseChip
import com.voria.kenaret.ui.common.SectionCard
import com.voria.kenaret.ui.common.isPersian

@Composable
fun PartnerHomeScreen(state: KenaretUiState, modifier: Modifier, onNavigate: (String) -> Unit) {
    val persian = isPersian()
    val status = state.partnerStatus
    val partner = state.partner
    // Only use the phase for advice when she chose to share it.
    val sharedPhase: CyclePhase? = status?.phase?.takeIf { partner.sharePhase }
    val doList = stringArrayResource(R.array.partner_do)
    val dontList = stringArrayResource(R.array.partner_dont)
    val advice = remember(sharedPhase, state.today) {
        AdviceCatalog.dailyPicks(Audience.PARTNER, sharedPhase, state.today, count = 2)
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
                Text(stringResource(R.string.home_partner_greeting), style = MaterialTheme.typography.headlineSmall)
            }
        }

        item {
            if (status == null) {
                SectionCard(title = stringResource(R.string.partner_not_connected_title)) {
                    Text(stringResource(R.string.partner_not_connected_body), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { onNavigate(Routes.SETTINGS_PARTNER) }) {
                        Text(stringResource(R.string.partner_connect_button))
                    }
                }
            } else {
                StatusCard(status, partner, persian)
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.partner_help_title),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
            ) {
                doList.forEach { MarkedLine(it, positive = true) }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.partner_advice_title)) {
                advice.forEach { item ->
                    Text(stringResource(item.titleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(item.bodyRes), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.partner_avoid_title),
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            ) {
                dontList.forEach { MarkedLine(it, positive = false) }
            }
        }

        item {
            NoteText(stringResource(R.string.experience_differs_note))
        }
    }
}

@Composable
private fun StatusCard(status: CycleStatus, partner: PartnerConnection, persian: Boolean) {
    SectionCard(title = stringResource(R.string.partner_status_title), containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        val headline = when {
            status.isOnPeriod && (partner.sharePhase || partner.shareDaysUntil) ->
                stringResource(R.string.partner_status_on_period)
            partner.shareDaysUntil && status.daysUntilNextPeriod > 1 ->
                stringResource(R.string.partner_status_days, status.daysUntilNextPeriod)
            partner.shareDaysUntil && status.daysUntilNextPeriod == 1 ->
                stringResource(R.string.partner_status_tomorrow)
            partner.shareDaysUntil && status.daysUntilNextPeriod <= 0 ->
                stringResource(R.string.partner_status_today)
            else -> null
        }
        if (headline != null) {
            Text(headline, style = MaterialTheme.typography.titleLarge)
        }
        if (partner.shareNextPeriodDate) {
            Text(
                stringResource(R.string.next_period_date, Fmt.dayMonth(status.nextPeriodStart, persian)),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (partner.sharePhase) {
            PhaseChip(status.phase)
            Text(stringResource(partnerPhaseDescriptionRes(status.phase)), style = MaterialTheme.typography.bodyMedium)
        }
        NoteText(stringResource(R.string.partner_status_estimate_note))
    }
}

fun partnerPhaseDescriptionRes(phase: CyclePhase): Int = when (phase) {
    CyclePhase.MENSTRUAL -> R.string.phase_desc_partner_menstrual
    CyclePhase.FOLLICULAR -> R.string.phase_desc_partner_follicular
    CyclePhase.OVULATION -> R.string.phase_desc_partner_ovulation
    CyclePhase.LUTEAL -> R.string.phase_desc_partner_luteal
}
