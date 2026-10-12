package ai.sach.libraryup.backup

import ai.sach.libraryup.data.backup.BackupEnvelopeKeys
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Backups written by minified release builds carry R8-renamed envelope keys. These pin
 * that such a file is mapped back onto the real field names, so it can still be restored.
 */
class BackupEnvelopeKeysTest {

    private val gson = Gson()

    private fun parse(json: String): JsonObject = gson.fromJson(json, JsonObject::class.java)

    @Test
    fun `an envelope with real field names is returned untouched`() {
        val root = parse("""{"schemaVersion":2,"appVersionName":"1.0","properties":[]}""")
        assertSame(root, BackupEnvelopeKeys.normalize(root))
    }

    @Test
    fun `renamed keys are recovered from the shape of each value`() {
        val root = parse(
            """{
              "a": 2,
              "b": "1.0",
              "c": "2026-09-19T08:15:30.123Z",
              "d": [{"id":"p1","name":"Sunrise","address":"12 MG Road","totalUnits":4}],
              "e": [{"id":1,"name":"Asha","isPaid":false,"mobileNumber":"98","dueDate":"5"}],
              "f": [{"id":1,"tenantId":1,"name":"Ravi","relationship":"Brother","isEmergencyContact":true}],
              "g": [{"id":1,"tenantId":1,"date":"2026-09-01","amount":"5000","collectedBy":"me"}],
              "h": [{"tenantId":1,"propertyId":"p1","isActive":true,"rentAmount":5000.0}],
              "i": [{"id":1,"tenantId":1,"documentType":"ID","documentPath":"rentup-backup:attachments/document_1.pdf"}]
            }"""
        )

        val normalized = BackupEnvelopeKeys.normalize(root)

        assertEquals(2, normalized.get("schemaVersion").asInt)
        assertEquals("1.0", normalized.get("appVersionName").asString)
        assertEquals("2026-09-19T08:15:30.123Z", normalized.get("exportedAt").asString)
        assertEquals("p1", normalized.getAsJsonArray("properties")[0].asJsonObject.get("id").asString)
        assertEquals("Asha", normalized.getAsJsonArray("tenants")[0].asJsonObject.get("name").asString)
        assertEquals("Ravi", normalized.getAsJsonArray("tenantFamilyInfo")[0].asJsonObject.get("name").asString)
        assertEquals("me", normalized.getAsJsonArray("paymentHistory")[0].asJsonObject.get("collectedBy").asString)
        assertEquals("p1", normalized.getAsJsonArray("tenantPropertyCrossRef")[0].asJsonObject.get("propertyId").asString)
        assertEquals("ID", normalized.getAsJsonArray("tenantDocuments")[0].asJsonObject.get("documentType").asString)
    }

    @Test
    fun `empty lists in a renamed envelope become empty lists, not missing ones`() {
        val root = parse(
            """{"x": 1, "y": "1.0", "z": "2026-09-19T08:15:30Z",
                "p": [{"id":"p1","name":"Sunrise","address":"12 MG Road"}],
                "q": [], "r": [], "s": [], "t": [], "u": []}"""
        )

        val normalized = BackupEnvelopeKeys.normalize(root)

        listOf("tenants", "tenantFamilyInfo", "paymentHistory", "tenantPropertyCrossRef", "tenantDocuments").forEach {
            assertTrue(it, normalized.getAsJsonArray(it).size() == 0)
        }
        assertEquals(1, normalized.getAsJsonArray("properties").size())
    }

    /** BackupManager maps JsonParseException to "This doesn't look like a RentUp backup file." */
    @Test
    fun `a file that is not a JSON object fails with JsonParseException`() {
        listOf("[1, 2, 3]", "\"just a string\"", "PK\u0003\u0004 binary junk {").forEach { input ->
            try {
                gson.fromJson(input, JsonObject::class.java)
                org.junit.Assert.fail("expected JsonParseException for $input")
            } catch (e: com.google.gson.JsonParseException) {
                // expected
            }
        }
    }

    @Test
    fun `something that is not a backup is left for the caller to reject`() {
        val root = parse("""{"hello":"world","items":[{"foo":1}]}""")
        assertSame(root, BackupEnvelopeKeys.normalize(root))
    }
}
