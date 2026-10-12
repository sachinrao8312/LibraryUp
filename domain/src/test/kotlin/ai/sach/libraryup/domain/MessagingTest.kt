package ai.sach.libraryup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MessagingTest {

    @Test
    fun `receipt numbers are padded and use the payment year`() {
        assertEquals("LU-2026-000042", ReceiptNumber.format("lu", 42, LocalDate.of(2026, 10, 1)))
        assertEquals("LU-2027-1234567", ReceiptNumber.format(" ", 1234567, LocalDate.of(2027, 1, 1)))
        assertEquals("SVL-2026-000001", ReceiptNumber.format("svl", 1, LocalDate.of(2026, 1, 1)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `receipt sequence starts at one`() {
        ReceiptNumber.format("LU", 0, LocalDate.of(2026, 1, 1))
    }

    @Test
    fun `upi link is encoded`() {
        val link = UpiLink.build("owner@okicici", "Saraswati Library", 1200, "Fee Mar - Asha")
        assertEquals("upi://pay?pa=owner%40okicici&pn=Saraswati%20Library&am=1200.00&cu=INR&tn=Fee%20Mar%20-%20Asha", link)
        assertEquals("upi://pay?pa=a.b%40ybl&pn=X&cu=INR", UpiLink.build("a.b@ybl", "X", null, null))
    }

    @Test
    fun `vpa validation`() {
        assertTrue(UpiLink.isValidVpa("9876543210@paytm"))
        assertTrue(UpiLink.isValidVpa(" name.surname-1@okhdfcbank "))
        assertFalse(UpiLink.isValidVpa("no-at-sign"))
        assertFalse(UpiLink.isValidVpa("a@"))
        assertFalse(UpiLink.isValidVpa("a b@ybl"))
    }

    @Test
    fun `templates fill placeholders and drop lines with only empty ones`() {
        val template = MessageTemplates.default(MessageTemplates.Kind.FEE_DUE, hindi = false)
        val filled = MessageTemplates.render(
            template,
            mapOf("name" to "Asha", "amount" to "1200", "library" to "Saraswati", "due_date" to "05 Apr 2026", "upi_link" to "")
        )
        assertEquals("Hello Asha, your fee of ₹1200 at Saraswati is due on 05 Apr 2026.\nThank you!", filled)
    }

    @Test
    fun `unknown placeholders stay visible`() {
        assertEquals("Hi {nmae}", MessageTemplates.render("Hi {nmae}", mapOf("name" to "Asha")))
    }

    @Test
    fun `every template exists in both languages`() {
        MessageTemplates.Kind.values().forEach {
            assertTrue(MessageTemplates.default(it, hindi = false).isNotBlank())
            assertTrue(MessageTemplates.default(it, hindi = true).contains("{"))
        }
    }

    @Test
    fun `phone numbers for whatsapp`() {
        assertEquals("919876543210", PhoneNumbers.forWhatsApp("98765 43210"))
        assertEquals("919876543210", PhoneNumbers.forWhatsApp("09876543210"))
        assertEquals("919876543210", PhoneNumbers.forWhatsApp("+91-98765-43210"))
        assertNull(PhoneNumbers.forWhatsApp("12345"))
    }

    @Test
    fun `profit by month`() {
        val p = listOf(MoneyEntry(LocalDate.of(2026, 3, 2), 1000), MoneyEntry(LocalDate.of(2026, 3, 20), 500), MoneyEntry(LocalDate.of(2026, 5, 1), 700))
        val e = listOf(MoneyEntry(LocalDate.of(2026, 3, 31), 1200), MoneyEntry(LocalDate.of(2026, 4, 1), 100))
        val r = Reports.profitByMonth(p, e)
        assertEquals(listOf(YearMonth.of(2026, 3), YearMonth.of(2026, 4), YearMonth.of(2026, 5)), r.map { it.month })
        assertEquals(listOf(300L, -100L, 700L), r.map { it.profit })
    }
}
