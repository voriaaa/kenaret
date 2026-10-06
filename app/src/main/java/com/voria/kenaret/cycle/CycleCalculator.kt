package com.voria.kenaret.cycle

import com.voria.kenaret.domain.CyclePhase
import com.voria.kenaret.domain.PeriodSpan
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

/** Day ranges (1-based cycle days) for each estimated phase. [follicular] may be empty for short cycles. */
data class PhaseRanges(
    val menstrual: IntRange,
    val follicular: IntRange,
    val ovulation: IntRange,
    val luteal: IntRange,
)

/** Snapshot of where a cycle currently is. All values are estimates. */
data class CycleStatus(
    val today: LocalDate,
    val cycleStart: LocalDate,
    val cycleDay: Int,
    val cycleLength: Int,
    val periodLength: Int,
    val phase: CyclePhase,
    val nextPeriodStart: LocalDate,
    /** Days until the next estimated period. Negative means the estimate has passed. */
    val daysUntilNextPeriod: Int,
    val ovulationWindowStart: LocalDate,
    val ovulationWindowEnd: LocalDate,
) {
    val isLate: Boolean get() = daysUntilNextPeriod < 0
    val isOnPeriod: Boolean get() = cycleDay in 1..periodLength
}

/** Information about a single calendar day. */
data class DayInfo(
    val cycleDay: Int,
    val phase: CyclePhase,
    val isPeriod: Boolean,
    val isPredicted: Boolean,
    val isLate: Boolean = false,
)

/**
 * Pure cycle math. Cycle day 1 is the first day of a period.
 * Nothing here assumes a 28-day cycle: every function takes the cycle length (21..45).
 */
object CycleCalculator {
    const val MIN_CYCLE = 21
    const val MAX_CYCLE = 45
    const val DEFAULT_CYCLE = 28
    const val MIN_PERIOD = 2
    const val MAX_PERIOD = 10
    const val DEFAULT_PERIOD = 5

    /** Typical luteal length used only for estimating the ovulation window. */
    private const val LUTEAL_ESTIMATE = 14

    fun daysBetween(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()

    fun clampCycle(length: Int): Int = length.coerceIn(MIN_CYCLE, MAX_CYCLE)
    fun clampPeriod(length: Int): Int = length.coerceIn(MIN_PERIOD, MAX_PERIOD)

    /** Cycle day of [date] for a cycle that started on [cycleStart]. Day 1 = cycleStart. */
    fun cycleDay(cycleStart: LocalDate, date: LocalDate): Int = daysBetween(cycleStart, date) + 1

    /** Estimated next period: last period start + average cycle length. */
    fun nextPeriodStart(lastPeriodStart: LocalDate, cycleLength: Int): LocalDate =
        lastPeriodStart.plusDays(clampCycle(cycleLength).toLong())

    fun phaseRanges(cycleLength: Int, periodLength: Int): PhaseRanges {
        val length = clampCycle(cycleLength)
        val period = clampPeriod(periodLength)
        val estimatedOvulationDay = length - LUTEAL_ESTIMATE
        var windowStart = max(estimatedOvulationDay - 2, period + 1)
        val windowEnd = max(estimatedOvulationDay + 1, windowStart + 1).coerceAtMost(length - 1)
        if (windowStart > windowEnd) windowStart = windowEnd
        return PhaseRanges(
            menstrual = 1..period,
            follicular = (period + 1)..(windowStart - 1),
            ovulation = windowStart..windowEnd,
            luteal = (windowEnd + 1)..length,
        )
    }

    /** Estimated phase for a cycle day. Days past the cycle length stay in the luteal phase (a late period). */
    fun phaseForDay(cycleDay: Int, cycleLength: Int, periodLength: Int): CyclePhase {
        val ranges = phaseRanges(cycleLength, periodLength)
        return when {
            cycleDay <= ranges.menstrual.last -> CyclePhase.MENSTRUAL
            cycleDay in ranges.follicular -> CyclePhase.FOLLICULAR
            cycleDay in ranges.ovulation -> CyclePhase.OVULATION
            else -> CyclePhase.LUTEAL
        }
    }

    /**
     * Current status for a cycle that began on [lastPeriodStart].
     * When [projectForward] is true (used for the partner, who can't log periods), the start is moved
     * forward by whole cycles so the status never shows an overdue period.
     */
    fun status(
        lastPeriodStart: LocalDate,
        today: LocalDate,
        cycleLength: Int,
        periodLength: Int,
        projectForward: Boolean = false,
    ): CycleStatus {
        val length = clampCycle(cycleLength)
        val period = clampPeriod(periodLength)
        var start = lastPeriodStart
        var day = cycleDay(start, today)
        if (projectForward && day > length) {
            val cycles = (day - 1) / length
            start = start.plusDays(cycles.toLong() * length)
            day = cycleDay(start, today)
        }
        val ranges = phaseRanges(length, period)
        val next = start.plusDays(length.toLong())
        return CycleStatus(
            today = today,
            cycleStart = start,
            cycleDay = day,
            cycleLength = length,
            periodLength = period,
            phase = phaseForDay(day, length, period),
            nextPeriodStart = next,
            daysUntilNextPeriod = daysBetween(today, next),
            ovulationWindowStart = start.plusDays((ranges.ovulation.first - 1).toLong()),
            ovulationWindowEnd = start.plusDays((ranges.ovulation.last - 1).toLong()),
        )
    }

    /** Length of a recorded period, falling back to [defaultLength] when no end date was recorded. */
    fun periodLengthOf(span: PeriodSpan, defaultLength: Int): Int {
        val end = span.end ?: return clampPeriod(defaultLength)
        return (daysBetween(span.start, end) + 1).coerceIn(1, 15)
    }

    /**
     * Average of the most recent cycle lengths (intervals between recorded period starts).
     * Implausible intervals are ignored. Returns [fallback] when there isn't enough data.
     */
    fun averageCycleLength(starts: List<LocalDate>, fallback: Int, maxCycles: Int = 6): Int {
        val lengths = cycleLengths(starts).takeLast(maxCycles)
        if (lengths.isEmpty()) return clampCycle(fallback)
        return clampCycle(lengths.average().roundToInt())
    }

    /** Lengths of completed cycles, oldest first. */
    fun cycleLengths(starts: List<LocalDate>): List<Int> =
        starts.distinct().sorted().zipWithNext { a, b -> daysBetween(a, b) }.filter { it in 15..90 }

    /**
     * Info for one calendar day based on recorded periods (sorted or not).
     * Past days between two recorded periods use the real cycle; days after the last record are estimated.
     */
    fun dayInfo(
        date: LocalDate,
        records: List<PeriodSpan>,
        cycleLength: Int,
        periodLength: Int,
        today: LocalDate,
    ): DayInfo? {
        if (records.isEmpty()) return null
        val sorted = records.sortedBy { it.start }
        val index = sorted.indexOfLast { !it.start.isAfter(date) }
        if (index < 0) return null
        val record = sorted[index]
        val next = sorted.getOrNull(index + 1)
        val recordedPeriod = periodLengthOf(record, periodLength)

        if (next != null) {
            val actualLength = daysBetween(record.start, next.start)
            val day = cycleDay(record.start, date)
            return DayInfo(
                cycleDay = day,
                phase = phaseForDay(day, actualLength, recordedPeriod),
                isPeriod = day <= recordedPeriod,
                isPredicted = false,
            )
        }

        val length = clampCycle(cycleLength)
        val day = cycleDay(record.start, date)
        if (day <= length) {
            return DayInfo(
                cycleDay = day,
                phase = phaseForDay(day, length, recordedPeriod),
                isPeriod = day <= recordedPeriod,
                isPredicted = date.isAfter(today),
            )
        }
        if (!date.isAfter(today)) {
            // The estimated period has passed and no new period was logged.
            return DayInfo(day, CyclePhase.LUTEAL, isPeriod = false, isPredicted = false, isLate = true)
        }
        val cycles = (day - 1) / length
        val projectedStart = record.start.plusDays(cycles.toLong() * length)
        val projectedDay = cycleDay(projectedStart, date)
        val period = clampPeriod(periodLength)
        return DayInfo(
            cycleDay = projectedDay,
            phase = phaseForDay(projectedDay, length, period),
            isPeriod = projectedDay <= period,
            isPredicted = true,
        )
    }
}

/** Purely statistical summary of recorded cycles. No medical interpretation. */
data class CycleInsights(
    val completedCycles: Int,
    val averageLength: Int?,
    val recentMin: Int?,
    val recentMax: Int?,
    val recentCount: Int,
    val averagePeriodLength: Int?,
    /** (cycle start, length) pairs, oldest first. */
    val history: List<Pair<LocalDate, Int>>,
)

object CycleStats {
    fun insights(records: List<PeriodSpan>, maxCycles: Int = 6): CycleInsights {
        val sorted = records.distinctBy { it.start }.sortedBy { it.start }
        val history = sorted.zipWithNext { a, b -> a.start to CycleCalculator.daysBetween(a.start, b.start) }
            .filter { it.second in 15..90 }
        val recent = history.takeLast(maxCycles).map { it.second }
        val lastThree = history.takeLast(3).map { it.second }
        val periodLengths = sorted.mapNotNull { span ->
            span.end?.let { CycleCalculator.daysBetween(span.start, it) + 1 }?.takeIf { it in 1..15 }
        }
        return CycleInsights(
            completedCycles = history.size,
            averageLength = recent.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
            recentMin = lastThree.minOrNull(),
            recentMax = lastThree.maxOrNull(),
            recentCount = lastThree.size,
            averagePeriodLength = periodLengths.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
            history = history,
        )
    }
}
