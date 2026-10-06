package com.voria.kenaret.notifications

import android.content.Context
import com.voria.kenaret.R
import com.voria.kenaret.content.AdviceCatalog
import com.voria.kenaret.domain.Audience
import com.voria.kenaret.domain.CyclePhase
import com.voria.kenaret.domain.Role
import java.time.LocalDate

/** Builds the text of a reminder for a given role. Different wording for her and for the partner. */
object ReminderContent {

    fun build(
        context: Context,
        role: Role,
        type: ReminderType,
        phase: CyclePhase,
        date: LocalDate,
        hideSensitive: Boolean,
    ): Pair<String, String> {
        if (hideSensitive) {
            return context.getString(R.string.app_name) to context.getString(R.string.notification_private_body)
        }
        val (titleRes, bodyRes) = when (role) {
            Role.HER -> when (type) {
                ReminderType.PERIOD_SOON -> R.string.notif_her_period_soon_title to R.string.notif_her_period_soon_body
                ReminderType.PERIOD_START -> R.string.notif_her_day1_title to R.string.notif_her_day1_body
                ReminderType.PHASE_FOLLICULAR -> R.string.notif_her_follicular_title to R.string.notif_her_follicular_body
                ReminderType.PHASE_OVULATION -> R.string.notif_her_ovulation_title to R.string.notif_her_ovulation_body
                ReminderType.PHASE_LUTEAL -> R.string.notif_her_luteal_title to R.string.notif_her_luteal_body
                ReminderType.EDUCATIONAL -> return educational(context, Audience.HER, phase, date)
            }
            Role.PARTNER -> when (type) {
                ReminderType.PERIOD_SOON -> R.string.notif_partner_period_soon_title to R.string.notif_partner_period_soon_body
                ReminderType.PERIOD_START -> R.string.notif_partner_day1_title to R.string.notif_partner_day1_body
                ReminderType.PHASE_FOLLICULAR -> R.string.notif_partner_follicular_title to R.string.notif_partner_follicular_body
                ReminderType.PHASE_OVULATION -> R.string.notif_partner_ovulation_title to R.string.notif_partner_ovulation_body
                ReminderType.PHASE_LUTEAL -> R.string.notif_partner_luteal_title to R.string.notif_partner_luteal_body
                ReminderType.EDUCATIONAL -> return educational(context, Audience.PARTNER, phase, date)
            }
        }
        return context.getString(titleRes) to context.getString(bodyRes)
    }

    private fun educational(context: Context, audience: Audience, phase: CyclePhase, date: LocalDate): Pair<String, String> {
        val advice = AdviceCatalog.dailyPicks(audience, phase, date, count = 1, salt = 3).firstOrNull()
            ?: return context.getString(R.string.app_name) to context.getString(R.string.notification_private_body)
        val title = context.getString(R.string.notif_tip_title, context.getString(advice.titleRes))
        return title to context.getString(advice.bodyRes)
    }
}
