package com.voria.kenaret.cycle

import java.time.LocalDate

data class JalaliDate(val year: Int, val month: Int, val day: Int)

/**
 * Solar Hijri (Jalali / Persian) calendar conversion.
 * Based on the well-known "jalaali" algorithm by Kazimierz M. Borkowski.
 * Epoch-day arithmetic is delegated to java.time for the Gregorian side.
 */
object JalaliCalendar {
    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )

    private data class CalInfo(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): CalInfo {
        require(jy >= breaks.first() && jy < breaks.last()) { "Year out of range: $jy" }
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0
        for (i in 1 until breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + (jump % 33) / 4
            jp = jm
        }
        var n = jy - jp
        leapJ += (n / 33) * 8 + ((n % 33) + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1
        val leapG = gy / 4 - ((gy / 100) + 1) * 3 / 4 - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4
        return CalInfo(leap, gy, march)
    }

    fun isLeapYear(jy: Int): Boolean = jalCal(jy).leap == 0

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        isLeapYear(jy) -> 30
        else -> 29
    }

    fun toGregorian(jy: Int, jm: Int, jd: Int): LocalDate {
        val info = jalCal(jy)
        val nowruz = LocalDate.of(info.gy, 3, info.march)
        val offset = (jm - 1) * 31 - (jm / 7) * (jm - 7) + jd - 1
        return nowruz.plusDays(offset.toLong())
    }

    fun fromGregorian(date: LocalDate): JalaliDate {
        val gy = date.year
        var jy = gy - 621
        val info = jalCal(jy)
        val nowruz = LocalDate.of(gy, 3, info.march)
        var k = (date.toEpochDay() - nowruz.toEpochDay()).toInt()
        if (k >= 0) {
            if (k <= 185) {
                return JalaliDate(jy, 1 + k / 31, k % 31 + 1)
            }
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (info.leap == 1) k += 1
        }
        return JalaliDate(jy, 7 + k / 30, k % 30 + 1)
    }
}
