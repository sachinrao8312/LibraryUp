package ai.sach.rentup.ui.screens.settingsScreen.notifications

import ai.sach.rentup.RentUpApplication
import ai.sach.rentup.data.local.entity.NotificationLog
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The single daily sweep. Walks every unpaid tenant, evaluates the [ReminderPolicy]
 * ladder against live database rows, filters out anything the ledger says was already
 * delivered, and hands the survivors to [NotificationHelper] to post as one grouped
 * batch.
 *
 * Everything is computed at fire time, never at schedule time, so a delayed run still
 * reports the correct number of days.
 */
class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "DailyReminderWorker"

        /** Ledger rows older than this many months are pruned on each run. */
        private const val LEDGER_RETENTION_MONTHS = 3L
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? RentUpApplication ?: return Result.success()

        return try {
            sweep(app)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Reminder sweep failed", e)
            Result.retry()
        }
    }

    private suspend fun sweep(app: RentUpApplication) {
        val preferences = app.notificationPreferences
        if (!preferences.areRentRemindersEnabled()) {
            Log.d(TAG, "Rent reminders disabled, nothing to do")
            return
        }

        val today = LocalDate.now()
        val reminderDays = preferences.getRentReminderDays()
        val dailyOverdueAlerts = preferences.areDailyOverdueAlertsEnabled()

        val ledger = app.database.notificationLogDao()
        val tenants = app.database.tenantDao().getAllTenantsSync()

        val due = mutableListOf<PendingReminder>()

        for (tenant in tenants) {
            if (tenant.isPaid) continue

            val dueDate = runCatching { LocalDate.parse(tenant.dueDate) }.getOrNull()
            if (dueDate == null) {
                Log.w(TAG, "Skipping tenant ${tenant.id}: unparseable due date '${tenant.dueDate}'")
                continue
            }

            val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
            val kind = ReminderPolicy.kindFor(daysUntilDue, reminderDays) ?: continue
            val cycleKey = ReminderPolicy.cycleKeyFor(dueDate)

            val record = ledger.find(tenant.id, cycleKey, kind.name)
            val lastSentOn = record?.lastSentOn?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

            val shouldSend = ReminderPolicy.shouldSend(
                kind = kind,
                daysOverdue = -daysUntilDue,
                lastSentOn = lastSentOn,
                sendCount = record?.sendCount ?: 0,
                today = today,
                dailyOverdueAlerts = dailyOverdueAlerts
            )
            if (!shouldSend) continue

            due += PendingReminder(
                item = RentReminderItem(
                    tenantId = tenant.id,
                    tenantName = tenant.name,
                    amount = tenant.amount.orEmpty(),
                    kind = kind,
                    daysUntilDue = daysUntilDue
                ),
                cycleKey = cycleKey,
                previousSendCount = record?.sendCount ?: 0
            )
        }

        if (due.isEmpty()) {
            Log.d(TAG, "Sweep found nothing to send")
            prune(app, today)
            return
        }

        val posted = app.notificationHelper.postRentReminders(due.map { it.item })

        // Only record delivery if the notifications actually went out. If the system
        // dropped them (permission revoked, channel muted) we must not mark them sent.
        if (posted) {
            for (pending in due) {
                ledger.upsert(
                    NotificationLog(
                        tenantId = pending.item.tenantId,
                        cycleKey = pending.cycleKey,
                        kind = pending.item.kind.name,
                        lastSentOn = today.toString(),
                        sendCount = pending.previousSendCount + 1
                    )
                )
            }
            Log.d(TAG, "Posted ${due.size} reminder(s)")
        } else {
            Log.d(TAG, "Notifications suppressed by system or settings; ledger left untouched")
        }

        prune(app, today)
    }

    private suspend fun prune(app: RentUpApplication, today: LocalDate) {
        val oldest = ReminderPolicy.cycleKeyFor(today.minusMonths(LEDGER_RETENTION_MONTHS))
        runCatching { app.database.notificationLogDao().pruneBefore(oldest) }
            .onFailure { Log.w(TAG, "Ledger prune failed", it) }
    }

    private data class PendingReminder(
        val item: RentReminderItem,
        val cycleKey: String,
        val previousSendCount: Int
    )
}
