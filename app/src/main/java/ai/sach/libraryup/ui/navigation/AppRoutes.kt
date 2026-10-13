package ai.sach.libraryup.ui.navigation

/** Navigation routes. One definition per route so a typo is a compile error. */
object AppRoutes {
    const val HOME = "home"
    const val SEATS = "seats"
    const val STUDENTS = "students?filter={filter}"
    const val PAYMENTS = "payments"
    const val MORE = "more"

    const val STUDENT = "student/{studentId}"
    const val STUDENT_FORM = "studentForm?studentId={studentId}&seatId={seatId}&shiftId={shiftId}"
    const val REMINDERS = "reminders"
    const val ATTENDANCE = "attendance"
    const val EXPENSES = "expenses"
    const val LIBRARY_SETTINGS = "librarySettings"
    const val DATA = "data"
    const val ACTIVITY_LOG = "activityLog"
    const val LOCKERS = "lockers"
    const val ENQUIRIES = "enquiries"
    const val SETTINGS = "settings"

    fun students(filter: String = "ALL") = "students?filter=$filter"
    fun student(id: Long) = "student/$id"
    fun studentForm(studentId: Long? = null, seatId: Long? = null, shiftId: Long? = null) =
        "studentForm?studentId=${studentId ?: -1}&seatId=${seatId ?: -1}&shiftId=${shiftId ?: -1}"
}
