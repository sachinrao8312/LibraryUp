package ai.sach.libraryup.data.backup.drive

import com.google.gson.Gson
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException

/** The access token was rejected - expired or revoked. */
class DriveAuthException(message: String) : IOException(message)

class DriveApiException(val code: Int, message: String) : IOException(message)

data class DriveFile(
    val id: String,
    val name: String,
    val size: Long?,
    /** RFC 3339, always UTC - so it also sorts correctly as a plain string. */
    val createdTime: String?,
    val appProperties: Map<String, String> = emptyMap()
)

/**
 * The handful of Google Drive v3 REST calls RentUp's backup needs, over plain OkHttp.
 *
 * Deliberately not the Google API client library: that pulls in a large dependency tree
 * for five HTTP calls. Every call takes an OAuth access token for the `drive.file` scope,
 * which only ever exposes files and folders this app created - never the rest of the
 * user's Drive.
 *
 * Blocking; call off the main thread.
 */
class DriveClient(
    private val http: OkHttpClient,
    private val baseUrl: HttpUrl = "https://www.googleapis.com/".toHttpUrl()
) {
    companion object {
        const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
        const val FOLDER_NAME = "RentUp Backups"
        const val BACKUP_NAME_PREFIX = "rentup-backup-"

        /** Drive `appProperties` key naming the app install that uploaded a backup. */
        const val PROPERTY_INSTALL_ID = "rentupInstallId"

        private const val FOLDER_MIME = "application/vnd.google-apps.folder"
        private const val ZIP_MIME = "application/zip"
        private const val FILE_FIELDS = "id,name,size,createdTime,appProperties"
        private val JSON = "application/json; charset=UTF-8".toMediaType()

        /** Quotes a value for use inside a Drive `q` string literal. */
        internal fun escapeQueryValue(value: String): String =
            value.replace("\\", "\\\\").replace("'", "\\'")
    }

    private val gson = Gson()

    fun findFolder(token: String): String? {
        val query = "name = '${escapeQueryValue(FOLDER_NAME)}' and mimeType = '$FOLDER_MIME' and trashed = false"
        // Oldest first, so if two devices ever raced to create the folder, both settle on
        // the same one.
        return listFiles(token, query, orderBy = "createdTime").firstOrNull()?.id
    }

    fun findOrCreateFolder(token: String): String {
        findFolder(token)?.let { return it }
        val url = filesUrl().addQueryParameter("fields", "id").build()
        val body = gson.toJson(mapOf("name" to FOLDER_NAME, "mimeType" to FOLDER_MIME)).toRequestBody(JSON)
        return call(token, Request.Builder().url(url).post(body)) { response ->
            gson.fromJson(response.body!!.charStream(), ApiFile::class.java).toDriveFile().id
        }
    }

    /** Newest first. */
    fun listBackups(token: String, folderId: String): List<DriveFile> {
        val query = "'${escapeQueryValue(folderId)}' in parents and mimeType != '$FOLDER_MIME' and trashed = false"
        return listFiles(token, query, orderBy = "createdTime desc")
    }

    /**
     * Resumable upload - a single PUT of the whole file, but unlike a multipart upload it
     * has no 5 MB guidance ceiling, and a backup carrying tenant documents can exceed that.
     */
    fun upload(
        token: String,
        folderId: String,
        name: String,
        file: File,
        appProperties: Map<String, String> = emptyMap()
    ): DriveFile {
        val initUrl = baseUrl.newBuilder()
            .addPathSegments("upload/drive/v3/files")
            .addQueryParameter("uploadType", "resumable")
            .addQueryParameter("fields", FILE_FIELDS)
            .build()
        val metadata = gson.toJson(
            mapOf(
                "name" to name,
                "parents" to listOf(folderId),
                "mimeType" to ZIP_MIME,
                "appProperties" to appProperties
            )
        ).toRequestBody(JSON)
        val init = Request.Builder().url(initUrl).post(metadata)
            .header("X-Upload-Content-Type", ZIP_MIME)
            .header("X-Upload-Content-Length", file.length().toString())

        val sessionUrl = call(token, init) { response ->
            response.header("Location")
                ?: throw DriveApiException(response.code, "Google Drive didn't start the upload.")
        }

        val put = Request.Builder().url(sessionUrl).put(file.asRequestBody(ZIP_MIME.toMediaType()))
        return call(token, put) { response ->
            gson.fromJson(response.body!!.charStream(), ApiFile::class.java).toDriveFile()
        }
    }

    fun download(token: String, fileId: String, destination: File) {
        val url = filesUrl().addPathSegment(fileId).addQueryParameter("alt", "media").build()
        call(token, Request.Builder().url(url).get()) { response ->
            response.body!!.byteStream().use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    fun delete(token: String, fileId: String) {
        val url = filesUrl().addPathSegment(fileId).build()
        call(token, Request.Builder().url(url).delete()) { }
    }

    private fun listFiles(token: String, query: String, orderBy: String): List<DriveFile> {
        val url = filesUrl()
            .addQueryParameter("q", query)
            .addQueryParameter("spaces", "drive")
            .addQueryParameter("orderBy", orderBy)
            .addQueryParameter("pageSize", "100")
            .addQueryParameter("fields", "files($FILE_FIELDS)")
            .build()
        return call(token, Request.Builder().url(url).get()) { response ->
            gson.fromJson(response.body!!.charStream(), ApiFileList::class.java)
                ?.files.orEmpty()
                .map { it.toDriveFile() }
        }
    }

    private fun filesUrl(): HttpUrl.Builder = baseUrl.newBuilder().addPathSegments("drive/v3/files")

    private fun <T> call(token: String, builder: Request.Builder, handle: (Response) -> T): T {
        val request = builder.header("Authorization", "Bearer $token").build()
        http.newCall(request).execute().use { response ->
            if (response.code == 401) {
                throw DriveAuthException("Google Drive access has expired or was removed.")
            }
            if (!response.isSuccessful) {
                throw DriveApiException(response.code, errorMessage(response))
            }
            return handle(response)
        }
    }

    /** Drive's own message where it gives one - e.g. "The user's Drive storage quota has been exceeded." */
    private fun errorMessage(response: Response): String {
        val fallback = "Google Drive request failed (HTTP ${response.code})."
        val body = runCatching { response.body?.string() }.getOrNull() ?: return fallback
        return runCatching { gson.fromJson(body, ApiErrorResponse::class.java)?.error?.message }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: fallback
    }

    private class ApiErrorResponse(val error: ApiError?)

    private class ApiError(val message: String?)

    private class ApiFileList(val files: List<ApiFile>?)

    private class ApiFile(
        val id: String?,
        val name: String?,
        // Drive sends int64 values as JSON strings.
        val size: String?,
        val createdTime: String?,
        val appProperties: Map<String, String>?
    ) {
        fun toDriveFile() = DriveFile(
            id = id ?: throw IOException("Google Drive returned a file without an id."),
            name = name.orEmpty(),
            size = size?.toLongOrNull(),
            createdTime = createdTime,
            appProperties = appProperties.orEmpty()
        )
    }
}

/**
 * The backups to delete so only the newest [keep] made by this install remain.
 *
 * Only this install's own backups are ever candidates. Another phone on the same Google
 * account - or this phone before a reinstall - must never have its backups pushed out:
 * a fresh install with one test tenant backing up daily would otherwise delete every
 * real backup within a week. Files that don't look like RentUp backups are never touched.
 */
fun backupsBeyondRetention(backups: List<DriveFile>, keep: Int, installId: String): List<DriveFile> =
    backups
        .filter { it.name.startsWith(DriveClient.BACKUP_NAME_PREFIX) }
        .filter { it.appProperties[DriveClient.PROPERTY_INSTALL_ID] == installId }
        .sortedByDescending { it.createdTime.orEmpty() }
        .drop(keep)
