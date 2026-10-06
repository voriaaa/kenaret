package com.voria.kenaret.domain

import java.time.LocalDate

/** Who is using this device. HER = the person who tracks their own cycle. */
enum class Role {
    HER, PARTNER;

    companion object {
        fun from(value: String?): Role? = entries.firstOrNull { it.name == value }
    }
}

/** Estimated cycle phases. All phases are approximations, never certainties. */
enum class CyclePhase { MENSTRUAL, FOLLICULAR, OVULATION, LUTEAL }

enum class Audience { HER, PARTNER }

enum class AdviceCategory { SELF_CARE, EMOTIONAL_SUPPORT, PRACTICAL_SUPPORT, COMMUNICATION, EDUCATION }

enum class SymptomType {
    CRAMPS, HEADACHE, BACK_PAIN, FATIGUE, BLOATING, BREAST_TENDERNESS, APPETITE, SLEEP, MOOD, OTHER;

    companion object {
        fun from(value: String): SymptomType? = entries.firstOrNull { it.name == value }
    }
}

/** 0 = none, 1 = mild, 2 = moderate, 3 = severe. */
enum class Severity(val level: Int) {
    NONE(0), MILD(1), MODERATE(2), SEVERE(3);

    companion object {
        fun fromLevel(level: Int): Severity = entries.firstOrNull { it.level == level } ?: NONE
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK;

    companion object {
        fun from(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/** A recorded (or predicted) bleeding span. [end] is null when the end was not recorded. */
data class PeriodSpan(val start: LocalDate, val end: LocalDate?)

/** A piece of advice. Text lives in string resources so it can be localized. */
data class Advice(
    val id: String,
    val phase: CyclePhase?,
    val audience: Audience,
    val category: AdviceCategory,
    val titleRes: Int,
    val bodyRes: Int,
)
