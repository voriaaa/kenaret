package com.voria.kenaret.data

import android.content.Context
import com.voria.kenaret.cycle.CycleCalculator
import com.voria.kenaret.database.AppSettings
import com.voria.kenaret.database.KenaretDao
import com.voria.kenaret.database.KenaretDatabase
import com.voria.kenaret.database.NotificationPreference
import com.voria.kenaret.database.PartnerConnection
import com.voria.kenaret.database.PeriodRecord
import com.voria.kenaret.database.SymptomRecord
import com.voria.kenaret.database.UserProfile
import com.voria.kenaret.domain.Role
import com.voria.kenaret.domain.Severity
import com.voria.kenaret.domain.SymptomType
import com.voria.kenaret.domain.ThemeMode
import com.voria.kenaret.notifications.NotificationHelper
import com.voria.kenaret.notifications.ReminderScheduler
import com.voria.kenaret.pairing.PairingData
import com.voria.kenaret.settings.LocaleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Single source of truth. Everything is stored locally (Room); nothing is sent anywhere.
 * Any change that affects the cycle estimate re-plans the reminders.
 */
class KenaretRepository(
    private val context: Context,
    private val database: KenaretDatabase,
    private val dao: KenaretDao,
    private val scheduler: ReminderScheduler,
) {
    val profile: Flow<UserProfile?> = dao.observeProfile()
    val periods: Flow<List<PeriodRecord>> = dao.observePeriods()
    val symptoms: Flow<List<SymptomRecord>> = dao.observeSymptoms()
    val partner: Flow<PartnerConnection> = dao.observePartner().map { it ?: PartnerConnection() }
    val notificationPreference: Flow<NotificationPreference> =
        dao.observeNotificationPreference().map { it ?: NotificationPreference() }
    val settings: Flow<AppSettings> = dao.observeSettings().map { it ?: AppSettings() }

    // ---------- Onboarding ----------

    suspend fun completeHerOnboarding(lastPeriodStart: LocalDate, cycleLength: Int, periodLength: Int, unknownLength: Boolean) {
        dao.upsertProfile(
            UserProfile(
                role = Role.HER.name,
                cycleLength = if (unknownLength) CycleCalculator.DEFAULT_CYCLE else CycleCalculator.clampCycle(cycleLength),
                periodLength = CycleCalculator.clampPeriod(periodLength),
                cycleLengthUnknown = unknownLength,
                onboardingCompleted = true,
            )
        )
        dao.insertPeriod(PeriodRecord(startDate = lastPeriodStart))
        ensureDefaults()
        scheduler.reschedule()
    }

    suspend fun completePartnerOnboarding(data: PairingData?) {
        dao.upsertProfile(UserProfile(role = Role.PARTNER.name, onboardingCompleted = true))
        ensureDefaults()
        if (data != null) savePartnerData(data) else scheduler.reschedule()
    }

    private suspend fun ensureDefaults() {
        if (dao.getNotificationPreference() == null) dao.upsertNotificationPreference(NotificationPreference())
        if (dao.getPartner() == null) dao.upsertPartner(PartnerConnection())
    }

    // ---------- Periods ----------

    /** Logs a new period start. Returns false when it overlaps an existing record. */
    suspend fun logPeriodStart(date: LocalDate): Boolean {
        val existing = dao.getPeriods()
        if (existing.any { it.startDate == date }) return false
        val profile = dao.getProfile()
        val periodLength = profile?.periodLength ?: CycleCalculator.DEFAULT_PERIOD
        val overlaps = existing.any { record ->
            val end = record.endDate ?: record.startDate.plusDays((periodLength - 1).toLong())
            !date.isBefore(record.startDate) && !date.isAfter(end)
        }
        if (overlaps) return false
        dao.insertPeriod(PeriodRecord(startDate = date))
        scheduler.reschedule()
        return true
    }

    suspend fun updatePeriod(record: PeriodRecord, newStart: LocalDate, newEnd: LocalDate?): Boolean {
        val others = dao.getPeriods().filter { it.id != record.id }
        if (others.any { it.startDate == newStart }) return false
        val end = newEnd?.takeIf { !it.isBefore(newStart) }
        dao.updatePeriod(record.copy(startDate = newStart, endDate = end))
        scheduler.reschedule()
        return true
    }

    suspend fun setPeriodEnd(record: PeriodRecord, end: LocalDate?) {
        dao.updatePeriod(record.copy(endDate = end?.takeIf { !it.isBefore(record.startDate) }))
        scheduler.reschedule()
    }

    suspend fun deletePeriod(record: PeriodRecord) {
        dao.deletePeriod(record.id)
        scheduler.reschedule()
    }

    // ---------- Symptoms ----------

    suspend fun saveSymptoms(date: LocalDate, values: Map<SymptomType, Severity>, note: String?) {
        dao.deleteSymptomsOn(date)
        val records = values.filterValues { it != Severity.NONE }.map { (type, severity) ->
            SymptomRecord(date = date, type = type.name, severity = severity.level)
        }.toMutableList()
        val cleanNote = note?.trim().orEmpty()
        if (cleanNote.isNotEmpty()) {
            records += SymptomRecord(date = date, type = SymptomRecord.TYPE_NOTE, severity = 0, note = cleanNote.take(1000))
        }
        if (records.isNotEmpty()) dao.insertSymptoms(records)
    }

    // ---------- Cycle settings ----------

    suspend fun updateCycleSettings(cycleLength: Int, periodLength: Int, unknownLength: Boolean) {
        val profile = dao.getProfile() ?: return
        dao.upsertProfile(
            profile.copy(
                cycleLength = if (unknownLength) CycleCalculator.DEFAULT_CYCLE else CycleCalculator.clampCycle(cycleLength),
                periodLength = CycleCalculator.clampPeriod(periodLength),
                cycleLengthUnknown = unknownLength,
            )
        )
        scheduler.reschedule()
    }

    // ---------- Partner ----------

    suspend fun updateSharing(sharePhase: Boolean, shareNextPeriodDate: Boolean, shareDaysUntil: Boolean) {
        val current = dao.getPartner() ?: PartnerConnection()
        dao.upsertPartner(
            current.copy(sharePhase = sharePhase, shareNextPeriodDate = shareNextPeriodDate, shareDaysUntil = shareDaysUntil)
        )
    }

    suspend fun savePartnerData(data: PairingData) {
        val current = dao.getPartner() ?: PartnerConnection()
        dao.upsertPartner(
            current.copy(
                sharePhase = data.sharePhase,
                shareNextPeriodDate = data.shareNextPeriodDate,
                shareDaysUntil = data.shareDaysUntil,
                partnerLastPeriodStart = data.lastPeriodStart,
                partnerCycleLength = data.cycleLength,
                partnerPeriodLength = data.periodLength,
                connectedOn = LocalDate.now(),
            )
        )
        scheduler.reschedule()
    }

    suspend fun disconnectPartner() {
        dao.upsertPartner(PartnerConnection())
        scheduler.reschedule()
    }

    // ---------- Settings ----------

    suspend fun updateNotificationPreference(preference: NotificationPreference) {
        dao.upsertNotificationPreference(preference)
        scheduler.reschedule()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dao.upsertSettings(AppSettings(themeMode = mode.name))
    }

    /** Deletes every piece of data on this device and cancels all reminders. */
    suspend fun deleteAllData() {
        scheduler.cancelAll()
        withContext(Dispatchers.IO) { database.clearAllTables() }
        NotificationHelper.cancelAll(context)
        LocaleManager.clear(context)
    }
}
