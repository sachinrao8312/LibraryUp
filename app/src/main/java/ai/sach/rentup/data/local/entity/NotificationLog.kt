package ai.sach.rentup.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Delivery ledger for rent reminders.
 *
 * One row per (tenant, billing cycle, reminder kind). This is what makes the daily
 * reminder worker idempotent: the worker may run late, early, or twice, and the
 * ledger still guarantees each reminder is delivered at most once per cycle
 * (for DUE_SOON / DUE_TODAY) or at most once per day on the configured cadence
 * (for OVERDUE).
 *
 * @param cycleKey  The due date's year-month, e.g. "2026-09". A new month is a new cycle.
 * @param kind      [ai.sach.rentup.ui.screens.settingsScreen.notifications.ReminderKind] name.
 * @param lastSentOn ISO local date the reminder was last posted.
 * @param sendCount How many times this reminder has been posted in this cycle.
 */
@Entity(
    tableName = "notification_log",
    primaryKeys = ["tenantId", "cycleKey", "kind"],
    indices = [Index(value = ["tenantId"])],
    foreignKeys = [
        ForeignKey(
            entity = Tenant::class,
            parentColumns = ["id"],
            childColumns = ["tenantId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NotificationLog(
    val tenantId: Long,
    val cycleKey: String,
    val kind: String,
    val lastSentOn: String,
    val sendCount: Int
)
