package ai.sach.libraryup.notifications

import ai.sach.libraryup.ui.navigation.AppRoutes
import ai.sach.libraryup.ui.screens.settingsScreen.notifications.NotificationIntent
import ai.sach.libraryup.ui.screens.settingsScreen.notifications.ReminderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The notification tap contract.
 *
 * MainActivity used to read a "notification_type" extra that no builder ever wrote, so
 * every tap fell through to the home screen by accident. These pin the mapping so a
 * renamed type or route cannot quietly break routing again.
 */
class NotificationIntentTest {

    @Test
    fun `rent reminders open the tenant list`() {
        assertEquals(AppRoutes.HOME, NotificationIntent.routeFor(NotificationIntent.TYPE_RENT_REMINDER))
        assertEquals(AppRoutes.HOME, NotificationIntent.routeFor(NotificationIntent.TYPE_RENT_REMINDER_SUMMARY))
        assertEquals(AppRoutes.HOME, NotificationIntent.routeFor(NotificationIntent.TYPE_RENT_REMINDER_DIGEST))
    }

    @Test
    fun `overdue maps to the overdue chip, due-soon and due-today both map to due-soon`() {
        // The home screen has no separate "due today" chip - a tenant due today already
        // falls under its "Due Soon" bucket - so both kinds resolve to the same filter.
        assertEquals(NotificationIntent.FILTER_OVERDUE, NotificationIntent.filterFor(ReminderKind.OVERDUE))
        assertEquals(NotificationIntent.FILTER_DUE_SOON, NotificationIntent.filterFor(ReminderKind.DUE_SOON))
        assertEquals(NotificationIntent.FILTER_DUE_SOON, NotificationIntent.filterFor(ReminderKind.DUE_TODAY))
    }

    @Test
    fun `payment confirmations open payment history`() {
        assertEquals(
            AppRoutes.PAYMENT_HISTORY,
            NotificationIntent.routeFor(NotificationIntent.TYPE_PAYMENT_CONFIRMATION)
        )
    }

    @Test
    fun `tenant updates and submissions open the tenant list`() {
        assertEquals(AppRoutes.HOME, NotificationIntent.routeFor(NotificationIntent.TYPE_TENANT_UPDATE))
        assertEquals(AppRoutes.HOME, NotificationIntent.routeFor(NotificationIntent.TYPE_TENANT_SUBMISSION))
    }

    @Test
    fun `property alerts deep link to the property when an id is present`() {
        assertEquals(
            "propertyDetails/prop-42",
            NotificationIntent.routeFor(NotificationIntent.TYPE_PROPERTY_ALERT, "prop-42")
        )
    }

    @Test
    fun `property alerts fall back to the property list without an id`() {
        assertEquals(
            AppRoutes.PROPERTIES,
            NotificationIntent.routeFor(NotificationIntent.TYPE_PROPERTY_ALERT, null)
        )
        assertEquals(
            AppRoutes.PROPERTIES,
            NotificationIntent.routeFor(NotificationIntent.TYPE_PROPERTY_ALERT, "  ")
        )
    }

    @Test
    fun `an unknown or absent type does not navigate`() {
        assertNull(NotificationIntent.routeFor(null))
        assertNull(NotificationIntent.routeFor("SOMETHING_ELSE"))
        // The dead types MainActivity used to switch on must not resolve either.
        assertNull(NotificationIntent.routeFor("RENT_DUE"))
        assertNull(NotificationIntent.routeFor("PROPERTY_ISSUE"))
    }

    @Test
    fun `the deep link route matches the pattern the NavHost registers`() {
        val built = NotificationIntent.routeFor(NotificationIntent.TYPE_PROPERTY_ALERT, "abc")
        val pattern = AppRoutes.PROPERTY_DETAILS_PATTERN.substringBefore("/{")
        assertEquals(pattern, built?.substringBefore("/"))
    }

    @Test
    fun `filter keys match the home screen's TenantFilterOption names exactly`() {
        // HomeScreen resolves EXTRA_FILTER with TenantFilterOption.entries.firstOrNull
        // { it.name == key } - a plain string comparison, not an enum reference, precisely
        // so this package need not depend on the home screen's types. If either side's
        // spelling ever drifts, that lookup silently returns null and the chip is left on
        // ALL instead of the one the notification was about. Pin the literal spelling here.
        assertEquals("OVERDUE", NotificationIntent.FILTER_OVERDUE)
        assertEquals("DUE_SOON", NotificationIntent.FILTER_DUE_SOON)
    }
}
