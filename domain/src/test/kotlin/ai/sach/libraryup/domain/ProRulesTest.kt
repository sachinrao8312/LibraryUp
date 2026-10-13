package ai.sach.libraryup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProRulesTest {
    private val start = LocalDate.of(2026, 3, 1)

    @Test
    fun `trial counts down to zero and never below`() {
        assertEquals(30, ProRules.trialDaysLeft(start, start))
        assertEquals(1, ProRules.trialDaysLeft(start, start.plusDays(29)))
        assertEquals(0, ProRules.trialDaysLeft(start, start.plusDays(30)))
        assertEquals(0, ProRules.trialDaysLeft(start, start.plusDays(400)))
    }

    @Test
    fun `pro from purchase, trial or pass`() {
        assertTrue(ProRules.isPro(purchased = true, trialDaysLeft = 0, passUntilMillis = 0, nowMillis = 10))
        assertTrue(ProRules.isPro(purchased = false, trialDaysLeft = 3, passUntilMillis = 0, nowMillis = 10))
        assertTrue(ProRules.isPro(purchased = false, trialDaysLeft = 0, passUntilMillis = 11, nowMillis = 10))
        assertFalse(ProRules.isPro(purchased = false, trialDaysLeft = 0, passUntilMillis = 10, nowMillis = 10))
    }

    @Test
    fun `free tier stops adding seats past the limit only`() {
        assertTrue(ProRules.canAddSeats(current = 40, adding = 10, pro = false))
        assertFalse(ProRules.canAddSeats(current = 40, adding = 11, pro = false))
        assertTrue(ProRules.canAddSeats(current = 200, adding = 50, pro = true))
    }
}
