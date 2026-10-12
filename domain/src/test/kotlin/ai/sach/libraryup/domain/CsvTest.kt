package ai.sach.libraryup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTest {

    @Test
    fun `fields are quoted only when needed`() {
        assertEquals("Asha Rao", Csv.field("Asha Rao"))
        assertEquals("\"12, MG Road\"", Csv.field("12, MG Road"))
        assertEquals("\"Ravi \"\"Raju\"\" K\"", Csv.field("Ravi \"Raju\" K"))
    }

    @Test
    fun `document round trips through the parser`() {
        val rows = listOf(listOf("Asha", "12, MG Road", "line1\nline2"), listOf("रवि", "\"quoted\"", ""))
        val doc = Csv.document(listOf("a", "b", "c"), rows)
        assertTrue(doc.startsWith("﻿"))
        assertEquals(listOf(listOf("a", "b", "c")) + rows, Csv.parse(doc))
    }

    @Test
    fun `parser handles LF, blank lines and a missing final newline`() {
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), Csv.parse("a,b\n\nc,d"))
        assertEquals(listOf(listOf("x", "")), Csv.parse("x,\n"))
    }

    @Test
    fun `student import maps columns by header and reports bad lines`() {
        val text = """
            Phone,Name,Exam
            9876543210,Asha,UPSC
            12,Ravi,SSC
            9876500000,,NEET
            ,,
            9123456789,"Meena, K",
        """.trimIndent()
        val result = StudentImport.parse(text)
        assertEquals(listOf("Asha", "Meena, K"), result.rows.map { it.name })
        assertEquals("UPSC", result.rows[0].exam)
        assertEquals(listOf(2, 6), result.rows.map { it.line })
        assertEquals(2, result.errors.size)
        assertTrue(result.errors[0].startsWith("Line 3"))
        assertTrue(result.errors[1].startsWith("Line 4"))
    }

    @Test
    fun `student import needs name and phone columns`() {
        assertEquals(1, StudentImport.parse("name,exam\nAsha,UPSC").errors.size)
        assertEquals(1, StudentImport.parse("").errors.size)
    }
}
