package ai.sach.libraryup.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Amounts are whole rupees. Library fees are always whole rupees in practice, and a
 * Long avoids the rounding surprises a Double brings to sums of payments.
 */
typealias Rupees = Long

/** How a library counts a "month" when a plan is measured in months. */
enum class FeeCycleMode {
    /** 5 Mar → 5 Apr, 31 Jan → 28/29 Feb. What most owners mean by "monthly". */
    CALENDAR_MONTH,

    /** Every month is [BillingCalculator.DAYS_PER_FIXED_MONTH] days, regardless of the calendar. */
    FIXED_DAYS
}

/** How long one billing period of a plan lasts. */
sealed class PlanDuration {
    data class Days(val days: Int) : PlanDuration() {
        init { require(days > 0) { "days must be positive" } }
    }

    data class Months(val months: Int) : PlanDuration() {
        init { require(months > 0) { "months must be positive" } }
    }

    companion object {
        val DAILY = Days(1)
        val WEEKLY = Days(7)
        val MONTHLY = Months(1)
        val QUARTERLY = Months(3)
        val HALF_YEARLY = Months(6)
        val YEARLY = Months(12)
    }
}

object BillingCalculator {

    const val DAYS_PER_FIXED_MONTH = 30

    /**
     * The date the next period starts, i.e. the next due date. A membership that starts on
     * [start] is valid up to and including the day before this.
     */
    fun nextDueDate(start: LocalDate, duration: PlanDuration, mode: FeeCycleMode): LocalDate =
        when (duration) {
            is PlanDuration.Days -> start.plusDays(duration.days.toLong())
            is PlanDuration.Months -> when (mode) {
                // plusMonths clamps to the last valid day: 31 Jan + 1 month = 28/29 Feb.
                FeeCycleMode.CALENDAR_MONTH -> start.plusMonths(duration.months.toLong())
                FeeCycleMode.FIXED_DAYS ->
                    start.plusDays(duration.months.toLong() * DAYS_PER_FIXED_MONTH)
            }
        }

    /** Last day the membership is valid ("valid till" on a receipt or ID card). */
    fun validTill(start: LocalDate, duration: PlanDuration, mode: FeeCycleMode): LocalDate =
        nextDueDate(start, duration, mode).minusDays(1)

    /**
     * New due date after a freeze from [frozenOn] to [unfrozenOn] (exclusive): the student
     * gets back every day the membership was paused.
     */
    fun extendForFreeze(dueDate: LocalDate, frozenOn: LocalDate, unfrozenOn: LocalDate): LocalDate {
        require(!unfrozenOn.isBefore(frozenOn)) { "unfreeze date is before freeze date" }
        val pausedDays = ChronoUnit.DAYS.between(frozenOn, unfrozenOn)
        return dueDate.plusDays(pausedDays)
    }

    /** What the student owes for one period before any payment. Never negative. */
    fun amountDue(fee: Rupees, discount: Rupees, lateFee: Rupees): Rupees {
        require(fee >= 0 && discount >= 0 && lateFee >= 0) { "amounts must not be negative" }
        return (fee - discount + lateFee).coerceAtLeast(0)
    }
}
