package ai.sach.libraryup.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Everything needed to tell where one membership period stands on a given day. */
data class PeriodSnapshot(
    val startDate: LocalDate,
    /** Next due date: the first day not covered by this period. */
    val dueDate: LocalDate,
    val fee: Rupees,
    val discount: Rupees = 0,
    val lateFee: Rupees = 0,
    val paid: Rupees = 0,
    val frozen: Boolean = false
) {
    val amountDue: Rupees get() = BillingCalculator.amountDue(fee, discount, lateFee)
    val balance: Rupees get() = (amountDue - paid).coerceAtLeast(0)
    val isFullyPaid: Boolean get() = balance == 0L
}

/**
 * Where a student stands. Ordered roughly by urgency so lists can sort on it.
 *
 * Two independent questions are folded together here: has this period been paid for,
 * and is the period about to run out? An unpaid period is [UNPAID] from day one (owners
 * collect up front), and [OVERDUE] once [DueRules.graceDays] have passed since it
 * started. A paid period becomes [EXPIRING_SOON] in its last week and [EXPIRED] once
 * the due date passes without a renewal.
 */
enum class DueStatus {
    OVERDUE,
    EXPIRED,
    DUE_TODAY,
    UNPAID,
    EXPIRING_SOON,
    PAID,
    FROZEN
}

data class DueRules(
    /** Days after the period starts before an unpaid fee counts as overdue. */
    val graceDays: Int = 3,
    /** How far ahead "expiring soon" looks. */
    val expiringWindowDays: Int = 7
)

object DueCalculator {

    fun status(period: PeriodSnapshot, today: LocalDate, rules: DueRules = DueRules()): DueStatus {
        if (period.frozen) return DueStatus.FROZEN

        if (!period.isFullyPaid) {
            val overdueFrom = period.startDate.plusDays(rules.graceDays.toLong())
            return when {
                !today.isBefore(overdueFrom) -> DueStatus.OVERDUE
                today == period.startDate -> DueStatus.DUE_TODAY
                else -> DueStatus.UNPAID
            }
        }

        val daysLeft = ChronoUnit.DAYS.between(today, period.dueDate)
        return when {
            daysLeft < 0 -> DueStatus.EXPIRED
            // The due date itself is when the next fee is due.
            daysLeft == 0L -> DueStatus.DUE_TODAY
            daysLeft <= rules.expiringWindowDays -> DueStatus.EXPIRING_SOON
            else -> DueStatus.PAID
        }
    }

    /** Days until the due date; negative once it has passed. */
    fun daysUntilDue(period: PeriodSnapshot, today: LocalDate): Long =
        ChronoUnit.DAYS.between(today, period.dueDate)
}
