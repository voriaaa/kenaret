package com.voria.kenaret.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.voria.kenaret.cycle.CycleCalculator
import com.voria.kenaret.database.KenaretDao
import com.voria.kenaret.database.NotificationPreference
import com.voria.kenaret.database.isConnected
import com.voria.kenaret.domain.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** What the planner needs, resolved from the database for the current role. */
data class ReminderSource(
    val role: Role,
    val lastPeriodStart: LocalDate,
    val cycleLength: Int,
    val periodLength: Int,
    val options: PlanOptions,
)

/**
 * Keeps scheduled reminders in sync with the cycle data.
 * Every change (new period, edited date, new cycle length, new settings) calls [reschedule], which
 * 1) cancels all pending reminders, 2) recalculates them, 3) schedules them again.
 * WorkManager persists the work, so reminders survive a device reboot; [RescheduleReceiver] and the
 * daily [RefreshWorker] re-plan as an extra safety net.
 */
class ReminderScheduler(private val context: Context, private val dao: KenaretDao) {
    private val mutex = Mutex()

    suspend fun reschedule() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(TAG_REMINDER).result.get()

            val prefs = dao.getNotificationPreference() ?: NotificationPreference()
            val source = resolveSource(prefs)
            if (!prefs.enabled || source == null) {
                workManager.cancelUniqueWork(REFRESH_WORK)
                return@withLock
            }

            val now = LocalDateTime.now()
            val planned = ReminderPlanner.plan(
                lastPeriodStart = source.lastPeriodStart,
                cycleLength = source.cycleLength,
                periodLength = source.periodLength,
                today = now.toLocalDate(),
                options = source.options,
            )
            val zone = ZoneId.systemDefault()
            planned.asSequence()
                .map { it to it.date.atTime(prefs.hour, prefs.minute) }
                .filter { (_, time) -> time.isAfter(now) }
                .take(MAX_SCHEDULED)
                .forEach { (reminder, time) ->
                    val delay = Duration.between(now.atZone(zone), time.atZone(zone)).toMillis()
                    val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(
                            workDataOf(
                                ReminderWorker.KEY_TYPE to reminder.type.name,
                                ReminderWorker.KEY_PHASE to reminder.phase.name,
                                ReminderWorker.KEY_DATE to reminder.date.toEpochDay(),
                            )
                        )
                        .addTag(TAG_REMINDER)
                        .build()
                    workManager.enqueue(request)
                }

            val refresh = PeriodicWorkRequestBuilder<RefreshWorker>(1, TimeUnit.DAYS).build()
            workManager.enqueueUniquePeriodicWork(REFRESH_WORK, ExistingPeriodicWorkPolicy.KEEP, refresh)
            Unit
        }
    }

    suspend fun cancelAll() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(TAG_REMINDER).result.get()
            workManager.cancelUniqueWork(REFRESH_WORK).result.get()
            Unit
        }
    }

    private suspend fun resolveSource(prefs: NotificationPreference): ReminderSource? {
        val profile = dao.getProfile() ?: return null
        if (!profile.onboardingCompleted) return null
        return when (Role.from(profile.role)) {
            Role.HER -> {
                val periods = dao.getPeriods()
                val last = periods.maxByOrNull { it.startDate } ?: return null
                val length = CycleCalculator.averageCycleLength(periods.map { it.startDate }, profile.cycleLength)
                ReminderSource(
                    role = Role.HER,
                    lastPeriodStart = last.startDate,
                    cycleLength = length,
                    periodLength = profile.periodLength,
                    options = PlanOptions(
                        periodReminders = prefs.periodReminders,
                        phaseReminders = prefs.phaseReminders,
                        educationalReminders = prefs.educationalReminders,
                    ),
                )
            }
            Role.PARTNER -> {
                val partner = dao.getPartner() ?: return null
                if (!partner.isConnected() || !prefs.partnerReminders) return null
                val sharesTiming = partner.shareNextPeriodDate || partner.shareDaysUntil
                ReminderSource(
                    role = Role.PARTNER,
                    lastPeriodStart = partner.partnerLastPeriodStart!!,
                    cycleLength = partner.partnerCycleLength!!,
                    periodLength = partner.partnerPeriodLength!!,
                    options = PlanOptions(
                        periodReminders = prefs.periodReminders && sharesTiming,
                        phaseReminders = prefs.phaseReminders && partner.sharePhase,
                        educationalReminders = prefs.educationalReminders && (partner.sharePhase || sharesTiming),
                    ),
                )
            }
            null -> null
        }
    }

    companion object {
        const val TAG_REMINDER = "kenaret_reminder"
        const val REFRESH_WORK = "kenaret_reminder_refresh"
        private const val MAX_SCHEDULED = 40
    }
}
