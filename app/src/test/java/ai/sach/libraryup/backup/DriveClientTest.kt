package ai.sach.libraryup.backup

import ai.sach.libraryup.data.backup.drive.DriveApiException
import ai.sach.libraryup.data.backup.drive.DriveAuthException
import ai.sach.libraryup.data.backup.drive.DriveClient
import ai.sach.libraryup.data.backup.drive.DriveFile
import ai.sach.libraryup.data.backup.drive.backupsBeyondRetention
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Pins the Drive REST requests the backup depends on against a local server: the query
 * strings, the two-step resumable upload, and how failures surface.
 */
class DriveClientTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var client: DriveClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = DriveClient(OkHttpClient(), server.url("/"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    @Test
    fun `findOrCreateFolder reuses an existing folder`() {
        server.enqueue(json("""{"files":[{"id":"folder-1","name":"LibraryUp Backups"}]}"""))

        assertEquals("folder-1", client.findOrCreateFolder("tok"))

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("Bearer tok", request.getHeader("Authorization"))
        val q = request.requestUrl!!.queryParameter("q")!!
        assertTrue(q, q.contains("name = 'LibraryUp Backups'"))
        assertTrue(q, q.contains("mimeType = 'application/vnd.google-apps.folder'"))
        assertTrue(q, q.contains("trashed = false"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `findOrCreateFolder creates the folder when none exists`() {
        server.enqueue(json("""{"files":[]}"""))
        server.enqueue(json("""{"id":"new-folder"}"""))

        assertEquals("new-folder", client.findOrCreateFolder("tok"))

        server.takeRequest()
        val create = server.takeRequest()
        assertEquals("POST", create.method)
        assertEquals("/drive/v3/files", create.requestUrl!!.encodedPath)
        val body = create.body.readUtf8()
        assertTrue(body, body.contains("\"name\":\"LibraryUp Backups\""))
        assertTrue(body, body.contains("\"mimeType\":\"application/vnd.google-apps.folder\""))
    }

    @Test
    fun `findFolder returns null when there is no folder`() {
        server.enqueue(json("""{}"""))
        assertNull(client.findFolder("tok"))
    }

    @Test
    fun `listBackups scopes the query to the folder, newest first, and parses string sizes`() {
        server.enqueue(
            json(
                """{"files":[
                  {"id":"b","name":"libraryup-backup-2","size":"2048","createdTime":"2026-09-28T10:00:00.000Z"},
                  {"id":"a","name":"libraryup-backup-1","createdTime":"2026-09-27T10:00:00.000Z"}
                ]}"""
            )
        )

        val backups = client.listBackups("tok", "folder'1")

        assertEquals(listOf("b", "a"), backups.map { it.id })
        assertEquals(2048L, backups[0].size)
        assertNull(backups[1].size)
        val url = server.takeRequest().requestUrl!!
        assertEquals("'folder\\'1' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false", url.queryParameter("q"))
        assertEquals("createdTime desc", url.queryParameter("orderBy"))
    }

    @Test
    fun `upload starts a resumable session then PUTs the file to it`() {
        val file = temp.newFile("backup.zip").apply { writeBytes(ByteArray(1234) { it.toByte() }) }
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Location", server.url("/upload-session?id=xyz").toString()))
        server.enqueue(json("""{"id":"file-9","name":"libraryup-backup-x.zip","size":"1234","createdTime":"2026-09-28T10:00:00.000Z"}"""))

        val uploaded = client.upload("tok", "folder-1", "libraryup-backup-x.zip", file)

        assertEquals(DriveFile("file-9", "libraryup-backup-x.zip", 1234L, "2026-09-28T10:00:00.000Z"), uploaded)
        assertEquals(emptyMap<String, String>(), uploaded.appProperties)

        val init = server.takeRequest()
        assertEquals("POST", init.method)
        assertEquals("/upload/drive/v3/files", init.requestUrl!!.encodedPath)
        assertEquals("resumable", init.requestUrl!!.queryParameter("uploadType"))
        assertEquals("1234", init.getHeader("X-Upload-Content-Length"))
        assertEquals("application/zip", init.getHeader("X-Upload-Content-Type"))
        val metadata = init.body.readUtf8()
        assertTrue(metadata, metadata.contains("\"parents\":[\"folder-1\"]"))
        assertTrue(metadata, metadata.contains("\"name\":\"libraryup-backup-x.zip\""))

        val put = server.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals("xyz", put.requestUrl!!.queryParameter("id"))
        assertEquals("Bearer tok", put.getHeader("Authorization"))
        assertEquals(1234L, put.bodySize)
    }

    @Test
    fun `download writes the file bytes`() {
        server.enqueue(MockResponse().setBody("zip-bytes"))
        val destination = temp.newFile("restore.zip")

        client.download("tok", "file/1", destination)

        assertEquals("zip-bytes", destination.readText())
        val url = server.takeRequest().requestUrl!!
        assertEquals("/drive/v3/files/file%2F1", url.encodedPath)
        assertEquals("media", url.queryParameter("alt"))
    }

    @Test
    fun `delete issues a DELETE for the file`() {
        server.enqueue(MockResponse().setResponseCode(204))
        client.delete("tok", "old")
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/drive/v3/files/old", request.requestUrl!!.encodedPath)
    }

    @Test
    fun `401 surfaces as DriveAuthException`() {
        server.enqueue(json("""{"error":{"code":401,"message":"Invalid Credentials"}}""", code = 401))
        try {
            client.findFolder("expired")
            fail("expected DriveAuthException")
        } catch (e: DriveAuthException) {
            // expected
        }
    }

    @Test
    fun `other failures carry Drive's own message`() {
        server.enqueue(json("""{"error":{"code":403,"message":"The user's Drive storage quota has been exceeded."}}""", code = 403))
        try {
            client.findFolder("tok")
            fail("expected DriveApiException")
        } catch (e: DriveApiException) {
            assertEquals(403, e.code)
            assertEquals("The user's Drive storage quota has been exceeded.", e.message)
        }
    }

    @Test
    fun `failures without a JSON body fall back to a generic message`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("<html>oops</html>"))
        try {
            client.findFolder("tok")
            fail("expected DriveApiException")
        } catch (e: DriveApiException) {
            assertEquals("Google Drive request failed (HTTP 500).", e.message)
        }
    }

    private fun backup(id: String, day: Int, install: String?, name: String = "libraryup-backup-$id.zip") =
        DriveFile(
            id, name, null, "2026-09-${"%02d".format(day)}T00:00:00.000Z",
            install?.let { mapOf(DriveClient.PROPERTY_INSTALL_ID to it) } ?: emptyMap()
        )

    @Test
    fun `retention keeps this install's newest backups and never touches other files`() {
        val files = listOf(
            backup("1", 21, "me"),
            backup("3", 23, "me"),
            backup("x", 1, "me", name = "notes.txt"),
            backup("2", 22, "me"),
            backup("4", 24, "me")
        )

        assertEquals(listOf("2", "1"), backupsBeyondRetention(files, keep = 2, installId = "me").map { it.id })
        assertEquals(emptyList<DriveFile>(), backupsBeyondRetention(files, keep = 10, installId = "me"))
    }

    @Test
    fun `retention never deletes another install's backups`() {
        // An old phone's full backups, then a fresh install backing up daily.
        val files = listOf(backup("old1", 1, "old-phone"), backup("old2", 2, "old-phone"), backup("untagged", 3, null)) +
            (10..16).map { backup("new$it", it, "new-phone") }

        val deleted = backupsBeyondRetention(files, keep = 5, installId = "new-phone").map { it.id }

        assertEquals(listOf("new11", "new10"), deleted)
    }

    @Test
    fun `upload tags the file with the given app properties and reads them back`() {
        val file = temp.newFile("backup.zip").apply { writeBytes(ByteArray(10)) }
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Location", server.url("/session").toString()))
        server.enqueue(json("""{"id":"f","name":"libraryup-backup-x.zip","appProperties":{"libraryupInstallId":"abc"}}"""))

        val uploaded = client.upload("tok", "folder", "libraryup-backup-x.zip", file, mapOf(DriveClient.PROPERTY_INSTALL_ID to "abc"))

        assertEquals("abc", uploaded.appProperties[DriveClient.PROPERTY_INSTALL_ID])
        val metadata = server.takeRequest().body.readUtf8()
        assertTrue(metadata, metadata.contains("\"appProperties\":{\"libraryupInstallId\":\"abc\"}"))
        assertEquals("PUT", server.takeRequest().method)
    }

    @Test
    fun `listed files carry their app properties`() {
        server.enqueue(json("""{"files":[{"id":"a","name":"libraryup-backup-a.zip","appProperties":{"libraryupInstallId":"xyz"}}]}"""))
        val listed = client.listBackups("tok", "folder")
        assertEquals("xyz", listed.single().appProperties[DriveClient.PROPERTY_INSTALL_ID])
        val fields = server.takeRequest().requestUrl!!.queryParameter("fields")!!
        assertTrue(fields, fields.contains("appProperties"))
    }
}
