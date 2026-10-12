package ai.sach.libraryup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class LateFeeTest {

    private val start = LocalDate.of(2026, 3, 5)

    @Test
    fun `off by default`() {
        assertFalse(LateFeeRule().enabled)
        assertEquals(0L, LateFeeRule().suggested(start, start.plusDays(30), 3))
    }

    @Test
    fun `nothing inside the grace period`() {
        val rule = LateFeeRule(perDay = 10)
        assertEquals(0L, rule.suggested(start, start, 3))
        assertEquals(0L, rule.suggested(start, start.plusDays(2), 3))
    }

    @Test
    fun `charges per day from the first overdue day`() {
        val rule = LateFeeRule(perDay = 10)
        // Overdue from start + 3 (same day the status turns OVERDUE).
        assertEquals(10L, rule.suggested(start, start.plusDays(3), 3))
        assertEquals(50L, rule.suggested(start, start.plusDays(7), 3))
    }

    @Test
    fun `cap applies`() {
        val rule = LateFeeRule(perDay = 20, maxAmount = 100)
        assertEquals(100L, rule.suggested(start, start.plusDays(30), 0))
        assertEquals(60L, rule.suggested(start, start.plusDays(2), 0))
    }
}
