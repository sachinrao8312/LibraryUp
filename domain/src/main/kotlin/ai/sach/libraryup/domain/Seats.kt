package ai.sach.libraryup.domain

import java.time.LocalDate

/**
 * A shift's daily time window in minutes from midnight. [endMinute] is exclusive and may
 * be 1440 (midnight). A window that crosses midnight (22:00–06:00) has
 * [endMinute] <= [startMinute].
 */
data class ShiftWindow(val startMinute: Int, val endMinute: Int) {
    init {
        require(startMinute in 0 until MINUTES_PER_DAY) { "startMinute out of range" }
        require(endMinute in 0..MINUTES_PER_DAY) { "endMinute out of range" }
        require(startMinute != endMinute || endMinute == 0) { "empty shift window" }
    }

    /** Covers the whole day (00:00–24:00). */
    val isFullDay: Boolean get() = startMinute == 0 && (endMinute == MINUTES_PER_DAY || endMinute == 0)

    /** The window as one or two [start, end) ranges within a single day. */
    private fun ranges(): List<IntRange> = when {
        isFullDay -> listOf(0 until MINUTES_PER_DAY)
        endMinute > startMinute -> listOf(startMinute until endMinute)
        else -> listOfNotNull(
            startMinute until MINUTES_PER_DAY,
            if (endMinute > 0) 0 until endMinute else null
        )
    }

    fun overlaps(other: ShiftWindow): Boolean =
        ranges().any { a -> other.ranges().any { b -> a.first <= b.last && b.first <= a.last } }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
        val FULL_DAY = ShiftWindow(0, MINUTES_PER_DAY)

        fun of(startHour: Int, startMin: Int, endHour: Int, endMin: Int): ShiftWindow =
            ShiftWindow(startHour * 60 + startMin, endHour * 60 + endMin)
    }
}

/** One booking of a seat: a student in a shift, between two dates. */
data class SeatBooking(
    val id: Long,
    val seatId: Long,
    val shiftId: Long,
    val window: ShiftWindow,
    val from: LocalDate,
    /** Exclusive: the booking no longer holds the seat on this day. */
    val until: LocalDate
) {
    init { require(until.isAfter(from)) { "booking must last at least one day" } }

    fun overlapsDates(otherFrom: LocalDate, otherUntil: LocalDate): Boolean =
        from.isBefore(otherUntil) && otherFrom.isBefore(until)

    fun activeOn(day: LocalDate): Boolean = !day.isBefore(from) && day.isBefore(until)
}

object SeatRules {

    /**
     * Existing bookings that stop [candidate] from being made: same seat, overlapping
     * dates, and overlapping shift hours. A booking with the same id as the candidate
     * (i.e. the one being edited) is ignored.
     */
    fun conflicts(candidate: SeatBooking, existing: Collection<SeatBooking>): List<SeatBooking> =
        existing.filter {
            it.id != candidate.id &&
                it.seatId == candidate.seatId &&
                it.overlapsDates(candidate.from, candidate.until) &&
                it.window.overlaps(candidate.window)
        }

    fun isFree(candidate: SeatBooking, existing: Collection<SeatBooking>): Boolean =
        conflicts(candidate, existing).isEmpty()
}

/** How a seat looks on the seat map for one day. */
enum class SeatState {
    /** Nobody in any shift. */
    FREE,

    /** Some shifts are taken, others are free. */
    PARTIAL,

    /** Every shift is taken (or a full-day booking holds it). */
    FULL,

    /** Occupied, and at least one occupant's fee is overdue. Takes priority for colour. */
    OVERDUE,

    /** Seat switched off by the owner (broken, reserved for staff, …). */
    INACTIVE
}

object SeatMap {

    /**
     * State of one seat on [day].
     *
     * @param shifts every active shift of the library, by id.
     * @param bookings bookings of this seat only.
     * @param overdueBookingIds bookings whose student has an overdue fee.
     * @param onlyShiftId when the map is filtered to one shift, the state is just
     *   FREE / FULL / OVERDUE for that shift's hours.
     */
    fun stateOf(
        active: Boolean,
        shifts: Map<Long, ShiftWindow>,
        bookings: Collection<SeatBooking>,
        overdueBookingIds: Set<Long>,
        day: LocalDate,
        onlyShiftId: Long? = null
    ): SeatState {
        if (!active) return SeatState.INACTIVE
        val today = bookings.filter { it.activeOn(day) }

        if (onlyShiftId != null) {
            val window = shifts[onlyShiftId] ?: return SeatState.FREE
            val holders = today.filter { it.window.overlaps(window) }
            return when {
                holders.isEmpty() -> SeatState.FREE
                holders.any { it.id in overdueBookingIds } -> SeatState.OVERDUE
                else -> SeatState.FULL
            }
        }

        if (today.isEmpty()) return SeatState.FREE
        if (today.any { it.id in overdueBookingIds }) return SeatState.OVERDUE
        val everyShiftTaken = shifts.values.all { w -> today.any { it.window.overlaps(w) } }
        return if (everyShiftTaken || today.any { it.window.isFullDay }) SeatState.FULL else SeatState.PARTIAL
    }

    /** Ids of shifts with no booking on this seat on [day]. */
    fun freeShiftIds(
        shifts: Map<Long, ShiftWindow>,
        bookings: Collection<SeatBooking>,
        day: LocalDate
    ): Set<Long> {
        val today = bookings.filter { it.activeOn(day) }
        return shifts.filterValues { w -> today.none { it.window.overlaps(w) } }.keys
    }
}
