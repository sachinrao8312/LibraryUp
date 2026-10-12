package ai.sach.libraryup.domain

import ai.sach.libraryup.domain.FeeCycleMode.CALENDAR_MONTH
import ai.sach.libraryup.domain.FeeCycleMode.FIXED_DAYS
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BillingTest {

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test
    fun `calendar month runs to the same date next month`() {
        assertEquals(d(2026, 4, 5), BillingCalculator.nextDueDate(d(2026, 3, 5), PlanDuration.MONTHLY, CALENDAR_MONTH))
        assertEquals(d(2026, 4, 4), BillingCalculator.validTill(d(2026, 3, 5), PlanDuration.MONTHLY, CALENDAR_MONTH))
    }

    @Test
    fun `31 January clamps to the end of February`() {
        assertEquals(d(2026, 2, 28), BillingCalculator.nextDueDate(d(2026, 1, 31), PlanDuration.MONTHLY, CALENDAR_MONTH))
        // 2028 is a leap year.
        assertEquals(d(2028, 2, 29), BillingCalculator.nextDueDate(d(2028, 1, 31), PlanDuration.MONTHLY, CALENDAR_MONTH))
    }

    @Test
    fun `fixed days ignores the calendar`() {
        assertEquals(d(2026, 3, 3), BillingCalculator.nextDueDate(d(2026, 2, 1), PlanDuration.MONTHLY, FIXED_DAYS))
        assertEquals(d(2026, 3, 31), BillingCalculator.nextDueDate(d(2026, 1, 30), PlanDuration.Months(2), FIXED_DAYS))
    }

    @Test
    fun `quarterly half-yearly and yearly plans`() {
        val start = d(2026, 11, 30)
        assertEquals(d(2027, 2, 28), BillingCalculator.nextDueDate(start, PlanDuration.QUARTERLY, CALENDAR_MONTH))
        assertEquals(d(2027, 5, 30), BillingCalculator.nextDueDate(start, PlanDuration.HALF_YEARLY, CALENDAR_MONTH))
        assertEquals(d(2027, 11, 30), BillingCalculator.nextDueDate(start, PlanDuration.YEARLY, CALENDAR_MONTH))
        assertEquals(d(2027, 11, 25), BillingCalculator.nextDueDate(start, PlanDuration.YEARLY, FIXED_DAYS))
    }

    @Test
    fun `day plans are the same in both modes`() {
        val start = d(2026, 2, 27)
        assertEquals(d(2026, 3, 6), BillingCalculator.nextDueDate(start, PlanDuration.WEEKLY, CALENDAR_MONTH))
        assertEquals(d(2026, 3, 6), BillingCalculator.nextDueDate(start, PlanDuration.WEEKLY, FIXED_DAYS))
        assertEquals(d(2026, 2, 27), BillingCalculator.validTill(start, PlanDuration.DAILY, CALENDAR_MONTH))
    }

    @Test
    fun `freeze gives back every paused day`() {
        val due = d(2026, 4, 5)
        assertEquals(d(2026, 4, 15), BillingCalculator.extendForFreeze(due, d(2026, 3, 20), d(2026, 3, 30)))
        assertEquals(due, BillingCalculator.extendForFreeze(due, d(2026, 3, 20), d(2026, 3, 20)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unfreeze before freeze is rejected`() {
        BillingCalculator.extendForFreeze(d(2026, 4, 5), d(2026, 3, 20), d(2026, 3, 19))
    }

    @Test
    fun `amount due applies discount and late fee and never goes negative`() {
        assertEquals(1050L, BillingCalculator.amountDue(fee = 1000, discount = 100, lateFee = 150))
        assertEquals(0L, BillingCalculator.amountDue(fee = 500, discount = 800, lateFee = 0))
    }
}
