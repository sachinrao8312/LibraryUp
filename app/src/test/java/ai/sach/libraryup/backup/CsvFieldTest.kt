package ai.sach.libraryup.backup

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The RFC 4180 quoting rule [ai.sach.libraryup.data.backup.BackupManager]'s CSV export uses.
 *
 * `BackupManager.csvField` is private and the export method it feeds needs a live
 * Context/Uri/Room database, so this pins the escaping rule itself - reproduced verbatim
 * rather than exercised through the real class - against the cases that actually occur in
 * this schema: addresses with commas, a name with a quote in it, and a round-trip through
 * an independent parser to catch an escaping bug that looks right by eye but reads back
 * wrong.
 */
class CsvFieldTest {

    private fun csvField(value: String): String {
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuoting) "\"${value.replace("\"", "\"\"")}\"" else value
    }

    @Test
    fun `plain values are left unquoted`() {
        assertEquals("Asha Rao", csvField("Asha Rao"))
        assertEquals("", csvField(""))
    }

    @Test
    fun `a comma - the common case, an address - triggers quoting`() {
        assertEquals("\"Flat 4B, Sunrise Apts\"", csvField("Flat 4B, Sunrise Apts"))
    }

    @Test
    fun `an embedded quote is doubled and the field quoted`() {
        assertEquals("\"Tenant \"\"Junior\"\" Rao\"", csvField("Tenant \"Junior\" Rao"))
    }

    @Test
    fun `an embedded newline triggers quoting`() {
        assertEquals("\"Line1\nLine2\"", csvField("Line1\nLine2"))
    }

    @Test
    fun `a full row survives a round trip through an independent RFC4180 parser`() {
        val row = listOf(
            "Asha \"A\" Rao", "9990001111", "Sunrise Apts", "1 Main St, Springfield",
            "2", "15000", "2026-10-01", "Paid", "Old Rd\nNear Park"
        )
        val line = row.joinToString(",") { csvField(it) }

        assertEquals(row, parseCsvLine(line))
    }

    /** Minimal RFC 4180 line parser, used only to prove [csvField]'s output reads back correctly. */
    private fun parseCsvLine(s: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                inQuotes && c == '"' && i + 1 < s.length && s[i + 1] == '"' -> {
                    sb.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    fields.add(sb.toString()); sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        fields.add(sb.toString())
        return fields
    }
}
