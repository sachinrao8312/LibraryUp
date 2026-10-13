package ai.sach.libraryup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DuesTest {

    private val start = LocalDate.of(2026, 3, 5)
    private val due = LocalDate.of(2026, 4, 5)

    private fun period(paid: Long, fee: Long = 1000, discount: Long = 0, lateFee: Long = 0, frozen: Boolean = false) =
        PeriodSnapshot(start, due, fee, discount, lateFee, paid, frozen)

    @Test
    fun `partial payments leave a balance`() {
        val p = period(paid = 400, fee = 1200, discount = 200)
        assertEquals(1000L, p.amountDue)
        assertEquals(600L, p.balance)
        assertFalse(p.isFullyPaid)
        assertTrue(period(paid = 1000).isFullyPaid)
        // Overpayment does not make the balance negative.
        assertEquals(0L, period(paid = 1500).balance)
    }

    @Test
    fun `unpaid period is due on day one and overdue after the grace period`() {
        val p = period(paid = 0)
        assertEquals(DueStatus.DUE_TODAY, DueCalculator.status(p, start))
        assertEquals(DueStatus.UNPAID, DueCalculator.status(p, start.plusDays(2)))
        assertEquals(DueStatus.OVERDUE, DueCalculator.status(p, start.plusDays(3)))
        assertEquals(DueStatus.OVERDUE, DueCalculator.status(p, due.plusDays(10)))
    }

    @Test
    fun `a partly paid period still counts as unpaid`() {
        assertEquals(DueStatus.OVERDUE, DueCalculator.status(period(paid = 999), start.plusDays(5)))
    }

    @Test
    fun `paid period moves through expiring soon, due today and expired`() {
        val p = period(paid = 1000)
        assertEquals(DueStatus.PAID, DueCalculator.status(p, start))
        assertEquals(DueStatus.PAID, DueCalculator.status(p, due.minusDays(8)))
        assertEquals(DueStatus.EXPIRING_SOON, DueCalculator.status(p, due.minusDays(7)))
        assertEquals(DueStatus.EXPIRING_SOON, DueCalculator.status(p, due.minusDays(1)))
        assertEquals(DueStatus.DUE_TODAY, DueCalculator.status(p, due))
        assertEquals(DueStatus.EXPIRED, DueCalculator.status(p, due.plusDays(1)))
    }

    @Test
    fun `frozen trumps everything`() {
        assertEquals(DueStatus.FROZEN, DueCalculator.status(period(paid = 0, frozen = true), due.plusDays(30)))
    }

    @Test
    fun `custom rules change the windows`() {
        val rules = DueRules(graceDays = 0, expiringWindowDays = 3)
        assertEquals(DueStatus.OVERDUE, DueCalculator.status(period(paid = 0), start, rules))
        assertEquals(DueStatus.PAID, DueCalculator.status(period(paid = 1000), due.minusDays(4), rules))
        assertEquals(DueStatus.EXPIRING_SOON, DueCalculator.status(period(paid = 1000), due.minusDays(3), rules))
    }

    @Test
    fun `days until due`() {
        assertEquals(31L, DueCalculator.daysUntilDue(period(0), start))
        assertEquals(-2L, DueCalculator.daysUntilDue(period(0), due.plusDays(2)))
    }
}
