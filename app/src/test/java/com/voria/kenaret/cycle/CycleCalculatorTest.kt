package com.voria.kenaret.cycle

import com.voria.kenaret.domain.CyclePhase
import com.voria.kenaret.domain.PeriodSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleCalculatorTest {

    private val start = LocalDate.of(2026, 3, 1)

    @Test
    fun cycleDay_firstDayOfPeriodIsDayOne() {
        assertEquals(1, CycleCalculator.cycleDay(start, start))
        assertEquals(2, CycleCalculator.cycleDay(start, start.plusDays(1)))
        assertEquals(24, CycleCalculator.cycleDay(start, start.plusDays(23)))
    }

    @Test
    fun nextPeriod_isLastStartPlusCycleLength() {
        assertEquals(LocalDate.of(2026, 3, 29), CycleCalculator.nextPeriodStart(start, 28))
        assertEquals(LocalDate.of(2026, 3, 22), CycleCalculator.nextPeriodStart(start, 21))
        assertEquals(LocalDate.of(2026, 4, 5), CycleCalculator.nextPeriodStart(start, 35))
    }

    @Test
    fun cycle21Days_phasesAreValidAndCoverTheCycle() {
        assertPhasesCover(21, 5)
        val ranges = CycleCalculator.phaseRanges(21, 5)
        assertEquals(1..5, ranges.menstrual)
        assertEquals(21, ranges.luteal.last)
        assertEquals(CyclePhase.MENSTRUAL, CycleCalculator.phaseForDay(1, 21, 5))
        assertEquals(CyclePhase.LUTEAL, CycleCalculator.phaseForDay(21, 21, 5))
    }

    @Test
    fun cycle28Days_typicalPhases() {
        val ranges = CycleCalculator.phaseRanges(28, 5)
        assertEquals(1..5, ranges.menstrual)
        assertEquals(6..11, ranges.follicular)
        assertEquals(12..15, ranges.ovulation)
        assertEquals(16..28, ranges.luteal)
        assertEquals(CyclePhase.FOLLICULAR, CycleCalculator.phaseForDay(8, 28, 5))
        assertEquals(CyclePhase.OVULATION, CycleCalculator.phaseForDay(14, 28, 5))
        assertEquals(CyclePhase.LUTEAL, CycleCalculator.phaseForDay(24, 28, 5))
    }

    @Test
    fun cycle35Days_ovulationWindowMovesLater() {
        val ranges = CycleCalculator.phaseRanges(35, 5)
        assertEquals(19..22, ranges.ovulation)
        assertEquals(23..35, ranges.luteal)
        assertPhasesCover(35, 5)
    }

    @Test
    fun allSupportedLengths_produceConsistentPhases() {
        for (length in 21..45) {
            for (period in 2..10) {
                assertPhasesCover(length, period)
            }
        }
    }

    @Test
    fun shortCycleWithLongPeriod_isStillValid() {
        val ranges = CycleCalculator.phaseRanges(21, 10)
        assertTrue(ranges.follicular.isEmpty())
        assertEquals(11, ranges.ovulation.first)
        assertPhasesCover(21, 10)
    }

    @Test
    fun outOfRangeLengths_areClamped() {
        assertEquals(LocalDate.of(2026, 3, 22), CycleCalculator.nextPeriodStart(start, 10))
        assertEquals(LocalDate.of(2026, 4, 15), CycleCalculator.nextPeriodStart(start, 99))
    }

    @Test
    fun status_example24thDayOf28() {
        val today = start.plusDays(23)
        val status = CycleCalculator.status(start, today, 28, 5)
        assertEquals(24, status.cycleDay)
        assertEquals(CyclePhase.LUTEAL, status.phase)
        assertEquals(5, status.daysUntilNextPeriod)
        assertFalse(status.isLate)
    }

    @Test
    fun status_lateWithoutProjection_andProjectedForPartner() {
        val today = start.plusDays(30)
        val her = CycleCalculator.status(start, today, 28, 5, projectForward = false)
        assertTrue(her.isLate)
        assertEquals(31, her.cycleDay)

        val partner = CycleCalculator.status(start, today, 28, 5, projectForward = true)
        assertEquals(3, partner.cycleDay)
        assertEquals(CyclePhase.MENSTRUAL, partner.phase)
        assertEquals(start.plusDays(28), partner.cycleStart)
    }

    @Test
    fun monthBoundary_nextPeriodCrossesMonths() {
        val jan30 = LocalDate.of(2026, 1, 30)
        assertEquals(LocalDate.of(2026, 2, 27), CycleCalculator.nextPeriodStart(jan30, 28))
        val status = CycleCalculator.status(jan30, LocalDate.of(2026, 2, 2), 28, 5)
        assertEquals(4, status.cycleDay)
        val dec20 = LocalDate.of(2025, 12, 20)
        assertEquals(LocalDate.of(2026, 1, 17), CycleCalculator.nextPeriodStart(dec20, 28))
    }

    @Test
    fun leapYear_february29IsCounted() {
        assertEquals(LocalDate.of(2024, 3, 9), CycleCalculator.nextPeriodStart(LocalDate.of(2024, 2, 10), 28))
        assertEquals(LocalDate.of(2023, 3, 10), CycleCalculator.nextPeriodStart(LocalDate.of(2023, 2, 10), 28))
        assertEquals(20, CycleCalculator.cycleDay(LocalDate.of(2024, 2, 10), LocalDate.of(2024, 2, 29)))
        assertEquals(21, CycleCalculator.cycleDay(LocalDate.of(2024, 2, 10), LocalDate.of(2024, 3, 1)))
    }

    @Test
    fun newPeriod_updatesAverageAndRealCycleLength() {
        val starts = listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 30), LocalDate.of(2026, 2, 28))
        // Intervals: 29 and 29 days.
        assertEquals(29, CycleCalculator.averageCycleLength(starts, 28))
        assertEquals(28, CycleCalculator.averageCycleLength(starts.take(1), 28))

        val spans = starts.map { PeriodSpan(it, null) }
        val info = CycleCalculator.dayInfo(LocalDate.of(2026, 1, 29), spans, 29, 5, LocalDate.of(2026, 3, 1))
        assertNotNull(info)
        assertEquals(29, info!!.cycleDay)
        assertFalse(info.isPredicted)
    }

    @Test
    fun editingPeriod_changesTheEstimate() {
        val today = LocalDate.of(2026, 3, 20)
        val before = CycleCalculator.status(LocalDate.of(2026, 3, 1), today, 28, 5)
        val after = CycleCalculator.status(LocalDate.of(2026, 3, 5), today, 28, 5)
        assertEquals(20, before.cycleDay)
        assertEquals(16, after.cycleDay)
        assertEquals(LocalDate.of(2026, 4, 2), after.nextPeriodStart)
    }

    @Test
    fun dayInfo_predictsFuturePeriods() {
        val today = LocalDate.of(2026, 3, 10)
        val spans = listOf(PeriodSpan(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 4)))
        val recorded = CycleCalculator.dayInfo(LocalDate.of(2026, 3, 3), spans, 28, 5, today)!!
        assertTrue(recorded.isPeriod)
        assertFalse(recorded.isPredicted)
        val afterRecordedEnd = CycleCalculator.dayInfo(LocalDate.of(2026, 3, 5), spans, 28, 5, today)!!
        assertFalse(afterRecordedEnd.isPeriod)
        val future = CycleCalculator.dayInfo(LocalDate.of(2026, 3, 30), spans, 28, 5, today)!!
        assertEquals(2, future.cycleDay)
        assertTrue(future.isPeriod)
        assertTrue(future.isPredicted)
        assertNull(CycleCalculator.dayInfo(LocalDate.of(2026, 2, 1), spans, 28, 5, today))
    }

    @Test
    fun insights_areStatistical() {
        val spans = listOf(
            PeriodSpan(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 5)),
            PeriodSpan(LocalDate.of(2026, 1, 29), null),
            PeriodSpan(LocalDate.of(2026, 2, 28), null),
            PeriodSpan(LocalDate.of(2026, 3, 28), null),
        )
        val insights = CycleStats.insights(spans)
        assertEquals(3, insights.completedCycles)
        assertEquals(28, insights.recentMin)
        assertEquals(30, insights.recentMax)
        assertEquals(29, insights.averageLength)
        assertEquals(5, insights.averagePeriodLength)
    }

    private fun assertPhasesCover(length: Int, period: Int) {
        val r = CycleCalculator.phaseRanges(length, period)
        assertEquals(1, r.menstrual.first)
        assertEquals(period, r.menstrual.last)
        assertTrue("ovulation not empty for $length/$period", !r.ovulation.isEmpty())
        assertTrue(r.ovulation.first > r.menstrual.last)
        assertEquals(r.ovulation.last + 1, r.luteal.first)
        assertEquals(length, r.luteal.last)
        assertTrue(r.luteal.first <= r.luteal.last)
        if (!r.follicular.isEmpty()) {
            assertEquals(r.menstrual.last + 1, r.follicular.first)
            assertEquals(r.ovulation.first - 1, r.follicular.last)
        }
        for (day in 1..length) {
            val phase = CycleCalculator.phaseForDay(day, length, period)
            val expected = when (day) {
                in r.menstrual -> CyclePhase.MENSTRUAL
                in r.follicular -> CyclePhase.FOLLICULAR
                in r.ovulation -> CyclePhase.OVULATION
                else -> CyclePhase.LUTEAL
            }
            assertEquals(expected, phase)
        }
    }
}
