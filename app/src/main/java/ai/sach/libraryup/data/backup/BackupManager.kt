package ai.sach.libraryup.data.backup

import ai.sach.libraryup.BuildConfig
import ai.sach.libraryup.R
import ai.sach.libraryup.data.local.LibraryUpDatabase
import ai.sach.libraryup.domain.BackupCrypto
import ai.sach.libraryup.domain.Csv
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** What a completed export or import moved, shown to the user afterwards. */
data class BackupSummary(
    val studentCount: Int,
    val paymentCount: Int,
    val attendanceCount: Int,
    /** Student photos and ID proofs carried in the file. */
    val attachmentCount: Int = 0,
    /** Attachments whose bytes could not be read at export time, so were left out. */
    val attachmentsSkipped: Int = 0,
    val encrypted: Boolean = false,
    /** SHA-256 of the data and attachments, ignoring the timestamp: same data, same hash. */
    val contentHash: String = ""
)

/** The file is password-protected and no (or the wrong) password was given. */
class BackupPasswordRequiredException(val wrongPassword: Boolean) : Exception()

/**
 * Exports the whole database (every table) plus student photos and ID proofs to one
 * zip, optionally encrypted with [BackupCrypto], and restores one back.
 *
 * ```
 * libraryup-backup-2026-10-12.zip
 *   backup.json      BackupEnvelope: every row of every table
 *   attachments/     photos and ID proofs, byte for byte
 * ```
 *
 * Restore fully replaces local data inside one transaction; it is not a merge. The
 * caller shows a confirmation before calling [importFrom].
 */
class BackupManager(
    private val context: Context,
    private val database: LibraryUpDatabase
) {
    companion object {
        private const val TAG = "BackupManager"
        const val MIME_TYPE_ZIP = "application/zip"
        const val MIME_TYPE_CSV = "text/csv"

        internal const val MANIFEST_ENTRY = "backup.json"
        internal const val ATTACHMENT_DIR = "attachments/"
        internal const val ATTACHMENT_MARKER = "libraryup-backup:"
        private const val RESTORED_FILES_DIR = "restored_attachments"
        private const val MAX_MANIFEST_BYTES = 64L * 1024 * 1024
        private const val MAX_FILE_BYTES = 512L * 1024 * 1024

        fun suggestedFileName(): String = "libraryup-backup-${LocalDate.now()}.zip"

        /** Gson with ISO dates; LocalDate's private fields must never be reflected over. */
        val gson: Gson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter().nullSafe())
            .create()
    }

    private val lock = Mutex()
    private val dao get() = database.libraryDao()

    // ------------------------------------------------------------------ export

    /**
     * @param secrets password and/or recovery code; empty for an unencrypted backup.
     */
    suspend fun exportTo(uri: Uri, secrets: List<String> = emptyList()): Result<BackupSummary> =
        lock.withLock { withContext(Dispatchers.IO) { runCatching { exportLocked(uri, secrets) } } }
            .onFailure { Log.e(TAG, "Export failed", it) }

    private suspend fun exportLocked(uri: Uri, secrets: List<String>): BackupSummary {
        val envelope = database.withTransaction {
            BackupEnvelope(
                appVersionName = BuildConfig.VERSION_NAME,
                exportedAt = Instant.now().toString(),
                profile = listOfNotNull(dao.getProfile()),
                shifts = dao.getShifts(),
                seats = dao.getSeats(),
                plans = dao.getPlans(),
                students = dao.getStudents(),
                memberships = dao.getMemberships(),
                payments = dao.getPayments(),
                attendance = dao.getAttendance(),
                expenses = dao.getExpenses(),
                lockers = dao.getLockers(),
                enquiries = dao.getEnquiries(),
                activityLog = dao.getLog()
            )
        }

        var included = 0
        var skipped = 0
        val digest = MessageDigest.getInstance("SHA-256")
        val zipBytes = ByteArrayOutputStream()

        ZipOutputStream(zipBytes).use { zip ->
            val students = envelope.students.map { s ->
                val photo = s.photoUri?.let { src ->
                    val entry = ATTACHMENT_DIR + "photo_${s.id}${extensionOf(src, ".jpg")}"
                    if (copyIntoZip(zip, src, entry, digest)) { included++; ATTACHMENT_MARKER + entry } else { skipped++; null }
                }
                val idProof = s.idProofUri?.let { src ->
                    val entry = ATTACHMENT_DIR + "idproof_${s.id}${extensionOf(src, ".jpg")}"
                    if (copyIntoZip(zip, src, entry, digest)) { included++; ATTACHMENT_MARKER + entry } else { skipped++; null }
                }
                s.copy(photoUri = photo, idProofUri = idProof)
            }
            val final = envelope.copy(students = students)
            val json = gson.toJson(final)
            digest.update(gson.toJson(final.copy(exportedAt = "")).toByteArray(Charsets.UTF_8))
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        val bytes = if (secrets.isEmpty()) zipBytes.toByteArray() else BackupCrypto.encrypt(zipBytes.toByteArray(), secrets)
        val out = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException(context.getString(R.string.backup_error_open_write))
        out.use { it.write(bytes) }

        return BackupSummary(
            studentCount = envelope.students.size,
            paymentCount = envelope.payments.size,
            attendanceCount = envelope.attendance.size,
            attachmentCount = included,
            attachmentsSkipped = skipped,
            encrypted = secrets.isNotEmpty(),
            contentHash = digest.digest().joinToString("") { "%02x".format(it) }
        )
    }

    private fun copyIntoZip(zip: ZipOutputStream, uriOrPath: String, entryName: String, digest: MessageDigest): Boolean {
        var entryOpen = false
        return try {
            val input = openAttachment(uriOrPath) ?: return false
            input.use {
                val bytes = it.readBytes()
                digest.update(entryName.toByteArray(Charsets.UTF_8))
                digest.update(bytes)
                zip.putNextEntry(ZipEntry(entryName))
                entryOpen = true
                zip.write(bytes)
                zip.closeEntry()
                entryOpen = false
            }
            true
        } catch (e: Exception) {
            // A photo picked long ago may sit behind a lapsed permission: a missing
            // attachment, not a failed backup.
            Log.w(TAG, "Skipping unreadable attachment: ${e.message}")
            if (entryOpen) runCatching { zip.closeEntry() }
            false
        }
    }

    private fun openAttachment(uriOrPath: String): InputStream? = when {
        uriOrPath.isBlank() || uriOrPath.startsWith(ATTACHMENT_MARKER) -> null
        uriOrPath.startsWith("content://") -> context.contentResolver.openInputStream(Uri.parse(uriOrPath))
        uriOrPath.startsWith("file://") -> Uri.parse(uriOrPath).path?.let { File(it) }?.takeIf { it.isFile }?.inputStream()
        else -> File(uriOrPath).takeIf { it.isFile }?.inputStream()
    }

    // ------------------------------------------------------------------ import

    /** True when the file at [uri] needs a password (or recovery code) to restore. */
    suspend fun isEncrypted(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(6)
                val n = input.read(header)
                n == 6 && BackupCrypto.isEncrypted(header)
            } ?: false
        }.getOrDefault(false)
    }

    suspend fun importFrom(uri: Uri, secret: String? = null): Result<BackupSummary> =
        lock.withLock { withContext(Dispatchers.IO) { runCatching { importLocked(uri, secret) } } }
            .onFailure { Log.e(TAG, "Import failed", it) }

    private suspend fun importLocked(uri: Uri, secret: String?): BackupSummary {
        val raw = context.contentResolver.openInputStream(uri)?.use { readBounded(it, MAX_FILE_BYTES) }
            ?: throw IllegalStateException(context.getString(R.string.backup_error_open_read))

        val encrypted = BackupCrypto.isEncrypted(raw)
        val zipBytes = if (!encrypted) raw else {
            if (secret.isNullOrEmpty()) throw BackupPasswordRequiredException(wrongPassword = false)
            try {
                BackupCrypto.decrypt(raw, secret)
            } catch (e: BackupCrypto.WrongSecretException) {
                // The recovery code is stored without dashes; try that form too.
                try {
                    BackupCrypto.decrypt(raw, BackupCrypto.normalizeRecoveryCode(secret))
                } catch (e2: BackupCrypto.WrongSecretException) {
                    throw BackupPasswordRequiredException(wrongPassword = true)
                }
            }
        }

        val staging = File(context.filesDir, "$RESTORED_FILES_DIR/staging-${System.currentTimeMillis()}")
        try {
            val (envelope, attachments) = readArchive(zipBytes, staging)
            val e = validate(envelope)
            val students = e.students.map {
                it.copy(photoUri = resolve(it.photoUri, attachments), idProofUri = resolve(it.idProofUri, attachments))
            }
            database.withTransaction {
                database.clearAllTables()
                dao.restoreProfile(e.profile)
                dao.restoreShifts(e.shifts)
                dao.restoreSeats(e.seats)
                dao.restorePlans(e.plans)
                dao.restoreStudents(students)
                dao.restoreMemberships(e.memberships)
                dao.restorePayments(e.payments)
                dao.restoreAttendance(e.attendance)
                dao.restoreExpenses(e.expenses)
                dao.restoreLockers(e.lockers)
                dao.restoreEnquiries(e.enquiries)
                dao.restoreLog(e.activityLog)
            }
            File(context.filesDir, RESTORED_FILES_DIR).listFiles()
                ?.filter { it.absolutePath != staging.absolutePath }
                ?.forEach { it.deleteRecursively() }
            return BackupSummary(students.size, e.payments.size, e.attendance.size, attachments.size, encrypted = encrypted)
        } catch (ex: Exception) {
            staging.deleteRecursively()
            if (ex is JsonParseException) throw IllegalArgumentException(context.getString(R.string.backup_error_not_libraryup))
            throw ex
        }
    }

    private fun readArchive(bytes: ByteArray, staging: File): Pair<BackupEnvelope?, Map<String, String>> {
        var envelope: BackupEnvelope? = null
        val attachments = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    entry.isDirectory -> Unit
                    name == MANIFEST_ENTRY ->
                        envelope = gson.fromJson(String(readBounded(zip, MAX_MANIFEST_BYTES), Charsets.UTF_8), BackupEnvelope::class.java)
                    name.startsWith(ATTACHMENT_DIR) -> safeAttachmentFile(staging, name)?.let { target ->
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                        attachments[name] = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target).toString()
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return envelope to attachments
    }

    /** Gson ignores Kotlin nullability, so check what a damaged or foreign file left out. */
    @Suppress("SENSELESS_COMPARISON")
    private fun validate(e: BackupEnvelope?): BackupEnvelope {
        if (e == null || e.app == null) throw IllegalArgumentException(context.getString(R.string.backup_error_not_libraryup))
        if (e.app != BackupEnvelope.APP_ID) throw IllegalArgumentException(context.getString(R.string.backup_error_other_app))
        if (e.schemaVersion > BackupEnvelope.CURRENT_SCHEMA_VERSION) {
            throw IllegalArgumentException(context.getString(R.string.backup_error_newer))
        }
        val lists = listOf(e.profile, e.shifts, e.seats, e.plans, e.students, e.memberships, e.payments,
            e.attendance, e.expenses, e.lockers, e.enquiries, e.activityLog)
        if (lists.any { it == null }) throw IllegalArgumentException(context.getString(R.string.backup_error_corrupt))
        return e
    }

    /** Refuses zip entries that try to escape the staging directory (`../../databases/...`). */
    private fun safeAttachmentFile(staging: File, entryName: String): File? {
        val relative = entryName.removePrefix(ATTACHMENT_DIR)
        if (relative.isBlank() || relative.contains("..") || relative.startsWith("/")) return null
        val target = File(staging, relative)
        return target.takeIf { it.canonicalPath.startsWith(staging.canonicalPath + File.separator) }
    }

    private fun resolve(stored: String?, attachments: Map<String, String>): String? {
        if (stored == null || !stored.startsWith(ATTACHMENT_MARKER)) return stored
        return attachments[stored.removePrefix(ATTACHMENT_MARKER)]
    }

    private fun extensionOf(uriOrPath: String, fallback: String): String {
        val name = uriOrPath.substringAfterLast('/').substringBefore('?')
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.length - 1) return fallback
        val ext = name.substring(dot)
        return if (ext.length <= 6 && ext.drop(1).all { it.isLetterOrDigit() }) ext else fallback
    }

    private fun readBounded(input: InputStream, limit: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n == -1) break
            total += n
            if (total > limit) throw IllegalStateException(context.getString(R.string.backup_error_too_large))
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    // ------------------------------------------------------------------ CSV

    /** Writes a CSV document (UTF-8 with BOM so Excel shows Hindi correctly). */
    suspend fun writeCsv(uri: Uri, header: List<String>, rows: List<List<String>>): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val out = context.contentResolver.openOutputStream(uri, "wt")
                    ?: throw IllegalStateException(context.getString(R.string.backup_error_open_write))
                out.use { it.write(Csv.document(header, rows).toByteArray(Charsets.UTF_8)) }
                rows.size
            }
        }

    suspend fun readText(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException(context.getString(R.string.backup_error_open_read))
            String(input.use { readBounded(it, 10L * 1024 * 1024) }, Charsets.UTF_8)
        }
    }
}

private class LocalDateAdapter : TypeAdapter<LocalDate>() {
    override fun write(out: JsonWriter, value: LocalDate) {
        out.value(value.toString())
    }

    override fun read(reader: JsonReader): LocalDate {
        if (reader.peek() == JsonToken.NUMBER) return LocalDate.ofEpochDay(reader.nextLong())
        return LocalDate.parse(reader.nextString())
    }
}
