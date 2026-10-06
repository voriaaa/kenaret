package com.voria.kenaret.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voria.kenaret.KenaretApp
import com.voria.kenaret.database.NotificationPreference
import com.voria.kenaret.domain.CyclePhase
import com.voria.kenaret.domain.Role
import com.voria.kenaret.settings.LocaleManager
import java.time.LocalDate

/** Shows one scheduled reminder. Re-reads the settings at fire time so the latest choices apply. */
class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? KenaretApp ?: return Result.success()
        val dao = app.container.dao
        val prefs = dao.getNotificationPreference() ?: NotificationPreference()
        if (!prefs.enabled) return Result.success()
        val profile = dao.getProfile() ?: return Result.success()
        val role = Role.from(profile.role) ?: return Result.success()
        if (role == Role.PARTNER && !prefs.partnerReminders) return Result.success()

        val type = runCatching { ReminderType.valueOf(inputData.getString(KEY_TYPE).orEmpty()) }.getOrNull()
            ?: return Result.success()
        val phase = runCatching { CyclePhase.valueOf(inputData.getString(KEY_PHASE).orEmpty()) }.getOrNull()
            ?: CyclePhase.LUTEAL
        val date = LocalDate.ofEpochDay(inputData.getLong(KEY_DATE, LocalDate.now().toEpochDay()))

        val localized = LocaleManager.wrap(applicationContext)
        val (title, body) = ReminderContent.build(localized, role, type, phase, date, prefs.hideSensitive)
        NotificationHelper.show(localized, NOTIFICATION_ID_BASE + (date.toEpochDay() % 1000).toInt(), title, body)
        return Result.success()
    }

    companion object {
        const val KEY_TYPE = "type"
        const val KEY_PHASE = "phase"
        const val KEY_DATE = "date"
        private const val NOTIFICATION_ID_BASE = 4200
    }
}
