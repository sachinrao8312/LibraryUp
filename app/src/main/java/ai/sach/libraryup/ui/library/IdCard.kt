package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.entity.LibraryProfile
import ai.sach.libraryup.data.repository.StudentOverview
import ai.sach.libraryup.domain.StudentQr
import ai.sach.libraryup.utils.ImageUtils
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.text.TextPaint
import android.text.TextUtils
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File

private const val W = 1012 // ID-1 card ratio (85.6 × 54 mm) at 300 dpi
private const val H = 638
private const val BRAND = 0xFF283593.toInt()

/** Draws the student's ID card (photo, seat, validity, attendance QR) and shares it as a PNG. */
fun shareIdCard(context: Context, profile: LibraryProfile?, s: StudentOverview) {
    val card = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val c = Canvas(card)
    c.drawColor(Color.WHITE)
    fun paint(size: Float, color: Int = Color.BLACK, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; this.color = color; if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    fun text(t: String, x: Float, y: Float, p: TextPaint, maxWidth: Float = W - x - 40f) =
        c.drawText(TextUtils.ellipsize(t, p, maxWidth, TextUtils.TruncateAt.END).toString(), x, y, p)

    c.drawRect(0f, 0f, W.toFloat(), 130f, Paint().apply { color = BRAND })
    text(profile?.name ?: context.getString(R.string.app_name), 40f, 82f, paint(46f, Color.WHITE, bold = true))
    profile?.address?.takeIf { it.isNotBlank() }?.let { text(it, 40f, 118f, paint(24f, 0xCCFFFFFF.toInt())) }

    val photo = s.student.photoUri?.let { ImageUtils.loadBitmapFromUri(context, Uri.parse(it), 400) }
    val photoBox = Rect(40, 170, 260, 440)
    if (photo != null) c.drawBitmap(photo, null, photoBox, Paint(Paint.FILTER_BITMAP_FLAG))
    else c.drawRect(photoBox, Paint().apply { color = 0xFFE0E0E0.toInt() })

    val body = paint(30f)
    text(s.student.name, 300f, 215f, paint(42f, bold = true), 380f)
    var y = 265f
    fun line(t: String) { text(t, 300f, y, body, 380f); y += 45f }
    if (s.student.fatherName.isNotBlank()) line(context.getString(R.string.receipt_father, s.student.fatherName))
    line(s.student.phone)
    line(context.getString(R.string.receipt_seat_shift, s.seat?.label ?: "-", s.shift?.name ?: "-"))
    s.membership?.let { line(context.getString(R.string.id_valid_till, it.dueDate.minusDays(1).pretty())) }

    val qr = BarcodeEncoder().encodeBitmap(StudentQr.encode(s.student.id), BarcodeFormat.QR_CODE, 260, 260)
    c.drawBitmap(qr, 712f, 160f, null)
    text(context.getString(R.string.id_scan_hint), 712f, 450f, paint(20f, 0xFF666666.toInt()), 260f)
    profile?.phone?.takeIf { it.isNotBlank() }?.let { text(it, 40f, 600f, paint(26f, BRAND, bold = true)) }

    val file = File(File(context.cacheDir, "id-cards").apply { mkdirs() }, "id-${s.student.id}.png")
    file.outputStream().use { card.compress(Bitmap.CompressFormat.PNG, 100, it) }
    shareFile(context, file, "image/png", s.student.name, context.getString(R.string.action_id_card))
}
