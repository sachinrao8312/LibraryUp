package ai.sach.libraryup.ui.screens.settingsScreen.notifications

import ai.sach.libraryup.ui.navigation.AppRoutes

/**
 * The contract between a notification and the screen it opens.
 *
 * Every notification the app posts carries [EXTRA_TYPE], and [routeFor] turns that back
 * into a navigation route. Previously MainActivity read a "notification_type" extra that
 * no builder ever wrote, so every tap fell through to the home screen by accident.
 *
 * Rent reminders additionally carry [EXTRA_FILTER]: the tenant-list chip (see
 * `TenantFilterOption` on the home screen) that already shows the tenants the notification
 * is about, so tapping it lands the user on a pre-filtered list rather than the full,
 * unfiltered tenant roster. The value is a plain string key rather than the enum itself so
 * this package does not need to depend on the home screen's types.
 */
object NotificationIntent {

    const val EXTRA_TYPE = "notification_type"
    const val EXTRA_TENANT_ID = "tenant_id"
    const val EXTRA_PROPERTY_ID = "property_id"
    const val EXTRA_FORM_ID = "formId"
    const val EXTRA_FILTER = "notification_filter"

    const val TYPE_RENT_REMINDER = "RENT_REMINDER"
    const val TYPE_RENT_REMINDER_SUMMARY = "RENT_REMINDER_SUMMARY"
    const val TYPE_RENT_REMINDER_DIGEST = "RENT_REMINDER_DIGEST"
    const val TYPE_PAYMENT_CONFIRMATION = "PAYMENT_CONFIRMATION"
    const val TYPE_TENANT_UPDATE = "TENANT_UPDATE"
    const val TYPE_TENANT_SUBMISSION = "TENANT_SUBMISSION"
    const val TYPE_PROPERTY_ALERT = "PROPERTY_ALERT"

    /**
     * Values for [EXTRA_FILTER]. These are deliberately spelled to match
     * `TenantFilterOption.name` on the home screen (OVERDUE, DUE_SOON) so resolving one
     * is a plain `valueOf`, with no translation table to keep in sync.
     *
     * The home screen has no separate "due today" chip - a tenant due today already falls
     * under its "Due Soon" bucket (anything due within its fixed lookahead window) - so
     * [ai.sach.libraryup.ui.screens.settingsScreen.notifications.ReminderKind.DUE_TODAY] and
     * `DUE_SOON` both resolve to [FILTER_DUE_SOON] here.
     */
    const val FILTER_OVERDUE = "OVERDUE"
    const val FILTER_DUE_SOON = "DUE_SOON"

    /**
     * The route a notification of [type] should open, or null when it should just bring
     * the app to the front without navigating.
     *
     * Rent reminders land on the tenant list rather than a tenant detail screen because
     * the app does not have one; when it grows one, this is the single place to change.
     */
    fun routeFor(type: String?, propertyId: String? = null): String? = when (type) {
        TYPE_RENT_REMINDER,
        TYPE_RENT_REMINDER_SUMMARY,
        TYPE_RENT_REMINDER_DIGEST,
        TYPE_TENANT_UPDATE,
        TYPE_TENANT_SUBMISSION -> AppRoutes.HOME

        TYPE_PAYMENT_CONFIRMATION -> AppRoutes.PAYMENT_HISTORY

        TYPE_PROPERTY_ALERT ->
            if (!propertyId.isNullOrBlank()) AppRoutes.propertyDetails(propertyId)
            else AppRoutes.PROPERTIES

        else -> null
    }

    /** The [ReminderKind] this notification was about, mapped to a home-screen filter chip. */
    fun filterFor(kind: ReminderKind): String =
        if (kind == ReminderKind.OVERDUE) FILTER_OVERDUE else FILTER_DUE_SOON
}

/**
 * Where a notification tap should take the user: a navigation route, and either - for rent
 * reminders - the tenant-list filter chip that should already be selected when they land
 * there, so a landlord with many tenants sees the relevant subset immediately instead of
 * the full list, or - for a new tenant submission - the local id of the tenant it created,
 * so Home can open that tenant's details on top of the list rather than leaving the
 * landlord to find them.
 */
data class PendingNotificationTarget(
    val route: String,
    val filterKey: String? = null,
    val tenantIdToOpen: Long? = null
)
