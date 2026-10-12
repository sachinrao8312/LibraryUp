package ai.sach.libraryup.ui.screens.settingsScreen.notifications

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The three rent reminder states. */
enum class ReminderKind { DUE_SOON, DUE_TODAY, OVERDUE }

/**
 * The reminder ladder: decides which reminder (if any) a tenant is owed today,
 * and whether it has already been delivered.
 *
 * Deliberately free of Android and Room types so the whole policy can be exercised
 * with plain dates in a unit test.
 */
object ReminderPolicy {

    /** Past this many days overdue we stop notifying — it is a phone call, not a notification. */
    const val MAX_OVERDUE_DAYS = 60

    /** Year-month of the due date. A new month is a new billing cycle. */
    fun cycleKeyFor(dueDate: LocalDate): String =
        "%04d-%02d".format(dueDate.year, dueDate.monthValue)

    /**
     * Which reminder applies, given how many days remain until the due date.
     * Negative [daysUntilDue] means overdue. Returns null when nothing is owed today.
     */
    fun kindFor(daysUntilDue: Long, reminderDays: Int): ReminderKind? = when {
        daysUntilDue > reminderDays -> null          // not in the reminder window yet
        daysUntilDue > 0L -> ReminderKind.DUE_SOON
        daysUntilDue == 0L -> ReminderKind.DUE_TODAY
        else -> ReminderKind.OVERDUE
    }

    /**
     * Whether [kind] should actually be posted now, given what the ledger already recorded.
     *
     * DUE_SOON and DUE_TODAY are once per cycle. OVERDUE escalates then backs off:
     * first sighting, then +2 days, then +4 days, then weekly — measured from the last
     * send rather than from the due date, so a day the device was off does not swallow
     * a reminder. A tenant never receives more than one reminder per day in any case.
     */
    fun shouldSend(
        kind: ReminderKind,
        daysOverdue: Long,
        lastSentOn: LocalDate?,
        sendCount: Int,
        today: LocalDate,
        dailyOverdueAlerts: Boolean
    ): Boolean {
        // Hard ceiling, applies to every kind: at most one reminder per tenant per day.
        if (lastSentOn != null && !lastSentOn.isBefore(today)) return false

        return when (kind) {
            ReminderKind.DUE_SOON, ReminderKind.DUE_TODAY -> sendCount == 0
            ReminderKind.OVERDUE -> when {
                daysOverdue > MAX_OVERDUE_DAYS -> false
                dailyOverdueAlerts -> true
                lastSentOn == null -> true
                else -> ChronoUnit.DAYS.between(lastSentOn, today) >= backoffGapDays(sendCount)
            }
        }
    }

    /** Days to wait after the Nth overdue reminder before sending the next one. */
    private fun backoffGapDays(sendCount: Int): Long = when (sendCount) {
        0 -> 0L
        1 -> 2L   // first notice on day 1, second on day 3
        2 -> 4L   // third on day 7
        else -> 7L // weekly from then on
    }
}
