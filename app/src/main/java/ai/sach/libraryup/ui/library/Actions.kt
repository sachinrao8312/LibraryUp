package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.LibraryProfile
import ai.sach.libraryup.data.local.entity.Payment
import ai.sach.libraryup.data.repository.StudentOverview
import ai.sach.libraryup.domain.MessageTemplates
import ai.sach.libraryup.domain.PhoneNumbers
import ai.sach.libraryup.domain.UpiLink
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/* ---------------------------------------------------------------- formatting */

private val inr: NumberFormat = NumberFormat.getIntegerInstance(Locale("en", "IN"))

/** ₹12,500 with Indian digit grouping. */
fun rupees(amount: Long): String = "₹" + inr.format(amount)

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

fun LocalDate.pretty(): String = format(dateFormat)

/* ---------------------------------------------------------------- messages */

/** Values for [MessageTemplates] placeholders about one student. */
fun templateValues(profile: LibraryProfile?, s: StudentOverview, amount: Long? = null, receiptNo: String = ""): Map<String, String> {
    val m = s.membership
    val due = amount ?: s.balance.takeIf { it > 0 } ?: m?.let { it.fee - it.discount } ?: 0L
    val upi = profile?.upiVpa?.takeIf { UpiLink.isValidVpa(it) }?.let {
        UpiLink.build(it, profile.name, due.takeIf { a -> a > 0 }, "${s.student.name} fee")
    }.orEmpty()
    return mapOf(
        MessageTemplates.NAME to s.student.name,
        MessageTemplates.AMOUNT to inr.format(due),
        MessageTemplates.DUE_DATE to (m?.dueDate?.pretty() ?: ""),
        MessageTemplates.VALID_TILL to (m?.dueDate?.minusDays(1)?.pretty() ?: ""),
        MessageTemplates.SEAT to (s.seat?.label ?: "-"),
        MessageTemplates.SHIFT to (s.shift?.name ?: "-"),
        MessageTemplates.LIBRARY to (profile?.name ?: ""),
        MessageTemplates.UPI_LINK to upi,
        MessageTemplates.RECEIPT_NO to receiptNo
    )
}

fun buildMessage(
    profile: LibraryProfile?,
    kind: MessageTemplates.Kind,
    s: StudentOverview,
    amount: Long? = null,
    receiptNo: String = "",
    extra: Map<String, String> = emptyMap()
): String {
    val template = profile?.template(kind) ?: MessageTemplates.default(kind, false)
    return MessageTemplates.render(template, templateValues(profile, s, amount, receiptNo) + extra)
}

/* ---------------------------------------------------------------- intents */

/**
 * Opens a chat with [phone] in WhatsApp (or WhatsApp Business) with [text] typed in.
 * Falls back to any app that handles wa.me links, then to the share sheet.
 */
fun openWhatsApp(context: Context, phone: String, text: String, packageName: String = LibraryProfile.WHATSAPP) {
    val number = PhoneNumbers.forWhatsApp(phone)
    if (number == null) {
        Toast.makeText(context, R.string.error_phone_invalid, Toast.LENGTH_SHORT).show()
        return
    }
    val uri = Uri.parse("https://wa.me/$number?text=" + Uri.encode(text))
    val intent = Intent(Intent.ACTION_VIEW, uri).setPackage(packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e2: ActivityNotFoundException) {
            shareText(context, text)
        }
    }
}

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun dial(context: Context, phone: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' })).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.error_no_app, Toast.LENGTH_SHORT).show()
    }
}

/* ---------------------------------------------------------------- receipt PDF */

/**
 * Renders one payment's receipt as an A6 PDF in the cache and opens the share sheet.
 * Uses the platform PdfDocument, so no extra library.
 */
fun shareReceiptPdf(context: Context, profile: LibraryProfile?, s: StudentOverview, payment: Payment) {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(298, 420, 1).create()) // A6 in points
    val c = page.canvas
    val title = Paint().apply { textSize = 16f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
    val body = Paint().apply { textSize = 10f; isAntiAlias = true }
    val bold = Paint(body).apply { typeface = Typeface.DEFAULT_BOLD }
    val small = Paint(body).apply { textSize = 8f; color = 0xFF666666.toInt() }
    var y = 36f
    fun line(text: String, paint: Paint = body, gap: Float = 16f) { c.drawText(text, 20f, y, paint); y += gap }

    line(profile?.name ?: context.getString(R.string.app_name), title, 18f)
    profile?.address?.takeIf { it.isNotBlank() }?.let { line(it, small, 12f) }
    profile?.phone?.takeIf { it.isNotBlank() }?.let { line(it, small, 12f) }
    y += 6f
    line(context.getString(R.string.receipt_title), bold, 18f)
    line(context.getString(R.string.receipt_no, payment.receiptNo))
    line(context.getString(R.string.receipt_date, payment.paidOn.pretty()))
    y += 4f
    line(context.getString(R.string.receipt_student, s.student.name), bold)
    if (s.student.fatherName.isNotBlank()) line(context.getString(R.string.receipt_father, s.student.fatherName))
    line(context.getString(R.string.receipt_seat_shift, s.seat?.label ?: "-", s.shift?.name ?: "-"))
    s.membership?.let {
        line(context.getString(R.string.receipt_period, it.startDate.pretty(), it.dueDate.minusDays(1).pretty()))
        line(context.getString(R.string.receipt_plan, it.planName))
    }
    y += 6f
    line(context.getString(R.string.receipt_amount, rupees(payment.amount), payment.mode.name), title, 20f)
    if (s.balance > 0) line(context.getString(R.string.receipt_balance, rupees(s.balance)))
    if (payment.note.isNotBlank()) line(payment.note, small)
    y = 400f
    line(context.getString(R.string.receipt_footer), small)
    doc.finishPage(page)

    val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val file = File(dir, "receipt-${payment.receiptNo}.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()

    shareFile(
        context, file, "application/pdf",
        buildMessage(profile, MessageTemplates.Kind.RECEIPT, s, payment.amount, payment.receiptNo),
        context.getString(R.string.action_share_receipt)
    )
}

/** Opens the share sheet for a file in the app's cache (WhatsApp is usually first). */
fun shareFile(context: Context, file: File, mime: String, text: String, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType(mime)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, text)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
