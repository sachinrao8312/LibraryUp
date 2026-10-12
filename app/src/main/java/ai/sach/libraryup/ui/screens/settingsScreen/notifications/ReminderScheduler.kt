package ai.sach.libraryup.ui.screens.settingsScreen.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Owns every scheduled rent reminder in the app.
 *
 * There is exactly one recurring job in the system: [DailyReminderWorker], which runs
 * once a day and decides what is owed by reading live tenant rows. Data writes no longer
 * schedule anything — they only call [ensureScheduled], which is idempotent and cheap.
 */
class ReminderScheduler(private val context: Context) {

    companion object {
        const val UNIQUE_DAILY_WORK = "rent_reminder_daily"
        const val WORK_TAG = "rent_reminder_work"

        /** Local hour the daily reminder sweep runs at. */
        const val REMINDER_HOUR = 9
    }

    /**
     * Ensures the daily sweep is scheduled. Safe to call from anywhere, any number of
     * times: [ExistingPeriodicWorkPolicy.KEEP] means an already-running schedule is left
     * alone rather than being torn down and restarted.
     */
    fun ensureScheduled() {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(minutesUntilNextSlot(), TimeUnit.MINUTES)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_DAILY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Stops the daily sweep. Called when rent reminders are switched off. */
    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_DAILY_WORK)
    }

    /** Minutes from now until the next [REMINDER_HOUR] in local time. */
    private fun minutesUntilNextSlot(now: LocalDateTime = LocalDateTime.now()): Long {
        val todaySlot = LocalDateTime.of(now.toLocalDate(), LocalTime.of(REMINDER_HOUR, 0))
        val nextSlot = if (now.isBefore(todaySlot)) todaySlot else todaySlot.plusDays(1)
        return Duration.between(now, nextSlot).toMinutes().coerceAtLeast(1L)
    }
}
