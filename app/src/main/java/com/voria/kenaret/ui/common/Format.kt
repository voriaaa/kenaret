package com.voria.kenaret.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.voria.kenaret.cycle.JalaliCalendar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** True when the UI is in Persian. Persian uses the Jalali calendar and Persian digits. */
@Composable
fun isPersian(): Boolean = LocalConfiguration.current.locales[0].language == "fa"

/** A month in either the Jalali (Persian UI) or Gregorian (English UI) calendar. */
data class MonthKey(val year: Int, val month: Int, val jalali: Boolean) {
    fun firstDay(): LocalDate =
        if (jalali) JalaliCalendar.toGregorian(year, month, 1) else LocalDate.of(year, month, 1)

    fun length(): Int =
        if (jalali) JalaliCalendar.monthLength(year, month) else YearMonth.of(year, month).lengthOfMonth()

    fun days(): List<LocalDate> {
        val first = firstDay()
        return List(length()) { first.plusDays(it.toLong()) }
    }

    fun plus(months: Int): MonthKey {
        val index = year * 12 + (month - 1) + months
        return MonthKey(Math.floorDiv(index, 12), Math.floorMod(index, 12) + 1, jalali)
    }

    fun contains(date: LocalDate): Boolean = of(date, jalali) == this

    companion object {
        fun of(date: LocalDate, jalali: Boolean): MonthKey =
            if (jalali) {
                val j = JalaliCalendar.fromGregorian(date)
                MonthKey(j.year, j.month, true)
            } else {
                MonthKey(date.year, date.monthValue, false)
            }
    }
}

object Fmt {
    private val persianMonths = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    private val persianWeekdays = mapOf(
        DayOfWeek.SATURDAY to "شنبه",
        DayOfWeek.SUNDAY to "یکشنبه",
        DayOfWeek.MONDAY to "دوشنبه",
        DayOfWeek.TUESDAY to "سه‌شنبه",
        DayOfWeek.WEDNESDAY to "چهارشنبه",
        DayOfWeek.THURSDAY to "پنجشنبه",
        DayOfWeek.FRIDAY to "جمعه",
    )

    fun digits(text: String, persian: Boolean): String =
        if (!persian) text else buildString(text.length) {
            for (c in text) append(if (c in '0'..'9') '۰' + (c - '0') else c)
        }

    fun number(value: Int, persian: Boolean): String = digits(value.toString(), persian)

    fun monthName(month: Int, jalali: Boolean): String =
        if (jalali) persianMonths[month - 1]
        else java.time.Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    fun monthTitle(key: MonthKey, persian: Boolean): String =
        digits("${monthName(key.month, key.jalali)} ${key.year}", persian)

    /** "۱۴ مهر" or "Oct 6". */
    fun dayMonth(date: LocalDate, persian: Boolean): String =
        if (persian) {
            val j = JalaliCalendar.fromGregorian(date)
            digits("${j.day} ${persianMonths[j.month - 1]}", true)
        } else {
            "${date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${date.dayOfMonth}"
        }

    /** "سه‌شنبه ۱۴ مهر ۱۴۰۵" or "Tuesday, Oct 6, 2026". */
    fun fullDate(date: LocalDate, persian: Boolean): String =
        if (persian) {
            val j = JalaliCalendar.fromGregorian(date)
            digits("${persianWeekdays[date.dayOfWeek]} ${j.day} ${persianMonths[j.month - 1]} ${j.year}", true)
        } else {
            "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}, " +
                "${date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${date.dayOfMonth}, ${date.year}"
        }

    /** "۱۴ مهر ۱۴۰۵" or "Oct 6, 2026". */
    fun shortDate(date: LocalDate, persian: Boolean): String =
        if (persian) {
            val j = JalaliCalendar.fromGregorian(date)
            digits("${j.day} ${persianMonths[j.month - 1]} ${j.year}", true)
        } else {
            "${date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${date.dayOfMonth}, ${date.year}"
        }

    fun dayOfMonth(date: LocalDate, jalali: Boolean, persian: Boolean): String {
        val day = if (jalali) JalaliCalendar.fromGregorian(date).day else date.dayOfMonth
        return number(day, persian)
    }

    fun time(hour: Int, minute: Int, persian: Boolean): String =
        digits("%02d:%02d".format(Locale.US, hour, minute), persian)

    /** First day of the week: Saturday for the Persian calendar, Monday otherwise. */
    fun firstDayOfWeek(jalali: Boolean): DayOfWeek = if (jalali) DayOfWeek.SATURDAY else DayOfWeek.MONDAY

    fun weekdayInitials(jalali: Boolean): List<String> =
        if (jalali) listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        else listOf("M", "T", "W", "T", "F", "S", "S")
}
