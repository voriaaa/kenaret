package com.voria.kenaret.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PairingCodeTest {
    @Test
    fun roundTrip() {
        val data = PairingData(LocalDate.of(2026, 9, 28), 31, 6, sharePhase = true, shareNextPeriodDate = false, shareDaysUntil = true)
        val code = PairingCode.encode(data)
        assertTrue(code.matches(Regex("KNT-[2-9A-Z]{4}-[2-9A-Z]{3}")))
        assertEquals(data, PairingCode.decode(code))
        assertEquals(data, PairingCode.decode(code.lowercase().replace("-", " ")))
    }

    @Test
    fun allLengthsRoundTrip() {
        for (cycle in 21..45) for (period in 2..10) {
            val data = PairingData(LocalDate.of(2025, 2, 28).plusDays(cycle.toLong()), cycle, period, true, true, false)
            assertEquals(data, PairingCode.decode(PairingCode.encode(data)))
        }
    }

    @Test
    fun invalidCodes_areRejected() {
        assertNull(PairingCode.decode(""))
        assertNull(PairingCode.decode("KNT-1234"))
        assertNull(PairingCode.decode("hello world"))
        val code = PairingCode.encode(PairingData(LocalDate.of(2026, 1, 1), 28, 5, true, true, true))
        val last = code.last()
        val tampered = code.dropLast(1) + (if (last == 'Z') 'Y' else 'Z')
        assertNull(PairingCode.decode(tampered))
    }
}
