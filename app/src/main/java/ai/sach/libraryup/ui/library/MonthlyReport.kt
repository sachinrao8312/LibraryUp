package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.Expense
import ai.sach.libraryup.data.repository.LibraryState
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.time.YearMonth

/** One-page PDF for [month]: collections by mode, expenses by category, profit, dues and admissions. */
fun shareMonthlyReport(context: Context, state: LibraryState, expenses: List<Expense>, month: YearMonth) {
    val payments = state.payments.filter { !it.deleted && YearMonth.from(it.paidOn) == month }
    val spent = expenses.filter { !it.deleted && YearMonth.from(it.date) == month }
    val income = payments.sumOf { it.amount }
    val outgo = spent.sumOf { it.amount }

    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(420, 595, 1).create()) // A5 in points
    val c = page.canvas
    val title = Paint().apply { textSize = 16f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
    val body = Paint().apply { textSize = 10f; isAntiAlias = true }
    val bold = Paint(body).apply { typeface = Typeface.DEFAULT_BOLD }
    var y = 40f
    fun line(label: String, amount: String? = null, paint: Paint = body) {
        c.drawText(label, 30f, y, paint)
        amount?.let { c.drawText(it, 390f - paint.measureText(it), y, paint) }
        y += 16f
    }

    line(state.profile?.name ?: context.getString(R.string.app_name), paint = title)
    line(context.getString(R.string.report_title, month.format(monthFormat)), paint = bold)
    y += 10f
    line(context.getString(R.string.label_income), rupees(income), bold)
    payments.groupBy { it.mode }.forEach { (mode, list) -> line("   " + context.getString(mode.label()), rupees(list.sumOf { it.amount })) }
    line(context.getString(R.string.label_expenses), rupees(outgo), bold)
    spent.groupBy { it.category }.forEach { (category, list) -> line("   $category", rupees(list.sumOf { it.amount })) }
    line(context.getString(R.string.label_profit), rupees(income - outgo), title)
    y += 10f
    line(context.getString(R.string.dash_pending_amount), rupees(state.students.sumOf { it.balance }))
    line(context.getString(R.string.dash_active_students), state.students.count { it.membership != null && it.status != null }.toString())
    line(context.getString(R.string.report_admissions), state.students.count { YearMonth.from(it.student.joinDate) == month }.toString())
    line(context.getString(R.string.report_receipts), payments.size.toString())
    doc.finishPage(page)

    val file = File(File(context.cacheDir, "reports").apply { mkdirs() }, "report-$month.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()
    shareFile(context, file, "application/pdf", context.getString(R.string.report_title, month.format(monthFormat)), context.getString(R.string.action_monthly_report))
}
