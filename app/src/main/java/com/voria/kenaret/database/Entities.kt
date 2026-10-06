package com.voria.kenaret.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.voria.kenaret.cycle.CycleCalculator
import java.time.LocalDate

/** Single-row table describing how this device is used. */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val role: String,
    val cycleLength: Int = CycleCalculator.DEFAULT_CYCLE,
    val periodLength: Int = CycleCalculator.DEFAULT_PERIOD,
    val cycleLengthUnknown: Boolean = false,
    val onboardingCompleted: Boolean = false,
)

/** A recorded period. Each record starts a new cycle (cycle day 1 = [startDate]). */
@Entity(tableName = "period_records", indices = [Index(value = ["startDate"], unique = true)])
data class PeriodRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
)

/** One symptom on one day. Type [com.voria.kenaret.domain.SymptomType] or NOTE for a personal note. */
@Entity(tableName = "symptom_records", indices = [Index(value = ["date", "type"], unique = true)])
data class SymptomRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val type: String,
    val severity: Int,
    val note: String? = null,
) {
    companion object {
        const val TYPE_NOTE = "NOTE"
    }
}

/**
 * Partner connection.
 * On her device: what she allows to be shared.
 * On the partner's device: the data decoded from her pairing code.
 */
@Entity(tableName = "partner_connection")
data class PartnerConnection(
    @PrimaryKey val id: Int = 1,
    val sharePhase: Boolean = true,
    val shareNextPeriodDate: Boolean = true,
    val shareDaysUntil: Boolean = true,
    val partnerLastPeriodStart: LocalDate? = null,
    val partnerCycleLength: Int? = null,
    val partnerPeriodLength: Int? = null,
    val connectedOn: LocalDate? = null,
)

fun PartnerConnection.isConnected(): Boolean =
    partnerLastPeriodStart != null && partnerCycleLength != null && partnerPeriodLength != null

@Entity(tableName = "notification_preferences")
data class NotificationPreference(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean = true,
    val hour: Int = 8,
    val minute: Int = 30,
    val periodReminders: Boolean = true,
    val phaseReminders: Boolean = true,
    val partnerReminders: Boolean = true,
    val educationalReminders: Boolean = true,
    /** Replace sensitive wording with a neutral message. The lock-screen version is always neutral. */
    val hideSensitive: Boolean = false,
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val themeMode: String = "SYSTEM",
)
