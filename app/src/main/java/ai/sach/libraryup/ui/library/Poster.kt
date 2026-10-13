package ai.sach.libraryup.ui.library

import ai.sach.libraryup.R
import ai.sach.libraryup.data.repository.LibraryState
import ai.sach.libraryup.domain.SeatMap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import java.io.File

private const val W = 1080 // 4:5, fits WhatsApp status and Instagram
private const val H = 1350
private const val BRAND = 0xFF283593.toInt()
private const val ACCENT = 0xFFFFC107.toInt()

/** Shares the vacancy poster as a PNG. */
fun shareVacancyPoster(context: Context, state: LibraryState) {
    val file = File(File(context.cacheDir, "posters").apply { mkdirs() }, "vacancy.png")
    file.outputStream().use { vacancyPoster(context, state).compress(Bitmap.CompressFormat.PNG, 100, it) }
    shareFile(context, file, "image/png", context.getString(R.string.poster_headline), context.getString(R.string.action_vacancy_poster))
}

/** A "seats available" poster built from today's free seats per shift. */
fun vacancyPoster(context: Context, state: LibraryState): Bitmap {
    val profile = state.profile
    val bookings = state.bookings.groupBy { it.seatId }
    val shifts = state.activeShifts
    val free = SeatMap.freeSeatsPerShift(
        shifts.associate { it.id to it.window },
        state.seats.filter { it.active }.map { bookings[it.id].orEmpty() },
        state.today
    )

    val poster = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val c = Canvas(poster)
    c.drawColor(BRAND)
    fun paint(size: Float, color: Int = Color.WHITE, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; this.color = color; textAlign = align; if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    fun text(t: String, x: Float, y: Float, p: TextPaint, maxWidth: Float = W - 160f) =
        c.drawText(TextUtils.ellipsize(t, p, maxWidth, TextUtils.TruncateAt.END).toString(), x, y, p)

    libraryLogo(context, profile)?.let { c.drawFit(it, RectF(W / 2f - 90f, 70f, W / 2f + 90f, 250f)) }
    val centre = Paint.Align.CENTER
    text(profile?.name ?: context.getString(R.string.app_name), W / 2f, 340f, paint(64f, bold = true, align = centre))
    text(context.getString(R.string.poster_headline), W / 2f, 470f, paint(96f, ACCENT, bold = true, centre))

    var y = 600f
    shifts.filter { (free[it.id] ?: 0) > 0 }.forEach { shift ->
        c.drawRoundRect(RectF(80f, y - 70f, W - 80f, y + 40f), 24f, 24f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        text("${shift.name}  ${shift.timeLabel}", 120f, y, paint(44f, BRAND, bold = true), 640f)
        text(context.getString(R.string.poster_seats, free.getValue(shift.id)), W - 120f, y, paint(44f, BRAND, bold = true, Paint.Align.RIGHT), 260f)
        y += 140f
    }

    profile?.address?.takeIf { it.isNotBlank() }?.let { text(it, W / 2f, H - 220f, paint(36f, align = centre)) }
    profile?.phone?.takeIf { it.isNotBlank() }?.let {
        text(context.getString(R.string.poster_call, it), W / 2f, H - 140f, paint(56f, ACCENT, bold = true, centre))
    }
    return poster
}
