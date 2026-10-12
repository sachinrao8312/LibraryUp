package ai.sach.libraryup.data.backup

import ai.sach.libraryup.BuildConfig
import ai.sach.libraryup.data.local.LibraryUpDatabase
import ai.sach.libraryup.data.local.entity.PaymentHistory
import ai.sach.libraryup.data.local.entity.Property
import ai.sach.libraryup.data.local.entity.Tenant
import ai.sach.libraryup.data.local.entity.TenantDocument
import ai.sach.libraryup.data.local.entity.TenantFamilyInfo
import ai.sach.libraryup.data.local.entity.TenantPropertyCrossRef
import ai.sach.libraryup.data.repository.TenantRepository
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** What a completed export or import actually moved, shown to the user afterwards. */
data class BackupSummary(
    val propertyCount: Int,
    val tenantCount: Int,
    val paymentCount: Int,
    /** Tenant photos and documents carried in the file. */
    val attachmentCount: Int = 0,
    /** Attachments whose bytes could not be read at export time, so were left out. */
    val attachmentsSkipped: Int = 0,
    /** Rows a restore had to drop because the tenant or property they name is missing. */
    val rowsSkipped: Int = 0,
    /** SHA-256 of an export's data and attachments, ignoring its timestamp: same data, same hash. */
    val contentHash: String = ""
)

/**
 * Exports the whole local database to a single file the user picks a location for, and
 * restores one back. This is the feature the Backup & Restore screen has promised since
 * before it did anything - see [BackupEnvelope] for what "everything" covers and what it
 * deliberately leaves out.
 *
 * The file is a zip holding two things:
 *
 * ```
 * rentup-backup-2026-09-19.zip
 *   backup.json      the BackupEnvelope - every row of every table
 *   attachments/     tenant photos and uploaded documents, byte for byte
 * ```
 *
 * The attachments are the reason this is a zip rather than the bare JSON document it
 * started as. A tenant's `avatarUri` is a `content://` URI into this app's own sandbox;
 * writing that string into a backup and restoring it on a new phone yields a row that
 * points at nothing, so the photo is gone with no indication it ever existed. Copying the
 * bytes in and rewriting the URI on the way back out is what makes a restored tenant look
 * like the tenant that was backed up. Rows still carry a path they can resolve, so the
 * advice that URIs should not be backed up verbatim is respected - the URI in the file is
 * relative to the file.
 *
 * Deliberately not a merge: [importFrom] fully replaces local data inside one transaction.
 * Reconciling id collisions across a relational schema (a tenant id from the backup may
 * already exist locally, pointing at someone else) is real complexity a landlord's backup
 * tool doesn't need - "restore" should mean what it says, behind a confirmation the caller
 * is responsible for showing before this is ever invoked.
 */
class BackupManager(
    private val context: Context,
    private val database: LibraryUpDatabase,
    private val tenantRepository: TenantRepository
) {
    companion object {
        private const val TAG = "BackupManager"
        const val MIME_TYPE_ZIP = "application/zip"
        const val MIME_TYPE_CSV = "text/csv"

        /** The envelope, as a single entry inside the backup zip. */
        internal const val MANIFEST_ENTRY = "backup.json"

        /** Directory inside the zip holding avatars and tenant documents. */
        internal const val ATTACHMENT_DIR = "attachments/"

        /**
         * Written in place of a `content://` URI whose bytes were copied into the zip.
         * On restore the file is unpacked and the row gets a fresh, valid URI.
         */
        internal const val ATTACHMENT_MARKER = "rentup-backup:"

        /** Where unpacked attachments live inside the app's private storage. */
        private const val RESTORED_FILES_DIR = "restored_attachments"

        /** Guards against a zip bomb when reading a file we did not write. */
        private const val MAX_MANIFEST_BYTES = 64L * 1024 * 1024

        private const val COPY_BUFFER_BYTES = 8 * 1024

        /** Default file name offered in the "Save as" picker for a backup. */
        fun suggestedFileName(): String {
            val date = Instant.now().toString().substringBefore('T')
            return "rentup-backup-$date.zip"
        }

        /** Default file name offered in the "Save as" picker for a CSV tenant list. */
        fun suggestedCsvFileName(): String {
            val date = Instant.now().toString().substringBefore('T')
            return "rentup-tenants-$date.csv"
        }

        private val CSV_HEADER = listOf(
            "Tenant Name", "Mobile Number", "Property", "Property Address", "Floor",
            "Amount", "Due Date", "Paid", "Permanent Address"
        )

        /**
         * Quotes a CSV field per RFC 4180 whenever it contains a comma, quote, or newline -
         * addresses and names in this app can plausibly contain any of the three.
         */
        private fun csvField(value: String): String {
            val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
            return if (needsQuoting) "\"${value.replace("\"", "\"\"")}\"" else value
        }
    }

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Export and import each touch the whole database and the restored-attachments
     * directory. The scheduled Google Drive backup calls in from a background worker, so
     * without this an export could copy files a concurrent restore is deleting.
     */
    private val lock = Mutex()

    private data class Snapshot(
        val properties: List<Property>,
        val tenants: List<Tenant>,
        val familyInfo: List<TenantFamilyInfo>,
        val payments: List<PaymentHistory>,
        val crossRefs: List<TenantPropertyCrossRef>,
        val documents: List<TenantDocument>
    )

    // ------------------------------------------------------------------ export

    suspend fun exportTo(uri: Uri): Result<BackupSummary> = lock.withLock { exportLocked(uri) }

    private suspend fun exportLocked(uri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        try {
            // One read transaction: a backup taken while the landlord is editing must not
            // pair tenants from before the edit with payments from after it.
            val (properties, tenants, familyInfo, payments, crossRefs, documents) = database.withTransaction {
                Snapshot(
                    properties = database.propertyDao().getAllPropertiesSync(),
                    tenants = database.tenantDao().getAllTenantsSync(),
                    familyInfo = database.tenantFamilyInfoDao().getAllSync(),
                    payments = database.paymentHistoryDao().getAllSync(),
                    crossRefs = database.tenantPropertyDao().getAllSync(),
                    documents = database.tenantDocumentDao().getAllSync()
                )
            }

            val demoPropertyIds = properties.filter { it.id.startsWith("mock-property-") }
                .mapTo(mutableSetOf()) { it.id }
            val demoTenantIds = tenants
                .filter { tenant -> tenant.propertyId.split(",").any { it in demoPropertyIds } }
                .mapTo(mutableSetOf()) { it.id }
            val exportProperties = properties.filter { it.id !in demoPropertyIds }
            val exportTenants = tenants.filter { it.id !in demoTenantIds }
            val exportFamilyInfo = familyInfo.filter { it.tenantId !in demoTenantIds }
            val exportPayments = payments.filter { it.tenantId !in demoTenantIds }
            val exportCrossRefs = crossRefs.filter { it.propertyId !in demoPropertyIds && it.tenantId !in demoTenantIds }
            val exportDocuments = documents.filter { it.tenantId !in demoTenantIds }

            val stream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Couldn't open the chosen file for writing."))

            var included = 0
            var skipped = 0
            val digest = MessageDigest.getInstance("SHA-256")

            stream.use { out ->
                ZipOutputStream(out.buffered()).use { zip ->
                    // Attachments are written first so the envelope can record only the
                    // entry names whose copies actually succeeded.
                    val exportedTenants = exportTenants.map { tenant ->
                        val source = tenant.avatarUri
                        if (source.isNullOrBlank()) {
                            tenant
                        } else {
                            val entry = ATTACHMENT_DIR + "avatar_${tenant.id}${extensionOf(source, ".jpg")}"
                            if (copyIntoZip(zip, source, entry, digest)) {
                                included++
                                tenant.copy(avatarUri = ATTACHMENT_MARKER + entry)
                            } else {
                                // The URI is dropped rather than carried over: it names a
                                // sandbox path that will not resolve anywhere else, and a
                                // blank is honest about the photo being gone.
                                skipped++
                                tenant.copy(avatarUri = null)
                            }
                        }
                    }

                    val exportedDocuments = exportDocuments.map { document ->
                        val source = document.documentPath
                        if (source.isBlank()) {
                            document
                        } else {
                            val entry = ATTACHMENT_DIR + "document_${document.id}${extensionOf(source, "")}"
                            if (copyIntoZip(zip, source, entry, digest)) {
                                included++
                                document.copy(documentPath = ATTACHMENT_MARKER + entry)
                            } else {
                                skipped++
                                document.copy(documentPath = "")
                            }
                        }
                    }

                    val envelope = BackupEnvelope(
                        appVersionName = BuildConfig.VERSION_NAME,
                        exportedAt = Instant.now().toString(),
                        properties = exportProperties,
                        tenants = exportedTenants,
                        tenantFamilyInfo = exportFamilyInfo,
                        paymentHistory = exportPayments,
                        tenantPropertyCrossRef = exportCrossRefs,
                        tenantDocuments = exportedDocuments
                    )

                    digest.update(gson.toJson(envelope.copy(exportedAt = "")).toByteArray(Charsets.UTF_8))

                    zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                    // The writer must not close the zip stream before closeEntry runs.
                    OutputStreamWriter(NonClosingOutputStream(zip), Charsets.UTF_8).use { writer ->
                        gson.toJson(envelope, writer)
                    }
                    zip.closeEntry()
                }
            }

            Log.d(TAG, "Exported ${exportTenants.size} tenants, ${exportProperties.size} properties, $included attachments")
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            Result.success(BackupSummary(exportProperties.size, exportTenants.size, exportPayments.size, included, skipped, contentHash = hash))
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            Result.failure(e)
        }
    }

    /**
     * Writes the tenant roster as a flat CSV - one row per tenant, readable in Excel or
     * Sheets. This is deliberately not a backup: the schema is relational (a tenant has
     * many payments, many family members, belongs to properties through a join table) and
     * flattening that into one sheet without repeating or losing data isn't possible, so
     * there is no matching import - CSV here answers "give me a spreadsheet of my
     * tenants", not "restore my data". [exportTo] / [importFrom] are the actual backup.
     *
     * @return the number of tenant rows written.
     */
    suspend fun exportTenantsCsv(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val tenants = database.tenantDao().getAllTenantsSync()
            val properties = database.propertyDao().getAllPropertiesSync().associateBy { it.id }
            val demoPropertyIds = properties.keys.filter { it.startsWith("mock-property-") }.toSet()
            val exportTenants = tenants.filter { tenant ->
                tenant.propertyId.split(",").none { it in demoPropertyIds }
            }

            val stream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Couldn't open the chosen file for writing."))

            stream.use { out ->
                OutputStreamWriter(out).use { writer ->
                    writer.write(CSV_HEADER.joinToString(",") { csvField(it) })
                    writer.write("\r\n")
                    exportTenants.forEach { tenant ->
                        val property = properties[tenant.propertyId]
                        val row = listOf(
                            tenant.name,
                            tenant.mobileNumber,
                            property?.name.orEmpty(),
                            property?.address.orEmpty(),
                            tenant.floor,
                            tenant.amount.orEmpty(),
                            tenant.dueDate,
                            if (tenant.isPaid) "Paid" else "Unpaid",
                            tenant.permanentAddress
                        )
                        writer.write(row.joinToString(",") { csvField(it) })
                        writer.write("\r\n")
                    }
                }
            }

            Log.d(TAG, "Exported ${exportTenants.size} tenants to CSV")
            Result.success(exportTenants.size)
        } catch (e: Exception) {
            Log.e(TAG, "CSV export failed", e)
            Result.failure(e)
        }
    }

    /** @return true when the bytes behind [uriOrPath] were copied into the zip. */
    private fun copyIntoZip(zip: ZipOutputStream, uriOrPath: String, entryName: String, digest: MessageDigest): Boolean {
        var entryOpen = false
        return try {
            val input = openAttachment(uriOrPath) ?: return false
            DigestInputStream(input, digest).use {
                digest.update(entryName.toByteArray(Charsets.UTF_8))
                zip.putNextEntry(ZipEntry(entryName))
                entryOpen = true
                it.copyTo(zip, COPY_BUFFER_BYTES)
                zip.closeEntry()
                entryOpen = false
            }
            true
        } catch (e: Exception) {
            // A document picked long ago may sit behind a permission that has since
            // lapsed. That is a missing attachment, not a failed backup.
            Log.w(TAG, "Skipping unreadable attachment $uriOrPath: ${e.message}")
            if (entryOpen) {
                // Leaves a truncated entry that no envelope row points at.
                runCatching { zip.closeEntry() }
            }
            false
        }
    }

    private fun openAttachment(uriOrPath: String): InputStream? {
        return when {
            uriOrPath.isBlank() -> null
            uriOrPath.startsWith(ATTACHMENT_MARKER) -> null
            uriOrPath.startsWith("content://") -> context.contentResolver.openInputStream(Uri.parse(uriOrPath))
            uriOrPath.startsWith("file://") -> {
                val path = Uri.parse(uriOrPath).path ?: return null
                File(path).takeIf { it.isFile }?.inputStream()
            }
            else -> File(uriOrPath).takeIf { it.isFile }?.inputStream()
        }
    }

    // ------------------------------------------------------------------ import

    suspend fun importFrom(uri: Uri): Result<BackupSummary> = lock.withLock { importLocked(uri) }

    private suspend fun importLocked(uri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        val staging = File(context.filesDir, "$RESTORED_FILES_DIR/staging-${System.currentTimeMillis()}")
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Couldn't open the chosen file for reading."))

            val archive = stream.use { input -> readArchive(input, staging) }
            val envelope = archive.envelope.validOrThrow()

            val demoPropertyIds = envelope.properties
                .filter { it.id.startsWith("mock-property-") }
                .mapTo(mutableSetOf()) { it.id }
            val properties = envelope.properties.filter { it.id !in demoPropertyIds }
            val tenants = envelope.tenants
                .filter { tenant -> tenant.propertyId.split(",").none { it in demoPropertyIds } }
                .map { tenant ->
                tenant.copy(avatarUri = resolveAttachment(tenant.avatarUri, archive.attachments))
            }
            val documents = envelope.tenantDocuments.map { document ->
                document.copy(documentPath = resolveAttachment(document.documentPath, archive.attachments).orEmpty())
            }

            // A child row naming a tenant or property the file does not contain would
            // abort the transaction on a foreign key violation, and the screen would show
            // the user SQLite's "FOREIGN KEY constraint failed (code 787)". A backup this
            // app wrote is self-consistent, so this only bites on a hand-edited or
            // partially written file - where losing the odd stray row beats losing the
            // whole restore. The count is reported rather than swallowed.
            val tenantIds = tenants.mapTo(mutableSetOf()) { it.id }
            val propertyIds = properties.mapTo(mutableSetOf()) { it.id }

            val familyInfo = envelope.tenantFamilyInfo.filter { it.tenantId in tenantIds }
            val payments = envelope.paymentHistory.filter { it.tenantId in tenantIds }
            val ownedDocuments = documents.filter { it.tenantId in tenantIds }
            val crossRefs = envelope.tenantPropertyCrossRef
                .filter { it.tenantId in tenantIds && it.propertyId in propertyIds }

            val orphans = (envelope.tenantFamilyInfo.size - familyInfo.size) +
                (envelope.paymentHistory.size - payments.size) +
                (documents.size - ownedDocuments.size) +
                (envelope.tenantPropertyCrossRef.size - crossRefs.size)
            if (orphans > 0) Log.w(TAG, "Skipped $orphans row(s) referring to missing tenants or properties")

            database.withTransaction {
                // Deleting every tenant and property cascades their family info, documents,
                // payments and cross-references - see the FK definitions on those entities.
                database.tenantDao().deleteAll()
                database.propertyDao().deleteAllProperties()

                database.propertyDao().insertAllProperties(properties)
                database.tenantDao().insertAll(tenants)
                database.tenantFamilyInfoDao().insertAll(familyInfo)
                database.paymentHistoryDao().insertAll(payments)
                database.tenantDocumentDao().insertAll(ownedDocuments)
                if (crossRefs.isNotEmpty()) {
                    database.tenantPropertyDao().insertTenantPropertyRelations(crossRefs)
                }
            }

            // The restored rows reference files under the staging directory, so it only
            // becomes the live attachment directory once the transaction has committed.
            promoteStagingDirectory(staging)

            // TenantRepository serves the tenant list from a 5-minute in-memory cache, and
            // this restore wrote straight to the DAOs underneath it. Without clearing that
            // cache the home screen would keep showing the pre-restore tenants for up to
            // five minutes after a "Restored N tenants" confirmation - every other write
            // path in the app pairs its changes with clearCache() for exactly this reason.
            tenantRepository.clearCache()

            Log.d(TAG, "Imported ${tenants.size} tenants, ${properties.size} properties")
            Result.success(
                BackupSummary(
                    propertyCount = properties.size,
                    tenantCount = tenants.size,
                    paymentCount = payments.size,
                    attachmentCount = archive.attachments.size,
                    rowsSkipped = orphans
                )
            )
        } catch (e: JsonParseException) {
            // The screen shows error.message straight to the user, and Gson's is
            // "Expected BEGIN_OBJECT but was STRING at line 1 column 1 path $" plus a
            // link to its troubleshooting guide - true, and useless to a landlord who
            // picked the wrong file.
            staging.deleteRecursively()
            Log.e(TAG, "Import failed: unparseable backup", e)
            Result.failure(IllegalArgumentException("This doesn't look like a RentUp backup file."))
        } catch (e: Exception) {
            staging.deleteRecursively()
            Log.e(TAG, "Import failed", e)
            Result.failure(e)
        }
    }

    /** Everything the file yielded: the envelope, and where each attachment landed. */
    private data class ArchiveContents(
        val envelope: BackupEnvelope?,
        /** Zip entry name -> content URI of the unpacked file. */
        val attachments: Map<String, String>
    )

    /**
     * Reads either a zip backup or a bare JSON one.
     *
     * Backups written before attachments were carried are a single JSON document with no
     * zip container. They are still perfectly good backups of the rows, so they are read
     * rather than rejected; they simply have no attachments to unpack.
     */
    private fun readArchive(input: InputStream, staging: File): ArchiveContents {
        val buffered = BufferedInputStream(input)
        if (!buffered.looksLikeZip()) {
            val envelope = InputStreamReader(buffered, Charsets.UTF_8).use { reader ->
                parseEnvelope(gson.fromJson(reader, JsonObject::class.java))
            }
            return ArchiveContents(envelope, emptyMap())
        }

        var envelope: BackupEnvelope? = null
        val attachments = mutableMapOf<String, String>()

        ZipInputStream(buffered).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    entry.isDirectory -> Unit

                    name == MANIFEST_ENTRY ->
                        envelope = parseEnvelope(gson.fromJson(zip.readBoundedText(MAX_MANIFEST_BYTES), JsonObject::class.java))

                    name.startsWith(ATTACHMENT_DIR) -> {
                        val target = safeAttachmentFile(staging, name)
                        if (target == null) {
                            Log.w(TAG, "Ignoring attachment with suspicious path: $name")
                        } else {
                            target.parentFile?.mkdirs()
                            target.outputStream().use { zip.copyTo(it, COPY_BUFFER_BYTES) }
                            attachments[name] = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                target
                            ).toString()
                        }
                    }

                    else -> Log.d(TAG, "Ignoring unknown entry: $name")
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return ArchiveContents(envelope, attachments)
    }

    /** Also reads backups whose field names a minified release build renamed - see [BackupEnvelopeKeys]. */
    private fun parseEnvelope(json: JsonObject?): BackupEnvelope? =
        json?.let { gson.fromJson(BackupEnvelopeKeys.normalize(it), BackupEnvelope::class.java) }

    /**
     * Resolves a zip entry name to a real file under [staging], or null when the name tries
     * to escape it. A backup file can come from anywhere, and an entry called
     * `attachments/../../databases/rentup_database` would otherwise be unpacked straight
     * over the live database.
     */
    private fun safeAttachmentFile(staging: File, entryName: String): File? {
        val relative = entryName.removePrefix(ATTACHMENT_DIR)
        if (relative.isBlank() || relative.contains("..") || relative.startsWith("/")) return null
        val target = File(staging, relative)
        val stagingPath = staging.canonicalPath + File.separator
        return target.takeIf { it.canonicalPath.startsWith(stagingPath) }
    }

    /**
     * Turns a `rentup-backup:` marker into the URI of the file unpacked for it.
     *
     * Anything else is returned as-is, which is how a v1 backup's already-stripped empty
     * path survives untouched. Bytes missing from the zip mean the row keeps its data and
     * loses only its picture.
     */
    private fun resolveAttachment(stored: String?, attachments: Map<String, String>): String? {
        if (stored == null || !stored.startsWith(ATTACHMENT_MARKER)) return stored
        return attachments[stored.removePrefix(ATTACHMENT_MARKER)]
    }

    /**
     * Makes the staging directory the one and only attachment directory.
     *
     * A restore replaces the database wholesale, so every file left over from an earlier
     * restore is now unreferenced. Clearing them here stops private storage growing by a
     * full copy of the attachments on each restore.
     */
    private fun promoteStagingDirectory(staging: File) {
        File(context.filesDir, RESTORED_FILES_DIR).listFiles()
            ?.filter { it.absolutePath != staging.absolutePath }
            ?.forEach { it.deleteRecursively() }
    }

    private fun extensionOf(uriOrPath: String, fallback: String): String {
        val name = uriOrPath.substringAfterLast('/').substringBefore('?')
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.length - 1) return fallback
        val extension = name.substring(dot)
        // Anything longer is not an extension, it is the rest of a URI.
        return if (extension.length <= 6 && extension.drop(1).all { it.isLetterOrDigit() }) extension else fallback
    }

    /**
     * Gson does not enforce Kotlin's non-null types at runtime - a truncated or hand-edited
     * file can deserialize with a field silently left null, which would otherwise surface
     * as an NPE deep inside the restore transaction rather than a message the user can act on.
     */
    private fun BackupEnvelope?.validOrThrow(): BackupEnvelope {
        val envelope = this
            ?: throw IllegalArgumentException("This doesn't look like a RentUp backup file.")

        if (envelope.schemaVersion > BackupEnvelope.CURRENT_SCHEMA_VERSION) {
            throw IllegalArgumentException(
                "This backup (version ${envelope.schemaVersion}) was made by a newer version of RentUp."
            )
        }
        if (envelope.schemaVersion < 1) {
            throw IllegalArgumentException(
                "This backup (version ${envelope.schemaVersion}) isn't compatible with this version of RentUp."
            )
        }
        if (envelope.properties == null || envelope.tenants == null || envelope.tenantFamilyInfo == null ||
            envelope.paymentHistory == null || envelope.tenantPropertyCrossRef == null || envelope.tenantDocuments == null
        ) {
            throw IllegalArgumentException("This backup file looks incomplete or corrupted.")
        }
        return envelope
    }
}

/** True when the stream starts with the local file header every zip begins with. */
private fun BufferedInputStream.looksLikeZip(): Boolean {
    mark(4)
    val header = ByteArray(4)
    var read = 0
    while (read < 4) {
        val n = read(header, read, 4 - read)
        if (n == -1) break
        read += n
    }
    reset()
    return read == 4 &&
        header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
        header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
}

/**
 * Reads the current zip entry as UTF-8 text, refusing anything implausibly large.
 *
 * The bytes are collected before decoding: a tenant name in Hindi or Marathi is multi-byte,
 * and decoding each 8 KB chunk on its own would corrupt any character that happened to
 * straddle a chunk boundary.
 */
private fun ZipInputStream.readBoundedText(limit: Long): String {
    val collected = ByteArrayOutputStream()
    val chunk = ByteArray(8 * 1024)
    var total = 0L
    while (true) {
        val read = read(chunk)
        if (read == -1) break
        total += read
        if (total > limit) throw IllegalStateException("This backup file is too large to read.")
        collected.write(chunk, 0, read)
    }
    return collected.toString(Charsets.UTF_8.name())
}

/**
 * Lets a Writer be closed (flushing its buffer) without closing the zip underneath it,
 * so the entry can still be closed properly afterwards.
 */
private class NonClosingOutputStream(
    private val delegate: java.io.OutputStream
) : java.io.OutputStream() {
    override fun write(b: Int) = delegate.write(b)
    override fun write(b: ByteArray, off: Int, len: Int) = delegate.write(b, off, len)
    override fun flush() = delegate.flush()
    override fun close() = flush()
}
