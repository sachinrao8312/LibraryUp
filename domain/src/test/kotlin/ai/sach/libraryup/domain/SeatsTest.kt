package ai.sach.libraryup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SeatsTest {

    private val morning = ShiftWindow.of(6, 0, 12, 0)
    private val evening = ShiftWindow.of(12, 0, 18, 0)
    private val night = ShiftWindow.of(18, 0, 24, 0)
    private val lateNight = ShiftWindow.of(22, 0, 6, 0) // crosses midnight
    private val fullDay = ShiftWindow.FULL_DAY

    private val jan1 = LocalDate.of(2026, 1, 1)
    private val feb1 = LocalDate.of(2026, 2, 1)
    private val mar1 = LocalDate.of(2026, 3, 1)

    private fun booking(id: Long, seat: Long = 1, shift: Long = 1, window: ShiftWindow = morning,
                        from: LocalDate = jan1, until: LocalDate = feb1) =
        SeatBooking(id, seat, shift, window, from, until)

    @Test
    fun `back to back shifts do not overlap`() {
        assertFalse(morning.overlaps(evening))
        assertFalse(evening.overlaps(night))
        assertTrue(morning.overlaps(ShiftWindow.of(11, 0, 13, 0)))
    }

    @Test
    fun `full day overlaps everything`() {
        listOf(morning, evening, night, lateNight).forEach { assertTrue(fullDay.overlaps(it)); assertTrue(it.overlaps(fullDay)) }
        assertTrue(ShiftWindow(0, 0).isFullDay)
    }

    @Test
    fun `midnight crossing shift overlaps night and early morning only`() {
        assertTrue(lateNight.overlaps(night))
        assertTrue(lateNight.overlaps(ShiftWindow.of(5, 0, 7, 0)))
        assertFalse(lateNight.overlaps(morning))
        assertFalse(lateNight.overlaps(evening))
    }

    @Test
    fun `same seat same shift same dates conflicts`() {
        val existing = listOf(booking(1))
        val candidate = booking(2)
        assertEquals(listOf(existing[0]), SeatRules.conflicts(candidate, existing))
        assertFalse(SeatRules.isFree(candidate, existing))
    }

    @Test
    fun `different shift on the same seat is fine`() {
        assertTrue(SeatRules.isFree(booking(2, shift = 2, window = evening), listOf(booking(1))))
    }

    @Test
    fun `full day booking blocks every shift`() {
        val existing = listOf(booking(1, window = fullDay))
        assertFalse(SeatRules.isFree(booking(2, shift = 2, window = evening), existing))
    }

    @Test
    fun `consecutive date ranges do not conflict because until is exclusive`() {
        assertTrue(SeatRules.isFree(booking(2, from = feb1, until = mar1), listOf(booking(1))))
        assertFalse(SeatRules.isFree(booking(2, from = feb1.minusDays(1), until = mar1), listOf(booking(1))))
    }

    @Test
    fun `other seats and the booking being edited are ignored`() {
        assertTrue(SeatRules.isFree(booking(2, seat = 2), listOf(booking(1))))
        assertTrue(SeatRules.isFree(booking(1, until = mar1), listOf(booking(1))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty booking is rejected`() {
        booking(1, from = jan1, until = jan1)
    }

    private val shifts = mapOf(1L to morning, 2L to evening, 3L to night)
    private val day = LocalDate.of(2026, 1, 10)

    @Test
    fun `seat map states`() {
        assertEquals(SeatState.INACTIVE, SeatMap.stateOf(false, shifts, emptyList(), emptySet(), day))
        assertEquals(SeatState.FREE, SeatMap.stateOf(true, shifts, emptyList(), emptySet(), day))
        val m = booking(1)
        assertEquals(SeatState.PARTIAL, SeatMap.stateOf(true, shifts, listOf(m), emptySet(), day))
        val all = listOf(m, booking(2, shift = 2, window = evening), booking(3, shift = 3, window = night))
        assertEquals(SeatState.FULL, SeatMap.stateOf(true, shifts, all, emptySet(), day))
        assertEquals(SeatState.FULL, SeatMap.stateOf(true, shifts, listOf(booking(4, window = fullDay)), emptySet(), day))
        assertEquals(SeatState.OVERDUE, SeatMap.stateOf(true, shifts, listOf(m), setOf(1L), day))
        // A booking outside the day does not count.
        assertEquals(SeatState.FREE, SeatMap.stateOf(true, shifts, listOf(m), emptySet(), mar1))
    }

    @Test
    fun `seat map filtered by shift`() {
        val m = booking(1)
        assertEquals(SeatState.FULL, SeatMap.stateOf(true, shifts, listOf(m), emptySet(), day, onlyShiftId = 1))
        assertEquals(SeatState.FREE, SeatMap.stateOf(true, shifts, listOf(m), emptySet(), day, onlyShiftId = 2))
        assertEquals(SeatState.OVERDUE, SeatMap.stateOf(true, shifts, listOf(m), setOf(1L), day, onlyShiftId = 1))
        assertEquals(setOf(2L, 3L), SeatMap.freeShiftIds(shifts, listOf(m), day))
    }

    @Test
    fun `occupancy percentages`() {
        val bookings = listOf(booking(1, seat = 1), booking(2, seat = 2, window = fullDay))
        // 2 seats x 3 shifts = 6 slots; seat 1 uses 1, seat 2 uses 3.
        assertEquals(66, Reports.occupancyPercent(2, shifts, bookings, day))
        assertEquals(100, Reports.shiftOccupancyPercent(2, morning, bookings, day))
        assertEquals(50, Reports.shiftOccupancyPercent(2, evening, bookings, day))
        assertEquals(0, Reports.occupancyPercent(0, shifts, bookings, day))
    }
}

class SeatLabelsTest {
    @org.junit.Test
    fun `duplicate labels are per block and case-insensitive`() {
        val seats = listOf("A" to "1", "a" to " 1", "B" to "1", "A" to "2")
        org.junit.Assert.assertEquals(setOf("a" to "1"), SeatLabels.duplicates(seats))
        org.junit.Assert.assertTrue(SeatLabels.isTaken("A", "2", seats))
        org.junit.Assert.assertFalse(SeatLabels.isTaken("B", "2", seats))
    }
}
