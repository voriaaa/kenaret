package com.voria.kenaret.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReminderPlannerTest {
    private val start = LocalDate.of(2026, 1, 1)

    @Test
    fun notificationDates_for28DayCycle() {
        val plan = ReminderPlanner.plan(start, 28, 5, today = start, options = PlanOptions(), horizonDays = 30)
        val byType = plan.groupBy { it.type }.mapValues { entry -> entry.value.map { it.date } }
        assertEquals(listOf(LocalDate.of(2026, 1, 27)), byType[ReminderType.PERIOD_SOON])
        assertEquals(listOf(LocalDate.of(2026, 1, 29)), byType[ReminderType.PERIOD_START])
        assertEquals(LocalDate.of(2026, 1, 6), byType[ReminderType.PHASE_FOLLICULAR]!!.first())
        assertEquals(LocalDate.of(2026, 1, 12), byType[ReminderType.PHASE_OVULATION]!!.first())
        assertEquals(LocalDate.of(2026, 1, 16), byType[ReminderType.PHASE_LUTEAL]!!.first())
        assertTrue(plan.zipWithNext().all { (a, b) -> a.date < b.date })
    }

    @Test
    fun notificationDates_follow35DayCycle() {
        val plan = ReminderPlanner.plan(start, 35, 5, today = start, options = PlanOptions(phaseReminders = false, educationalReminders = false))
        val starts = plan.filter { it.type == ReminderType.PERIOD_START }.map { it.date }
        assertEquals(LocalDate.of(2026, 2, 5), starts.first())
        assertEquals(LocalDate.of(2026, 3, 12), starts[1])
        val soon = plan.filter { it.type == ReminderType.PERIOD_SOON }.map { it.date }
        assertEquals(LocalDate.of(2026, 2, 3), soon.first())
    }

    @Test
    fun noPastReminders_andRecalculationAfterEdit() {
        val today = LocalDate.of(2026, 1, 20)
        val plan = ReminderPlanner.plan(start, 28, 5, today, PlanOptions())
        assertTrue(plan.none { it.date.isBefore(today) })

        val edited = ReminderPlanner.plan(LocalDate.of(2026, 1, 5), 28, 5, today, PlanOptions())
        val nextStart = edited.first { it.type == ReminderType.PERIOD_START }.date
        assertEquals(LocalDate.of(2026, 2, 2), nextStart)
    }

    @Test
    fun disabledGroups_areNotPlanned() {
        val plan = ReminderPlanner.plan(start, 28, 5, start, PlanOptions(false, false, false))
        assertTrue(plan.isEmpty())
        val onlyPeriod = ReminderPlanner.plan(start, 28, 5, start, PlanOptions(true, false, false))
        assertFalse(onlyPeriod.isEmpty())
        assertTrue(onlyPeriod.all { it.type == ReminderType.PERIOD_SOON || it.type == ReminderType.PERIOD_START })
    }

    @Test
    fun veryOldStart_stillPlansFutureCycles() {
        val today = LocalDate.of(2026, 6, 1)
        val plan = ReminderPlanner.plan(start, 28, 5, today, PlanOptions())
        assertTrue(plan.isNotEmpty())
        assertTrue(plan.first().date >= today)
        assertTrue(plan.any { it.type == ReminderType.PERIOD_START })
    }
}
