package ai.sach.libraryup.domain

import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Receipt numbers like `LU-2026-000123`: prefix, year of payment, per-library sequence. */
object ReceiptNumber {
    fun format(prefix: String, sequence: Long, paidOn: LocalDate): String {
        require(sequence > 0) { "sequence starts at 1" }
        val cleanPrefix = prefix.trim().uppercase().ifEmpty { "LU" }
        return "%s-%d-%06d".format(cleanPrefix, paidOn.year, sequence)
    }
}

/** `upi://pay` deep links. Every UPI app on Indian phones handles these, with no gateway. */
object UpiLink {

    private val VPA = Regex("^[A-Za-z0-9.\\-_]{2,256}@[A-Za-z][A-Za-z0-9]{1,64}$")

    fun isValidVpa(vpa: String): Boolean = VPA.matches(vpa.trim())

    fun build(vpa: String, payeeName: String, amount: Rupees?, note: String?): String {
        require(isValidVpa(vpa)) { "invalid UPI ID" }
        val params = buildList {
            add("pa" to vpa.trim())
            add("pn" to payeeName.trim())
            if (amount != null && amount > 0) add("am" to "$amount.00")
            add("cu" to "INR")
            if (!note.isNullOrBlank()) add("tn" to note.trim().take(80))
        }
        return "upi://pay?" + params.joinToString("&") { (k, v) -> "$k=${encode(v)}" }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}

/** Reminder and receipt message templates with `{placeholder}` fields. */
object MessageTemplates {

    const val NAME = "name"
    const val AMOUNT = "amount"
    const val DUE_DATE = "due_date"
    const val SEAT = "seat"
    const val SHIFT = "shift"
    const val LIBRARY = "library"
    const val UPI_LINK = "upi_link"
    const val RECEIPT_NO = "receipt_no"
    const val VALID_TILL = "valid_till"

    private val PLACEHOLDER = Regex("\\{([a-z_]+)\\}")

    val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    /**
     * Fills `{key}` placeholders. Unknown keys are left as-is so a typo in an owner's
     * custom template is visible instead of silently vanishing. A line whose only
     * placeholders have empty values (e.g. no UPI ID set) is dropped.
     */
    fun render(template: String, values: Map<String, String>): String =
        template.lines().mapNotNull { line ->
            val keys = PLACEHOLDER.findAll(line).map { it.groupValues[1] }.toList()
            if (keys.isNotEmpty() && keys.all { it in values && values[it].isNullOrBlank() }) {
                null
            } else {
                PLACEHOLDER.replace(line) { m -> values[m.groupValues[1]] ?: m.value }
            }
        }.joinToString("\n").trim()

    enum class Kind { FEE_DUE, EXPIRING, WELCOME, RECEIPT, HOLIDAY }

    /** Built-in templates. Owners can override them per library. */
    fun default(kind: Kind, hindi: Boolean): String = if (hindi) hindiDefaults.getValue(kind) else englishDefaults.getValue(kind)

    private val englishDefaults = mapOf(
        Kind.FEE_DUE to """
            Hello {name}, your fee of ₹{amount} at {library} is due on {due_date}.
            Pay by UPI: {upi_link}
            Thank you!
        """.trimIndent(),
        Kind.EXPIRING to """
            Hello {name}, your seat {seat} ({shift}) at {library} is valid till {valid_till}. Please renew to keep your seat.
            Pay by UPI: {upi_link}
        """.trimIndent(),
        Kind.WELCOME to """
            Welcome to {library}, {name}! Your seat is {seat} ({shift}), valid till {valid_till}. All the best for your preparation.
        """.trimIndent(),
        Kind.RECEIPT to """
            Received ₹{amount} from {name}. Receipt {receipt_no}. Seat {seat} ({shift}), valid till {valid_till}. — {library}
        """.trimIndent(),
        Kind.HOLIDAY to """
            Dear {name}, {library} will remain closed on {due_date}.
        """.trimIndent()
    )

    private val hindiDefaults = mapOf(
        Kind.FEE_DUE to """
            नमस्ते {name}, {library} की आपकी ₹{amount} फीस {due_date} को देय है।
            UPI से भुगतान करें: {upi_link}
            धन्यवाद!
        """.trimIndent(),
        Kind.EXPIRING to """
            नमस्ते {name}, {library} में आपकी सीट {seat} ({shift}) {valid_till} तक मान्य है। सीट बनाए रखने के लिए कृपया रिन्यू करें।
            UPI से भुगतान करें: {upi_link}
        """.trimIndent(),
        Kind.WELCOME to """
            {library} में आपका स्वागत है, {name}! आपकी सीट {seat} ({shift}) है, {valid_till} तक मान्य। आपकी तैयारी के लिए शुभकामनाएँ।
        """.trimIndent(),
        Kind.RECEIPT to """
            {name} से ₹{amount} प्राप्त हुए। रसीद {receipt_no}। सीट {seat} ({shift}), {valid_till} तक मान्य। — {library}
        """.trimIndent(),
        Kind.HOLIDAY to """
            प्रिय {name}, {library} {due_date} को बंद रहेगी।
        """.trimIndent()
    )
}

/** Phone numbers as WhatsApp's `wa.me` / `jid` APIs want them: digits only, with country code. */
object PhoneNumbers {
    fun forWhatsApp(raw: String, defaultCountryCode: String = "91"): String? {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 10 -> defaultCountryCode + digits
            digits.length == 11 && digits.startsWith("0") -> defaultCountryCode + digits.drop(1)
            digits.length in 11..15 -> digits
            else -> null
        }
    }
}

/** Text in the ID-card QR: an app prefix plus the student id, scanned at the desk for attendance. */
object StudentQr {
    private const val PREFIX = "libraryup:student:"

    fun encode(studentId: Long): String = PREFIX + studentId

    fun decode(text: String): Long? =
        if (text.startsWith(PREFIX)) text.removePrefix(PREFIX).toLongOrNull()?.takeIf { it > 0 } else null
}
