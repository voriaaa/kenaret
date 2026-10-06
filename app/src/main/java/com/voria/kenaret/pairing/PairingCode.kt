package com.voria.kenaret.pairing

import com.voria.kenaret.cycle.CycleCalculator
import java.time.LocalDate

/** The minimum data a partner's device needs, plus what she chose to share. */
data class PairingData(
    val lastPeriodStart: LocalDate,
    val cycleLength: Int,
    val periodLength: Int,
    val sharePhase: Boolean,
    val shareNextPeriodDate: Boolean,
    val shareDaysUntil: Boolean,
)

/**
 * Offline pairing code, e.g. `KNT-7Q4M-2XA`.
 *
 * Kenaret has no server and no account, so the code itself carries the small amount of data the
 * partner's device needs (last period start, cycle and period length, sharing choices) plus a checksum.
 * Nothing leaves the phone unless the user shares the code. Symptoms, notes and health data are never
 * part of the code.
 *
 * Layout (35 bits): 16 bits day offset | 5 bits cycle length | 4 bits period length | 3 flag bits | 7 bits checksum
 */
object PairingCode {
    const val PREFIX = "KNT"
    private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val EPOCH: LocalDate = LocalDate.of(2020, 1, 1)
    private const val CODE_CHARS = 7

    fun encode(data: PairingData): String {
        val offset = CycleCalculator.daysBetween(EPOCH, data.lastPeriodStart).coerceIn(0, 0xFFFF).toLong()
        val cycle = (CycleCalculator.clampCycle(data.cycleLength) - CycleCalculator.MIN_CYCLE).toLong()
        val period = (CycleCalculator.clampPeriod(data.periodLength) - CycleCalculator.MIN_PERIOD).toLong()
        var flags = 0L
        if (data.sharePhase) flags = flags or 0b001L
        if (data.shareNextPeriodDate) flags = flags or 0b010L
        if (data.shareDaysUntil) flags = flags or 0b100L

        val payload = (offset shl 12) or (cycle shl 7) or (period shl 3) or flags
        val value = (payload shl 7) or checksum(payload)

        val chars = CharArray(CODE_CHARS)
        var rest = value
        for (i in CODE_CHARS - 1 downTo 0) {
            chars[i] = ALPHABET[(rest and 31L).toInt()]
            rest = rest shr 5
        }
        val body = String(chars)
        return "$PREFIX-${body.substring(0, 4)}-${body.substring(4)}"
    }

    /** Returns null when the code is malformed or the checksum doesn't match. */
    fun decode(input: String): PairingData? {
        var cleaned = input.uppercase().filter { it.isLetterOrDigit() }
        if (cleaned.startsWith(PREFIX)) cleaned = cleaned.removePrefix(PREFIX)
        if (cleaned.length != CODE_CHARS) return null

        var value = 0L
        for (c in cleaned) {
            val index = ALPHABET.indexOf(c)
            if (index < 0) return null
            value = (value shl 5) or index.toLong()
        }
        val check = value and 0x7FL
        val payload = value shr 7
        if (checksum(payload) != check) return null

        val flags = payload and 0b111L
        val period = ((payload shr 3) and 0xFL).toInt() + CycleCalculator.MIN_PERIOD
        val cycle = ((payload shr 7) and 0x1FL).toInt() + CycleCalculator.MIN_CYCLE
        val offset = (payload shr 12) and 0xFFFFL
        if (cycle > CycleCalculator.MAX_CYCLE || period > CycleCalculator.MAX_PERIOD) return null

        return PairingData(
            lastPeriodStart = EPOCH.plusDays(offset),
            cycleLength = cycle,
            periodLength = period,
            sharePhase = flags and 0b001L != 0L,
            shareNextPeriodDate = flags and 0b010L != 0L,
            shareDaysUntil = flags and 0b100L != 0L,
        )
    }

    private fun checksum(payload: Long): Long {
        var h = 0x5AL
        var p = payload
        repeat(4) {
            h = (h * 31 + (p and 0x7FL)) and 0xFFFFL
            p = p shr 7
        }
        return (h xor (h shr 7)) and 0x7FL
    }
}
