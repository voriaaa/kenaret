package com.voria.kenaret.cycle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class JalaliCalendarTest {
    @Test
    fun knownDates() {
        assertEquals(LocalDate.of(2024, 3, 20), JalaliCalendar.toGregorian(1403, 1, 1))
        assertEquals(LocalDate.of(2025, 3, 21), JalaliCalendar.toGregorian(1404, 1, 1))
        assertEquals(JalaliDate(1405, 7, 14), JalaliCalendar.fromGregorian(LocalDate.of(2026, 10, 6)))
    }

    @Test
    fun leapYears() {
        assertTrue(JalaliCalendar.isLeapYear(1399))
        assertTrue(JalaliCalendar.isLeapYear(1403))
        assertFalse(JalaliCalendar.isLeapYear(1404))
        assertEquals(30, JalaliCalendar.monthLength(1403, 12))
        assertEquals(29, JalaliCalendar.monthLength(1404, 12))
        assertEquals(LocalDate.of(2025, 3, 20), JalaliCalendar.toGregorian(1403, 12, 30))
    }

    @Test
    fun roundTripOverManyYears() {
        var date = LocalDate.of(2015, 1, 1)
        val end = LocalDate.of(2035, 12, 31)
        while (!date.isAfter(end)) {
            val j = JalaliCalendar.fromGregorian(date)
            assertEquals(date, JalaliCalendar.toGregorian(j.year, j.month, j.day))
            date = date.plusDays(1)
        }
    }
}
