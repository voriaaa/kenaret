package com.voria.kenaret.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.voria.kenaret.KenaretApp
import com.voria.kenaret.cycle.CycleCalculator
import com.voria.kenaret.cycle.CycleInsights
import com.voria.kenaret.cycle.CycleStats
import com.voria.kenaret.cycle.CycleStatus
import com.voria.kenaret.cycle.DayInfo
import com.voria.kenaret.data.KenaretRepository
import com.voria.kenaret.database.AppSettings
import com.voria.kenaret.database.NotificationPreference
import com.voria.kenaret.database.PartnerConnection
import com.voria.kenaret.database.PeriodRecord
import com.voria.kenaret.database.SymptomRecord
import com.voria.kenaret.database.UserProfile
import com.voria.kenaret.database.isConnected
import com.voria.kenaret.domain.PeriodSpan
import com.voria.kenaret.domain.Role
import com.voria.kenaret.domain.Severity
import com.voria.kenaret.domain.SymptomType
import com.voria.kenaret.domain.ThemeMode
import com.voria.kenaret.pairing.PairingCode
import com.voria.kenaret.pairing.PairingData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Everything the screens need, derived from the local database. */
data class KenaretUiState(
    val loaded: Boolean = false,
    val profile: UserProfile? = null,
    val periods: List<PeriodRecord> = emptyList(),
    val symptoms: List<SymptomRecord> = emptyList(),
    val partner: PartnerConnection = PartnerConnection(),
    val notificationPreference: NotificationPreference = NotificationPreference(),
    val settings: AppSettings = AppSettings(),
    val today: LocalDate = LocalDate.now(),
) {
    val role: Role? get() = Role.from(profile?.role)
    val onboarded: Boolean get() = profile?.onboardingCompleted == true
    val themeMode: ThemeMode get() = ThemeMode.from(settings.themeMode)

    val spans: List<PeriodSpan> get() = periods.map { PeriodSpan(it.startDate, it.endDate) }
    val lastPeriod: PeriodRecord? get() = periods.maxByOrNull { it.startDate }

    /** Average of recent recorded cycles, or the length she entered when there isn't enough data yet. */
    val effectiveCycleLength: Int
        get() = CycleCalculator.averageCycleLength(
            periods.map { it.startDate },
            profile?.cycleLength ?: CycleCalculator.DEFAULT_CYCLE,
        )

    val periodLength: Int get() = profile?.periodLength ?: CycleCalculator.DEFAULT_PERIOD

    val herStatus: CycleStatus?
        get() {
            val last = lastPeriod ?: return null
            val period = CycleCalculator.periodLengthOf(PeriodSpan(last.startDate, last.endDate), periodLength)
            return CycleCalculator.status(last.startDate, today, effectiveCycleLength, period, projectForward = false)
        }

    val partnerStatus: CycleStatus?
        get() {
            if (!partner.isConnected()) return null
            return CycleCalculator.status(
                partner.partnerLastPeriodStart!!,
                today,
                partner.partnerCycleLength!!,
                partner.partnerPeriodLength!!,
                projectForward = true,
            )
        }

    val insights: CycleInsights get() = CycleStats.insights(spans)

    fun dayInfo(date: LocalDate): DayInfo? = when (role) {
        Role.PARTNER -> {
            if (!partner.isConnected()) null
            else CycleCalculator.dayInfo(
                date,
                listOf(PeriodSpan(partner.partnerLastPeriodStart!!, null)),
                partner.partnerCycleLength!!,
                partner.partnerPeriodLength!!,
                today,
            )
        }
        else -> CycleCalculator.dayInfo(date, spans, effectiveCycleLength, periodLength, today)
    }

    fun symptomsOn(date: LocalDate): List<SymptomRecord> = symptoms.filter { it.date == date }

    /** Current pairing code for her device, or null when nothing is selected for sharing. */
    val pairingCode: String?
        get() {
            val last = lastPeriod ?: return null
            if (!partner.sharePhase && !partner.shareNextPeriodDate && !partner.shareDaysUntil) return null
            return PairingCode.encode(
                PairingData(
                    lastPeriodStart = last.startDate,
                    cycleLength = effectiveCycleLength,
                    periodLength = periodLength,
                    sharePhase = partner.sharePhase,
                    shareNextPeriodDate = partner.shareNextPeriodDate,
                    shareDaysUntil = partner.shareDaysUntil,
                )
            )
        }
}

class MainViewModel(private val repository: KenaretRepository) : ViewModel() {

    private val todayFlow = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000)
        }
    }.distinctUntilChanged()

    private val core = combine(
        repository.profile,
        repository.periods,
        repository.symptoms,
        repository.partner,
        repository.notificationPreference,
    ) { profile, periods, symptoms, partner, prefs ->
        KenaretUiState(
            loaded = true,
            profile = profile,
            periods = periods,
            symptoms = symptoms,
            partner = partner,
            notificationPreference = prefs,
        )
    }

    val state: StateFlow<KenaretUiState> = combine(core, repository.settings, todayFlow) { base, settings, today ->
        base.copy(settings = settings, today = today)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, KenaretUiState())

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun completeHerOnboarding(lastStart: LocalDate, cycleLength: Int, periodLength: Int, unknown: Boolean, onDone: () -> Unit) =
        launch {
            repository.completeHerOnboarding(lastStart, cycleLength, periodLength, unknown)
            onDone()
        }

    fun completePartnerOnboarding(data: PairingData?, onDone: () -> Unit) = launch {
        repository.completePartnerOnboarding(data)
        onDone()
    }

    fun logPeriodStart(date: LocalDate, onResult: (Boolean) -> Unit = {}) = launch {
        onResult(repository.logPeriodStart(date))
    }

    fun updatePeriod(record: PeriodRecord, start: LocalDate, end: LocalDate?, onResult: (Boolean) -> Unit = {}) = launch {
        onResult(repository.updatePeriod(record, start, end))
    }

    fun setPeriodEnd(record: PeriodRecord, end: LocalDate?) = launch { repository.setPeriodEnd(record, end) }

    fun deletePeriod(record: PeriodRecord) = launch { repository.deletePeriod(record) }

    fun saveSymptoms(date: LocalDate, values: Map<SymptomType, Severity>, note: String?) =
        launch { repository.saveSymptoms(date, values, note) }

    fun updateCycleSettings(cycleLength: Int, periodLength: Int, unknown: Boolean) =
        launch { repository.updateCycleSettings(cycleLength, periodLength, unknown) }

    fun updateSharing(phase: Boolean, date: Boolean, days: Boolean) =
        launch { repository.updateSharing(phase, date, days) }

    fun connectPartner(data: PairingData) = launch { repository.savePartnerData(data) }

    fun disconnectPartner() = launch { repository.disconnectPartner() }

    fun updateNotifications(preference: NotificationPreference) =
        launch { repository.updateNotificationPreference(preference) }

    fun setThemeMode(mode: ThemeMode) = launch { repository.setThemeMode(mode) }

    fun deleteAllData(onDone: () -> Unit) = launch {
        repository.deleteAllData()
        onDone()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KenaretApp
                MainViewModel(app.container.repository)
            }
        }
    }
}
