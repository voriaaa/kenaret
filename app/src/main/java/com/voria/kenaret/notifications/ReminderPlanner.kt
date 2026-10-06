package com.voria.kenaret.notifications

import com.voria.kenaret.cycle.CycleCalculator
import com.voria.kenaret.domain.CyclePhase
import java.time.LocalDate

enum class ReminderType(val priority: Int) {
    PERIOD_START(0),
    PERIOD_SOON(1),
    PHASE_FOLLICULAR(2),
    PHASE_OVULATION(3),
    PHASE_LUTEAL(4),
    EDUCATIONAL(5),
}

data class PlannedReminder(val date: LocalDate, val type: ReminderType, val phase: CyclePhase)

/** Which reminder groups to include. */
data class PlanOptions(
    val periodReminders: Boolean = true,
    val phaseReminders: Boolean = true,
    val educationalReminders: Boolean = true,
)

/**
 * Pure function that turns a cycle estimate into a list of future reminder dates.
 * The scheduler cancels everything and re-plans whenever the cycle data changes.
 */
object ReminderPlanner {
    const val DAYS_BEFORE_PERIOD = 2L
    const val DEFAULT_HORIZON_DAYS = 120

    fun plan(
        lastPeriodStart: LocalDate,
        cycleLength: Int,
        periodLength: Int,
        today: LocalDate,
        options: PlanOptions,
        horizonDays: Int = DEFAULT_HORIZON_DAYS,
    ): List<PlannedReminder> {
        val length = CycleCalculator.clampCycle(cycleLength)
        val period = CycleCalculator.clampPeriod(periodLength)
        val ranges = CycleCalculator.phaseRanges(length, period)
        val end = today.plusDays(horizonDays.toLong())
        val result = mutableListOf<PlannedReminder>()

        // Skip whole cycles that ended long before today so very old data stays cheap.
        var start = lastPeriodStart
        var isFirst = true
        val behind = CycleCalculator.daysBetween(lastPeriodStart, today) / length - 1
        if (behind > 0) {
            start = lastPeriodStart.plusDays(behind.toLong() * length)
            isFirst = false
        }

        fun dayOf(cycleStart: LocalDate, cycleDay: Int) = cycleStart.plusDays((cycleDay - 1).toLong())

        while (!start.isAfter(end)) {
            if (options.periodReminders && !isFirst) {
                result += PlannedReminder(start.minusDays(DAYS_BEFORE_PERIOD), ReminderType.PERIOD_SOON, CyclePhase.LUTEAL)
                result += PlannedReminder(start, ReminderType.PERIOD_START, CyclePhase.MENSTRUAL)
            }
            if (options.phaseReminders) {
                if (!ranges.follicular.isEmpty()) {
                    result += PlannedReminder(dayOf(start, ranges.follicular.first), ReminderType.PHASE_FOLLICULAR, CyclePhase.FOLLICULAR)
                }
                result += PlannedReminder(dayOf(start, ranges.ovulation.first), ReminderType.PHASE_OVULATION, CyclePhase.OVULATION)
                result += PlannedReminder(dayOf(start, ranges.luteal.first), ReminderType.PHASE_LUTEAL, CyclePhase.LUTEAL)
            }
            if (options.educationalReminders) {
                val menstrualTipDay = minOf(3, ranges.menstrual.last)
                result += PlannedReminder(dayOf(start, menstrualTipDay), ReminderType.EDUCATIONAL, CyclePhase.MENSTRUAL)
                val lutealTipDay = minOf(ranges.luteal.first + 3, length - DAYS_BEFORE_PERIOD.toInt() - 1)
                if (lutealTipDay >= ranges.luteal.first) {
                    result += PlannedReminder(dayOf(start, lutealTipDay), ReminderType.EDUCATIONAL, CyclePhase.LUTEAL)
                }
            }
            start = start.plusDays(length.toLong())
            isFirst = false
        }

        return result
            .filter { !it.date.isBefore(today) && !it.date.isAfter(end) }
            .groupBy { it.date }
            .map { (_, sameDay) -> sameDay.minBy { it.type.priority } }
            .sortedBy { it.date }
    }
}
