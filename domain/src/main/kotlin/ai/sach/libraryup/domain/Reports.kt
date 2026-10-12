package ai.sach.libraryup.domain

import java.time.LocalDate
import java.time.YearMonth

data class MoneyEntry(val date: LocalDate, val amount: Rupees)

data class MonthlyProfit(val month: YearMonth, val income: Rupees, val expenses: Rupees) {
    val profit: Rupees get() = income - expenses
}

object Reports {

    /** Income vs expenses per calendar month, oldest first, months with no entries skipped. */
    fun profitByMonth(payments: List<MoneyEntry>, expenses: List<MoneyEntry>): List<MonthlyProfit> {
        val income = payments.groupBy { YearMonth.from(it.date) }.mapValues { (_, v) -> v.sumOf { it.amount } }
        val spent = expenses.groupBy { YearMonth.from(it.date) }.mapValues { (_, v) -> v.sumOf { it.amount } }
        return (income.keys + spent.keys).sorted().map {
            MonthlyProfit(it, income[it] ?: 0, spent[it] ?: 0)
        }
    }

    /** Share of seat-shift slots in use, 0–100. A full-day booking uses every shift. */
    fun occupancyPercent(
        activeSeatCount: Int,
        shifts: Map<Long, ShiftWindow>,
        bookings: Collection<SeatBooking>,
        day: LocalDate
    ): Int {
        if (activeSeatCount <= 0 || shifts.isEmpty()) return 0
        val total = activeSeatCount * shifts.size
        val used = bookings.filter { it.activeOn(day) }
            .groupBy { it.seatId }
            .values
            .sumOf { seatBookings -> shifts.values.count { w -> seatBookings.any { it.window.overlaps(w) } } }
        return (used * 100 / total).coerceIn(0, 100)
    }

    /** Occupancy for one shift only. */
    fun shiftOccupancyPercent(
        activeSeatCount: Int,
        window: ShiftWindow,
        bookings: Collection<SeatBooking>,
        day: LocalDate
    ): Int {
        if (activeSeatCount <= 0) return 0
        val used = bookings.filter { it.activeOn(day) && it.window.overlaps(window) }.map { it.seatId }.toSet().size
        return (used * 100 / activeSeatCount).coerceIn(0, 100)
    }
}
