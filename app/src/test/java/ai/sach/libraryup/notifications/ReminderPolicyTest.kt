package ai.sach.libraryup.notifications

import ai.sach.libraryup.ui.screens.settingsScreen.notifications.ReminderKind
import ai.sach.libraryup.ui.screens.settingsScreen.notifications.ReminderPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Behaviour of the rent reminder ladder.
 *
 * These are the guarantees that keep the notification volume sane, so they are worth
 * pinning down: a tenant never gets two reminders in one day, due-soon and due-today
 * fire exactly once per billing cycle, and overdue escalates then backs off instead of
 * nagging forever.
 */
class ReminderPolicyTest {

    private val dueDate: LocalDate = LocalDate.of(2026, 9, 1)

    /**
     * Replays a whole billing cycle one day at a time against an in-memory ledger,
     * exactly as [ai.sach.libraryup.ui.screens.settingsScreen.notifications.DailyReminderWorker]
     * would, and returns what was actually sent.
     */
    private fun simulate(
        reminderDays: Int,
        dailyOverdueAlerts: Boolean,
        days: Int,
        startOffset: Long = -10
    ): List<Pair<LocalDate, ReminderKind>> {
        val sent = mutableListOf<Pair<LocalDate, ReminderKind>>()
        val ledger = mutableMapOf<ReminderKind, Pair<LocalDate, Int>>()
        var today = dueDate.plusDays(startOffset)

        repeat(days) {
            val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
            val kind = ReminderPolicy.kindFor(daysUntilDue, reminderDays)
            if (kind != null) {
                val record = ledger[kind]
                val shouldSend = ReminderPolicy.shouldSend(
                    kind = kind,
                    daysOverdue = -daysUntilDue,
                    lastSentOn = record?.first,
                    sendCount = record?.second ?: 0,
                    today = today,
                    dailyOverdueAlerts = dailyOverdueAlerts
                )
                if (shouldSend) {
                    sent += today to kind
                    ledger[kind] = today to ((record?.second ?: 0) + 1)
                }
            }
            today = today.plusDays(1)
        }
        return sent
    }

    private fun daysFromDue(date: LocalDate): Long = ChronoUnit.DAYS.between(dueDate, date)

    @Test
    fun `ladder picks the right state around the due date`() {
        assertEquals(null, ReminderPolicy.kindFor(4, 3))
        assertEquals(ReminderKind.DUE_SOON, ReminderPolicy.kindFor(3, 3))
        assertEquals(ReminderKind.DUE_SOON, ReminderPolicy.kindFor(1, 3))
        assertEquals(ReminderKind.DUE_TODAY, ReminderPolicy.kindFor(0, 3))
        assertEquals(ReminderKind.OVERDUE, ReminderPolicy.kindFor(-1, 3))
        assertEquals(ReminderKind.OVERDUE, ReminderPolicy.kindFor(-90, 3))
    }

    @Test
    fun `cycle key is the due date's year-month`() {
        assertEquals("2026-09", ReminderPolicy.cycleKeyFor(LocalDate.of(2026, 9, 1)))
        assertEquals("2026-01", ReminderPolicy.cycleKeyFor(LocalDate.of(2026, 1, 31)))
        assertEquals("2026-10", ReminderPolicy.cycleKeyFor(LocalDate.of(2026, 10, 1)))
    }

    @Test
    fun `due soon and due today each fire exactly once per cycle`() {
        val sent = simulate(reminderDays = 3, dailyOverdueAlerts = false, days = 40)

        val dueSoon = sent.filter { it.second == ReminderKind.DUE_SOON }
        assertEquals(1, dueSoon.size)
        assertEquals(LocalDate.of(2026, 8, 29), dueSoon.first().first)

        val dueToday = sent.filter { it.second == ReminderKind.DUE_TODAY }
        assertEquals(1, dueToday.size)
        assertEquals(dueDate, dueToday.first().first)
    }

    @Test
    fun `overdue escalates then backs off to weekly`() {
        val overdue = simulate(reminderDays = 3, dailyOverdueAlerts = false, days = 40)
            .filter { it.second == ReminderKind.OVERDUE }

        assertEquals(
            listOf(1L, 3L, 7L, 14L, 21L, 28L),
            overdue.map { daysFromDue(it.first) }
        )
    }

    @Test
    fun `daily overdue alerts send once a day`() {
        val overdue = simulate(reminderDays = 3, dailyOverdueAlerts = true, days = 40)
            .filter { it.second == ReminderKind.OVERDUE }

        assertEquals(29, overdue.size)
        assertEquals(1L, daysFromDue(overdue.first().first))
    }

    @Test
    fun `a tenant never gets two reminders on the same day`() {
        val sent = simulate(reminderDays = 3, dailyOverdueAlerts = true, days = 40)
        assertEquals(sent.size, sent.map { it.first }.distinct().size)
    }

    @Test
    fun `nagging stops after the overdue ceiling`() {
        val overdue = simulate(reminderDays = 3, dailyOverdueAlerts = true, days = 120)
            .filter { it.second == ReminderKind.OVERDUE }

        assertEquals(0, overdue.count { daysFromDue(it.first) > ReminderPolicy.MAX_OVERDUE_DAYS })
        assertEquals(ReminderPolicy.MAX_OVERDUE_DAYS.toLong(), daysFromDue(overdue.last().first))
    }

    @Test
    fun `backoff is measured from the last send so missed days do not swallow a reminder`() {
        // Device was off for weeks: last overdue notice went out on day 1, today is day 30.
        assertTrue(
            ReminderPolicy.shouldSend(
                kind = ReminderKind.OVERDUE,
                daysOverdue = 30,
                lastSentOn = dueDate.plusDays(1),
                sendCount = 1,
                today = dueDate.plusDays(30),
                dailyOverdueAlerts = false
            )
        )
    }

    @Test
    fun `a second sweep on the same day sends nothing`() {
        assertFalse(
            ReminderPolicy.shouldSend(
                kind = ReminderKind.OVERDUE,
                daysOverdue = 5,
                lastSentOn = dueDate.plusDays(5),
                sendCount = 1,
                today = dueDate.plusDays(5),
                dailyOverdueAlerts = true
            )
        )
    }

    @Test
    fun `due soon is not repeated later in the same cycle`() {
        assertFalse(
            ReminderPolicy.shouldSend(
                kind = ReminderKind.DUE_SOON,
                daysOverdue = 0,
                lastSentOn = dueDate.minusDays(3),
                sendCount = 1,
                today = dueDate.minusDays(2),
                dailyOverdueAlerts = false
            )
        )
    }

    @Test
    fun `a zero-day reminder window still notifies on the due date`() {
        val sent = simulate(reminderDays = 0, dailyOverdueAlerts = false, days = 5, startOffset = -3)

        assertEquals(0, sent.count { it.second == ReminderKind.DUE_SOON })
        assertEquals(1, sent.count { it.second == ReminderKind.DUE_TODAY })
    }
}
