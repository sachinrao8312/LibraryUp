package ai.sach.libraryup.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Free vs Pro. Pro = bought, inside the trial, or a rewarded-ad pass. Nothing already entered is
 * ever locked: the free tier only stops *adding* seats past the limit and hides Pro extras.
 */
object ProRules {
    const val TRIAL_DAYS = 30
    const val FREE_SEAT_LIMIT = 50

    fun trialDaysLeft(trialStartedOn: LocalDate, today: LocalDate, trialDays: Int = TRIAL_DAYS): Int =
        ChronoUnit.DAYS.between(today, trialStartedOn.plusDays(trialDays.toLong())).toInt().coerceAtLeast(0)

    fun isPro(purchased: Boolean, trialDaysLeft: Int, passUntilMillis: Long, nowMillis: Long): Boolean =
        purchased || trialDaysLeft > 0 || nowMillis < passUntilMillis

    fun canAddSeats(current: Int, adding: Int, pro: Boolean, limit: Int = FREE_SEAT_LIMIT): Boolean =
        pro || current + adding <= limit
}
