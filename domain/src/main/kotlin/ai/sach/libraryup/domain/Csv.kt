package ai.sach.libraryup.domain

/**
 * RFC 4180 CSV, enough for exports that open in Excel and for importing a student list
 * an owner typed into Excel or Google Sheets.
 */
object Csv {

    fun field(value: String): String {
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuoting) "\"${value.replace("\"", "\"\"")}\"" else value
    }

    fun row(values: List<String>): String = values.joinToString(",") { field(it) }

    /**
     * Excel only detects UTF-8 (needed for Hindi names) when the file starts with a BOM.
     */
    fun document(header: List<String>, rows: List<List<String>>): String =
        "﻿" + (listOf(header) + rows).joinToString("\r\n") { row(it) } + "\r\n"

    /** Parses a whole CSV document into rows of fields. Tolerates a BOM, CRLF or LF. */
    fun parse(text: String): List<List<String>> {
        val input = text.removePrefix("﻿")
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        var fieldStarted = false

        fun endField() { row.add(field.toString()); field.clear(); fieldStarted = false }
        fun endRow() {
            endField()
            if (!(row.size == 1 && row[0].isEmpty())) rows.add(row.toList())
            row.clear()
        }

        while (i < input.length) {
            val c = input[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < input.length && input[i + 1] == '"') { field.append('"'); i++ }
                    else inQuotes = false
                } else field.append(c)
            } else when (c) {
                '"' -> if (!fieldStarted && field.isEmpty()) { inQuotes = true; fieldStarted = true } else field.append(c)
                ',' -> endField()
                '\r' -> { if (i + 1 < input.length && input[i + 1] == '\n') i++; endRow() }
                '\n' -> endRow()
                else -> { field.append(c); fieldStarted = true }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty() || fieldStarted) endRow()
        return rows
    }
}

/** One row of a student import file, after validation. */
data class StudentImportRow(
    val line: Int,
    val name: String,
    val phone: String,
    val fatherName: String,
    val address: String,
    val exam: String,
    val guardianPhone: String
)

data class ImportResult(val rows: List<StudentImportRow>, val errors: List<String>)

object StudentImport {

    val HEADER = listOf("name", "phone", "father_name", "address", "exam", "guardian_phone")

    /** Accepts the template's header in any column order and any letter case. */
    fun parse(text: String): ImportResult {
        val table = Csv.parse(text)
        if (table.isEmpty()) return ImportResult(emptyList(), listOf("The file is empty."))
        val header = table.first().map { it.trim().lowercase().replace(' ', '_') }
        val index = HEADER.associateWith { header.indexOf(it) }
        if (index.getValue("name") < 0 || index.getValue("phone") < 0) {
            return ImportResult(emptyList(), listOf("Header must contain at least 'name' and 'phone'."))
        }
        val rows = mutableListOf<StudentImportRow>()
        val errors = mutableListOf<String>()
        table.drop(1).forEachIndexed { i, cells ->
            val line = i + 2
            fun cell(key: String) = index.getValue(key).let { if (it in cells.indices) cells[it].trim() else "" }
            val name = cell("name")
            val phone = cell("phone")
            when {
                cells.all { it.isBlank() } -> Unit
                name.isEmpty() -> errors.add("Line $line: name is missing.")
                PhoneNumbers.forWhatsApp(phone) == null -> errors.add("Line $line: phone '$phone' is not valid.")
                else -> rows.add(
                    StudentImportRow(line, name, phone, cell("father_name"), cell("address"), cell("exam"), cell("guardian_phone"))
                )
            }
        }
        return ImportResult(rows, errors)
    }
}
