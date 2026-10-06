package com.voria.kenaret.content

import com.voria.kenaret.domain.Advice
import com.voria.kenaret.domain.AdviceCategory
import com.voria.kenaret.domain.Audience
import com.voria.kenaret.domain.CyclePhase
import java.time.LocalDate

/**
 * Read-only access to the advice library. The UI never hard-codes advice text;
 * it asks the catalog for items by audience / phase / category.
 */
object AdviceCatalog {
    val all: List<Advice> get() = AdviceData.all

    fun forAudience(audience: Audience): List<Advice> = all.filter { it.audience == audience }

    fun forPhase(audience: Audience, phase: CyclePhase?): List<Advice> =
        all.filter { it.audience == audience && (phase == null || it.phase == phase) }

    fun forCategory(audience: Audience, category: AdviceCategory): List<Advice> =
        all.filter { it.audience == audience && it.category == category }

    /**
     * A stable daily pick: the same advice all day, rotating across days.
     * [salt] lets different cards on the same screen pick different items.
     */
    fun dailyPicks(audience: Audience, phase: CyclePhase?, date: LocalDate, count: Int, salt: Int = 0): List<Advice> {
        val pool = forPhase(audience, phase).ifEmpty { forAudience(audience) }
        if (pool.isEmpty()) return emptyList()
        val startIndex = Math.floorMod(date.toEpochDay() + salt * 7L, pool.size.toLong()).toInt()
        return List(minOf(count, pool.size)) { pool[(startIndex + it) % pool.size] }
    }

    fun byId(id: String): Advice? = all.firstOrNull { it.id == id }
}
