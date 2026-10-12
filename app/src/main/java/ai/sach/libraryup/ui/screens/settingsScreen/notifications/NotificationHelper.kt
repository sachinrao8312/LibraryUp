package ai.sach.rentup.ui.screens.settingsScreen.notifications

import ai.sach.rentup.MainActivity
import ai.sach.rentup.R
import ai.sach.rentup.data.local.entity.Tenant
import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

/**
 * One rent reminder ready to be posted. Produced by [DailyReminderWorker] from live
 * tenant rows; [daysUntilDue] is negative when the rent is overdue.
 */
data class RentReminderItem(
    val tenantId: Long,
    val tenantName: String,
    val amount: String,
    val kind: ReminderKind,
    val daysUntilDue: Long
)

/**
 * Owns every notification the app posts: channels, the permission gate, and delivery.
 *
 * All posting funnels through [post], so no call site can bypass the permission or
 * channel checks. Scheduling lives in [ReminderScheduler] — this class only delivers.
 */
class NotificationHelper(private val context: Context) {

    // In-app master toggle. The one place every notification - push-delivered or local -
    // is checked against it, so "Control all app notifications" actually controls all.
    private val preferences by lazy { NotificationPreferencesManager(context) }

    // The status bar icon (setSmallIcon) must stay a flat white silhouette - the system
    // ignores any color/detail in it from API 21 onward. The launcher icon is the only
    // place the actual RentUp logo can show, as the large icon on the notification itself.
    //
    // R.mipmap.ic_launcher is an adaptive icon (an <adaptive-icon> XML, not a raster) on
    // every device that supports one - API 26+, which is effectively all of them against
    // this app's minSdk 24. BitmapFactory.decodeResource only decodes raster resources, so
    // calling it on an adaptive icon silently returns null there: the large icon would
    // never actually show. Resolving it as a Drawable first and rasterizing that handles
    // an adaptive icon, a vector, or a plain bitmap the same way.
    private val largeIcon: Bitmap? by lazy {
        runCatching { ContextCompat.getDrawable(context, R.mipmap.ic_launcher)?.toBitmap() }
            .getOrNull()
    }

    private val accentColor: Int by lazy {
        ContextCompat.getColor(context, R.color.notification_accent)
    }

    /** Every notification starts here so the logo, accent tint and small icon stay consistent. */
    private fun baseBuilder(channelId: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setColor(accentColor)

    companion object {
        // Notification channel IDs
        const val CHANNEL_RENT_DUE = "rent_due"
        const val CHANNEL_RENT_OVERDUE = "rent_overdue"
        const val CHANNEL_PAYMENT_CONFIRMATIONS = "payment_confirmations"
        const val CHANNEL_TENANT_UPDATES = "tenant_updates"
        const val CHANNEL_PROPERTY_ALERTS = "property_alerts"
        const val CHANNEL_APP_UPDATES = "app_updates"

        /** Superseded by [CHANNEL_RENT_DUE] and [CHANNEL_RENT_OVERDUE]; deleted on upgrade. */
        private const val CHANNEL_LEGACY_RENT_REMINDERS = "rent_reminders"

        // Notification groups. Children post silently; one summary per group alerts.
        const val GROUP_RENT_DUE = "ai.sach.rentup.RENT_DUE"
        const val GROUP_RENT_OVERDUE = "ai.sach.rentup.RENT_OVERDUE"

        /**
         * Shared group used only on a mixed day (both overdue and upcoming present).
         * The digest is this group's summary, so the digest is the one bundle the shade
         * shows collapsed; the per-tenant children below nest under it instead of each
         * kind posting its own separate, independently-visible group summary card.
         */
        const val GROUP_RENT_DIGEST = "ai.sach.rentup.RENT_DIGEST"

        // Notification ID bands. Kept far enough apart that per-tenant offsets in one
        // band can never collide with another band.
        private const val ID_BAND_RENT_REMINDER = 1_000_000
        private const val ID_BAND_PAYMENT_CONFIRMATION = 2_000_000
        private const val ID_BAND_SIZE = 500_000L

        const val NOTIFICATION_ID_SUMMARY_RENT_DUE = 900_001
        const val NOTIFICATION_ID_SUMMARY_RENT_OVERDUE = 900_002
        const val NOTIFICATION_ID_TENANT_UPDATE = 900_003
        const val NOTIFICATION_ID_TENANT_SUBMISSION = 900_004
        const val NOTIFICATION_ID_DIGEST = 900_005

        /** InboxStyle lines shown before the digest collapses the rest into "and N more". */
        private const val DIGEST_MAX_LINES = 6

        private const val REQUEST_CODE_MAIN = 0

        private const val TAG = "NotificationHelper"

        /** Stable per-tenant notification id inside a band. */
        private fun idFor(band: Int, tenantId: Long): Int =
            (band + (tenantId % ID_BAND_SIZE)).toInt()
    }

    /* ---------------- Channels ---------------- */

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Rent due soon / due today. Default importance: informative, not urgent.
        val rentDueChannel = NotificationChannel(
            CHANNEL_RENT_DUE,
            "Rent Due",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for rent due soon and rent due today"
            enableLights(true)
            lightColor = Color.YELLOW
        }

        // Overdue rent. High importance, and on its own channel so it can be muted
        // independently of the ordinary due reminders.
        val rentOverdueChannel = NotificationChannel(
            CHANNEL_RENT_OVERDUE,
            "Rent Overdue",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for rent that is past its due date"
            enableLights(true)
            lightColor = Color.RED
            enableVibration(true)
        }

        val paymentConfirmationsChannel = NotificationChannel(
            CHANNEL_PAYMENT_CONFIRMATIONS,
            "Payment Confirmations",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for confirmed rent payments"
            lightColor = Color.GREEN
        }

        val tenantUpdatesChannel = NotificationChannel(
            CHANNEL_TENANT_UPDATES,
            "Tenant Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications about tenant changes"
        }

        val propertyAlertsChannel = NotificationChannel(
            CHANNEL_PROPERTY_ALERTS,
            "Property Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts about your properties"
        }

        val appUpdatesChannel = NotificationChannel(
            CHANNEL_APP_UPDATES,
            "App Updates",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Updates about new features and app changes"
        }

        notificationManager.createNotificationChannels(
            listOf(
                rentDueChannel,
                rentOverdueChannel,
                paymentConfirmationsChannel,
                tenantUpdatesChannel,
                propertyAlertsChannel,
                appUpdatesChannel
            )
        )

        // Retire the single combined channel from earlier versions.
        runCatching { notificationManager.deleteNotificationChannel(CHANNEL_LEGACY_RENT_REMINDERS) }
    }

    /* ---------------- Permission ---------------- */

    /**
     * Whether the app can post notifications at all: the runtime permission on Android 13+,
     * and the system-level app toggle on every version.
     */
    fun areNotificationsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Whether a specific channel is enabled — the user can mute one without muting the app. */
    private fun isChannelEnabled(channelId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(channelId) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /**
     * Reconciles a system-settings opt-in back into the in-app master toggle.
     *
     * When the user re-enables notifications in system settings while the in-app master
     * toggle is off, that transition means they want notifications again - so the toggle is
     * flipped back on. The caller is responsible for re-arming reminders and republishing
     * the FCM opt-in flag.
     *
     * @return true when the master toggle was flipped back on.
     */
    fun reconcileOptInFromSystemSettings(): Boolean {
        val justEnabled = preferences.systemNotificationsJustEnabled(areNotificationsEnabled())
        if (justEnabled && !preferences.getMasterToggle()) {
            preferences.setMasterToggle(true)
            return true
        }
        return false
    }

    fun getNotificationSettingsIntent(): Intent {
        val intent = Intent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.data = Uri.parse("package:${context.packageName}")
        }
        return intent
    }

    /* ---------------- Delivery ---------------- */

    /**
     * The single delivery gate. Returns true only if the notification was actually handed
     * to the system, so callers can record delivery honestly.
     */
    @SuppressLint("MissingPermission") // areNotificationsEnabled() checks POST_NOTIFICATIONS
    private fun post(id: Int, channelId: String, notification: android.app.Notification): Boolean {
        if (!preferences.getMasterToggle()) return false
        if (!areNotificationsEnabled() || !isChannelEnabled(channelId)) return false
        return try {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification $id rejected by the system", e)
            false
        }
    }

    /**
     * Builds the tap target for a notification.
     *
     * Uses SINGLE_TOP rather than CLEAR_TASK so a tap on a running app is delivered to
     * MainActivity.onNewIntent instead of tearing the task down and losing UI state, and
     * always stamps [NotificationIntent.EXTRA_TYPE] so the activity can route the tap.
     */
    private fun contentIntentFor(
        requestCode: Int,
        type: String,
        tenantId: Long? = null,
        propertyId: String? = null,
        filter: String? = null
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationIntent.EXTRA_TYPE, type)
            tenantId?.let { putExtra(NotificationIntent.EXTRA_TENANT_ID, it.toString()) }
            propertyId?.let { putExtra(NotificationIntent.EXTRA_PROPERTY_ID, it) }
            filter?.let { putExtra(NotificationIntent.EXTRA_FILTER, it) }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /* ---------------- Rent reminders ---------------- */

    /**
     * Posts a batch of rent reminders as grouped notifications: each tenant gets its own
     * silent, individually tappable entry, and a summary carries the alert.
     *
     * When only overdue or only upcoming (due-soon/due-today) tenants are present, that
     * group's own summary is the day's one alert and the day's one visible bundle -
     * unchanged from before. When both are present - a common case, since overdue tenants
     * don't stop being overdue just because new ones become due - the two kinds used to
     * each post their own group summary, silenced so only the digest alerted. But
     * `setSilent`/`GROUP_ALERT_SUMMARY` only suppress sound; they don't hide the
     * notification, so the digest and both silenced summaries all remained visible as
     * three separate top-level cards in the shade for what was meant to be one bundle.
     *
     * Now, on a mixed day, every child nests under [GROUP_RENT_DIGEST] with the digest
     * itself as that group's summary, so the shade shows exactly one bundle (the digest,
     * expandable into the individual tenants) instead of the digest plus two redundant
     * per-kind cards repeating the same information.
     *
     * @return true if anything was delivered.
     */
    fun postRentReminders(items: List<RentReminderItem>): Boolean {
        if (items.isEmpty()) return false

        val overdue = items.filter { it.kind == ReminderKind.OVERDUE }
        val upcoming = items.filter { it.kind != ReminderKind.OVERDUE }
        val mixed = overdue.isNotEmpty() && upcoming.isNotEmpty()

        if (mixed) {
            val digestDelivered = postDigest(overdue, upcoming)
            if (digestDelivered) {
                var delivered = true
                delivered = postReminderChildren(overdue, CHANNEL_RENT_OVERDUE, GROUP_RENT_DIGEST) || delivered
                delivered = postReminderChildren(upcoming, CHANNEL_RENT_DUE, GROUP_RENT_DIGEST) || delivered
                return delivered
            }
            // The digest lands on the overdue channel, so it can fail to post while the
            // due channel is still fine (e.g. the overdue channel alone is muted). Fall
            // back to each kind alerting on its own rather than silently dropping the
            // day's due-soon reminders along with it.
        }

        return postSeparateGroups(overdue, upcoming)
    }

    private fun postSeparateGroups(overdue: List<RentReminderItem>, upcoming: List<RentReminderItem>): Boolean {
        var delivered = postReminderGroup(
            items = overdue,
            channelId = CHANNEL_RENT_OVERDUE,
            groupKey = GROUP_RENT_OVERDUE,
            summaryId = NOTIFICATION_ID_SUMMARY_RENT_OVERDUE,
            summaryTitle = if (overdue.size == 1) "Rent Overdue" else "${overdue.size} Rents Overdue"
        )

        delivered = postReminderGroup(
            items = upcoming,
            channelId = CHANNEL_RENT_DUE,
            groupKey = GROUP_RENT_DUE,
            summaryId = NOTIFICATION_ID_SUMMARY_RENT_DUE,
            summaryTitle = if (upcoming.size == 1) "Rent Due" else "${upcoming.size} Rents Due"
        ) || delivered

        return delivered
    }

    private fun postReminderGroup(
        items: List<RentReminderItem>,
        channelId: String,
        groupKey: String,
        summaryId: Int,
        summaryTitle: String
    ): Boolean {
        if (items.isEmpty()) return false

        var delivered = postReminderChildren(items, channelId, groupKey)

        val inbox = NotificationCompat.InboxStyle().setBigContentTitle(summaryTitle)
        items.forEach { inbox.addLine(messageFor(it)) }
        totalAmountOf(items)?.let { inbox.setSummaryText("$it total") }

        val summary = baseBuilder(channelId)
            .setContentTitle(summaryTitle)
            .setContentText(summaryLineFor(items))
            .setStyle(inbox)
            .setPriority(priorityFor(items.first().kind))
            .setContentIntent(
                contentIntentFor(
                    requestCode = summaryId,
                    type = NotificationIntent.TYPE_RENT_REMINDER_SUMMARY,
                    filter = NotificationIntent.filterFor(items.first().kind)
                )
            )
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()

        if (post(summaryId, channelId, summary)) delivered = true

        return delivered
    }

    /** Posts each tenant's own silent, individually tappable entry inside [groupKey]. */
    private fun postReminderChildren(
        items: List<RentReminderItem>,
        channelId: String,
        groupKey: String
    ): Boolean {
        var delivered = false

        items.forEach { item ->
            val notificationId = idFor(ID_BAND_RENT_REMINDER, item.tenantId)
            val message = "${detailFor(item)}. ${callToActionFor(item.kind)}"

            val notification = baseBuilder(channelId)
                .setContentTitle(titleFor(item))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(priorityFor(item.kind))
                .setContentIntent(
                    contentIntentFor(
                        requestCode = notificationId,
                        type = NotificationIntent.TYPE_RENT_REMINDER,
                        tenantId = item.tenantId,
                        filter = NotificationIntent.filterFor(item.kind)
                    )
                )
                .setGroup(groupKey)
                // Children stay silent; the group's summary (or the digest, on a mixed day)
                // is the one thing that alerts.
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .build()

            if (post(notificationId, channelId, notification)) delivered = true
        }

        return delivered
    }

    /**
     * The single alerting notification for a day where both overdue and upcoming tenants
     * are due. Lands on the overdue channel, since overdue exists whenever this is called
     * and is the more urgent of the two - and tapping it selects the Overdue chip, so the
     * most actionable tenants are what the landlord sees first.
     *
     * Declared as [GROUP_RENT_DIGEST]'s summary so it is the one bundle the shade shows
     * collapsed, with the per-tenant children posted by [postReminderChildren] nested
     * underneath it rather than forming their own separate visible cards.
     */
    private fun postDigest(overdue: List<RentReminderItem>, upcoming: List<RentReminderItem>): Boolean {
        val counts = "${overdue.size} overdue, ${upcoming.size} due soon"
        val title = totalAmountOf(overdue + upcoming)?.let { "$it to collect · $counts" } ?: counts

        val inbox = NotificationCompat.InboxStyle().setBigContentTitle(title)
        val allLines = overdue.map { messageFor(it) } + upcoming.map { messageFor(it) }
        allLines.take(DIGEST_MAX_LINES).forEach { inbox.addLine(it) }
        if (allLines.size > DIGEST_MAX_LINES) {
            inbox.addLine("and ${allLines.size - DIGEST_MAX_LINES} more")
        }

        val notification = baseBuilder(CHANNEL_RENT_OVERDUE)
            .setContentTitle(title)
            .setContentText("${overdue.size} overdue and ${upcoming.size} due soon. Tap to view.")
            .setStyle(inbox)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(
                contentIntentFor(
                    requestCode = NOTIFICATION_ID_DIGEST,
                    type = NotificationIntent.TYPE_RENT_REMINDER_DIGEST,
                    filter = NotificationIntent.FILTER_OVERDUE
                )
            )
            .setGroup(GROUP_RENT_DIGEST)
            .setGroupSummary(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()

        return post(NOTIFICATION_ID_DIGEST, CHANNEL_RENT_OVERDUE, notification)
    }

    /** Leads with who and how much, since the title is what survives collapsing. */
    private fun titleFor(item: RentReminderItem): String {
        val status = when (item.kind) {
            ReminderKind.OVERDUE -> "overdue"
            ReminderKind.DUE_TODAY -> "due today"
            ReminderKind.DUE_SOON -> "due soon"
        }
        return "${item.tenantName} · ${item.amount.ifBlank { "Rent" }} $status"
    }

    /** The timing on its own, e.g. "8 days late" - the title already says who and how much. */
    private fun detailFor(item: RentReminderItem): String = when (item.kind) {
        ReminderKind.OVERDUE -> {
            val days = -item.daysUntilDue
            "$days ${if (days == 1L) "day" else "days"} late"
        }
        ReminderKind.DUE_TODAY -> "Due today"
        ReminderKind.DUE_SOON -> {
            val days = item.daysUntilDue
            "Due in $days ${if (days == 1L) "day" else "days"}"
        }
    }

    private fun callToActionFor(kind: ReminderKind): String =
        if (kind == ReminderKind.OVERDUE) "Tap to collect payment." else "Tap to view details."

    /** One self-contained line, for inbox rows and single-item summaries. */
    private fun messageFor(item: RentReminderItem): String =
        listOf(item.tenantName, item.amount.takeIf { it.isNotBlank() }, detailFor(item))
            .filterNotNull()
            .joinToString(" · ")

    private fun summaryLineFor(items: List<RentReminderItem>): String =
        if (items.size == 1) messageFor(items.first())
        else items.joinToString(", ") { it.tenantName }

    /**
     * Sums the reminder amounts when every one of them parses as a number, so the summary
     * can show a total. Returns null if any amount is free-form, rather than showing a
     * figure that is quietly wrong.
     */
    private fun totalAmountOf(items: List<RentReminderItem>): String? {
        if (items.size < 2) return null
        var total = 0L
        for (item in items) {
            val digits = item.amount.filter { it.isDigit() }
            if (digits.isEmpty() || digits.length > 15) return null
            total += digits.toLong()
        }
        val prefix = items.first().amount.takeWhile { !it.isDigit() }.trim()
        return if (prefix.isEmpty()) total.toString() else "$prefix$total"
    }

    private fun priorityFor(kind: ReminderKind): Int =
        if (kind == ReminderKind.OVERDUE) NotificationCompat.PRIORITY_HIGH
        else NotificationCompat.PRIORITY_DEFAULT

    /** Posts a single rent reminder. Used by the test button in notification settings. */
    fun sendRentReminderNotification(tenant: Tenant, daysUntilDue: Int) {
        val kind = when {
            daysUntilDue < 0 -> ReminderKind.OVERDUE
            daysUntilDue == 0 -> ReminderKind.DUE_TODAY
            else -> ReminderKind.DUE_SOON
        }
        postRentReminders(
            listOf(
                RentReminderItem(
                    tenantId = tenant.id,
                    tenantName = tenant.name,
                    amount = tenant.amount.orEmpty(),
                    kind = kind,
                    daysUntilDue = daysUntilDue.toLong()
                )
            )
        )
    }

    /* ---------------- Other notifications ---------------- */

    fun sendPaymentConfirmationNotification(tenant: Tenant, amount: String) {
        val notificationId = idFor(ID_BAND_PAYMENT_CONFIRMATION, tenant.id)
        val notification = baseBuilder(CHANNEL_PAYMENT_CONFIRMATIONS)
            .setContentTitle("Payment Received")
            .setContentText("Received payment of $amount from ${tenant.name}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(
                contentIntentFor(
                    requestCode = notificationId,
                    type = NotificationIntent.TYPE_PAYMENT_CONFIRMATION,
                    tenantId = tenant.id
                )
            )
            .setAutoCancel(true)
            .build()

        post(notificationId, CHANNEL_PAYMENT_CONFIRMATIONS, notification)
    }

    fun sendTenantUpdateNotification(title: String, message: String, tenantId: Long? = null) {
        val requestCode = tenantId?.let { idFor(ID_BAND_PAYMENT_CONFIRMATION, it) } ?: REQUEST_CODE_MAIN
        val notification = baseBuilder(CHANNEL_TENANT_UPDATES)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(
                contentIntentFor(
                    requestCode = requestCode,
                    type = NotificationIntent.TYPE_TENANT_UPDATE,
                    tenantId = tenantId
                )
            )
            .setAutoCancel(true)
            .build()

        post(NOTIFICATION_ID_TENANT_UPDATE, CHANNEL_TENANT_UPDATES, notification)
    }

    /**
     * Shows a notification for new tenant form submissions. Uses a stable id so repeated
     * submissions replace the previous notice instead of stacking without limit.
     */
    fun showNewTenantNotification(count: Int, formId: String? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationIntent.EXTRA_TYPE, NotificationIntent.TYPE_TENANT_SUBMISSION)
            formId?.let { putExtra(NotificationIntent.EXTRA_FORM_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_TENANT_SUBMISSION,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (count == 1) "New Tenant Submission" else "$count New Tenant Submissions"
        val message = if (count == 1) {
            "A new tenant has submitted their information via your onboarding form."
        } else {
            "$count new tenants have submitted their information via your onboarding forms."
        }

        val notification = baseBuilder(CHANNEL_TENANT_UPDATES)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        post(NOTIFICATION_ID_TENANT_SUBMISSION, CHANNEL_TENANT_UPDATES, notification)
    }
}
