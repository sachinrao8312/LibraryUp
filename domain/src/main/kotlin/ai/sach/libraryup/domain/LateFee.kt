package ai.sach.libraryup.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Automatic late fee: [perDay] rupees for every day an unpaid period is past its grace
 * period, capped at [maxAmount] (0 = no cap). [perDay] = 0 turns the rule off.
 */
data class LateFeeRule(val perDay: Rupees = 0, val maxAmount: Rupees = 0) {

    val enabled: Boolean get() = perDay > 0

    /**
     * Suggested late fee for a period that started on [periodStart] and is still unpaid
     * on [today]. Zero inside the grace period or when the rule is off.
     */
    fun suggested(periodStart: LocalDate, today: LocalDate, graceDays: Int): Rupees {
        if (!enabled) return 0
        val lateDays = ChronoUnit.DAYS.between(periodStart.plusDays(graceDays.toLong()), today) + 1
        if (lateDays <= 0) return 0
        val fee = lateDays * perDay
        return if (maxAmount > 0) minOf(fee, maxAmount) else fee
    }
}
