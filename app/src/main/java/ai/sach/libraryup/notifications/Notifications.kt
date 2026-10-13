package ai.sach.libraryup.notifications

import ai.sach.libraryup.LibraryUpApplication
import ai.sach.libraryup.MainActivity
import ai.sach.libraryup.R
import ai.sach.libraryup.domain.DueStatus
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** On/off switch for the owner's morning digest. Stored in plain SharedPreferences. */
class NotificationPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("libraryup_notifications", Context.MODE_PRIVATE)

    var dailyDigestEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIGEST, true)
        set(value) = prefs.edit().putBoolean(KEY_DIGEST, value).apply()

    var permissionRequested: Boolean
        get() = prefs.getBoolean(KEY_ASKED, false)
        set(value) = prefs.edit().putBoolean(KEY_ASKED, value).apply()

    private companion object {
        const val KEY_DIGEST = "daily_digest_enabled"
        const val KEY_ASKED = "permission_requested"
    }
}

/**
 * Notifications go to the library owner only - students are reached through WhatsApp,
 * from the owner's own phone. One channel, one daily digest.
 */
class NotificationHelper(private val context: Context) {

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DIGEST,
                context.getString(R.string.notification_channel_digest),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.notification_channel_digest_desc) }
        )
    }

    fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun postDigest(dueToday: Int, overdue: Int, expiring: Int, followUps: Int) {
        if (!canPost() || dueToday + overdue + expiring + followUps == 0) return
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = context.getString(R.string.notification_digest_text, dueToday, overdue, expiring) +
            if (followUps > 0) "\n" + context.getString(R.string.notification_digest_follow_ups, followUps) else ""
        val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(context.getString(R.string.notification_digest_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_DIGEST, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

    companion object {
        const val CHANNEL_DIGEST = "owner_digest"
        private const val ID_DIGEST = 1001
    }
}

/** Runs once a day: counts who is due, overdue or expiring and tells the owner. */
class DailyDigestWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as LibraryUpApplication
        if (!app.notificationPrefs.dailyDigestEnabled) return Result.success()
        val state = app.libraryRepository.state.first()
        val statuses = state.students.mapNotNull { it.status }
        app.notificationHelper.postDigest(
            dueToday = statuses.count { it == DueStatus.DUE_TODAY },
            overdue = statuses.count { it == DueStatus.OVERDUE || it == DueStatus.EXPIRED },
            expiring = statuses.count { it == DueStatus.EXPIRING_SOON },
            followUps = app.libraryRepository.followUpsDue(LocalDate.now())
        )
        return Result.success()
    }
}

class ReminderScheduler(private val context: Context) {

    fun ensureScheduled() {
        val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(minutesUntil(DIGEST_HOUR), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
    }

    private fun minutesUntil(hour: Int, now: LocalDateTime = LocalDateTime.now()): Long {
        val today = LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour, 0))
        val next = if (now.isBefore(today)) today else today.plusDays(1)
        return Duration.between(now, next).toMinutes().coerceAtLeast(1)
    }

    companion object {
        private const val UNIQUE_WORK = "owner_daily_digest"
        const val DIGEST_HOUR = 8
    }
}
