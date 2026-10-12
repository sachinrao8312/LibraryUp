package ai.sach.rentup.backup

import ai.sach.rentup.data.backup.BackupEnvelope
import ai.sach.rentup.data.local.entity.EmergencyContact
import ai.sach.rentup.data.local.entity.PaymentHistory
import ai.sach.rentup.data.local.entity.Property
import ai.sach.rentup.data.local.entity.Tenant
import ai.sach.rentup.data.local.entity.TenantDocument
import ai.sach.rentup.data.local.entity.TenantFamilyInfo
import ai.sach.rentup.data.local.entity.TenantPropertyCrossRef
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * The backup envelope - the `backup.json` entry inside the backup zip. [BackupManager]
 * itself needs a live Context/Uri/Room database and isn't exercised here; this pins the
 * things that would otherwise fail silently or far from the cause: the envelope
 * round-trips every foreign key intact, attachment paths stay relative to the file rather
 * than to a device, and Gson's known blind spot around Kotlin non-null types is real for
 * this exact class, not theoretical.
 */
class BackupEnvelopeTest {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    private fun sampleEnvelope() = BackupEnvelope(
        appVersionName = "1.0",
        exportedAt = "2026-09-14T10:00:00Z",
        properties = listOf(Property(id = "prop-1", name = "Sunrise Apts", address = "1 Main St", totalUnits = 4, floors = 2)),
        tenants = listOf(
            Tenant(
                id = 42, name = "Asha Rao", propertyId = "prop-1", dueDate = "2026-10-01", amount = "15000",
                avatarUri = "rentup-backup:attachments/avatar_42.jpg",
                emergencyContacts = listOf(EmergencyContact("Ravi Rao", "9990001111", "Brother", "2 Park Rd"))
            )
        ),
        tenantFamilyInfo = listOf(TenantFamilyInfo(id = 1, tenantId = 42, name = "Kid Rao", relationship = "Child", age = 8)),
        paymentHistory = listOf(PaymentHistory(id = 1, tenantId = 42, date = "2026-09-01", amount = "15000", collectedBy = "Owner", remark = null)),
        tenantPropertyCrossRef = listOf(TenantPropertyCrossRef(tenantId = 42, propertyId = "prop-1", floor = "2")),
        tenantDocuments = listOf(
            TenantDocument(
                id = 1, tenantId = 42, documentType = "ID Proof", uploadDate = "2026-08-01",
                documentPath = "rentup-backup:attachments/document_1.pdf"
            )
        )
    )

    @Test
    fun `round trip preserves every foreign key across the schema`() {
        val restored = gson.fromJson(gson.toJson(sampleEnvelope()), BackupEnvelope::class.java)

        val tenantId = restored.tenants.first().id
        assertEquals(42L, tenantId)
        assertEquals(tenantId, restored.tenantFamilyInfo.first().tenantId)
        assertEquals(tenantId, restored.paymentHistory.first().tenantId)
        assertEquals(tenantId, restored.tenantPropertyCrossRef.first().tenantId)
        assertEquals(restored.properties.first().id, restored.tenantPropertyCrossRef.first().propertyId)
        assertEquals("Ravi Rao", restored.tenants.first().emergencyContacts.first().name)
    }

    @Test
    fun `attachment markers survive a round trip instead of device URIs`() {
        val restored = gson.fromJson(gson.toJson(sampleEnvelope()), BackupEnvelope::class.java)

        // What goes in the file is a name relative to the file, never a content:// URI
        // into a sandbox the reading device does not have.
        assertEquals("rentup-backup:attachments/document_1.pdf", restored.tenantDocuments.first().documentPath)
        assertEquals("rentup-backup:attachments/avatar_42.jpg", restored.tenants.first().avatarUri)
    }

    @Test
    fun `a blank document path from a version 1 backup round trips untouched`() {
        // Version 1 wrote "" for every document because it had nowhere to put the bytes.
        // Those files still restore; they simply have no attachment to resolve.
        val legacy = sampleEnvelope().copy(
            schemaVersion = 1,
            tenantDocuments = listOf(
                TenantDocument(id = 1, tenantId = 42, documentType = "ID Proof", uploadDate = "2026-08-01", documentPath = "")
            )
        )
        val restored = gson.fromJson(gson.toJson(legacy), BackupEnvelope::class.java)

        assertEquals("", restored.tenantDocuments.first().documentPath)
        assertEquals(1, restored.schemaVersion)
    }

    @Test
    fun `a backup whose envelope keys R8 renamed restores the same as the original`() {
        // Minified release builds wrote the envelope with renamed keys; the entity rows
        // inside were never renamed. Rename the top-level keys the way R8 would - in an
        // order that doesn't match the declaration - and read it back the way BackupManager does.
        val original = gson.toJsonTree(sampleEnvelope()).asJsonObject
        val renamed = com.google.gson.JsonObject()
        original.entrySet().reversed().forEachIndexed { index, (_, value) -> renamed.add(('a' + index).toString(), value) }

        val restored = gson.fromJson(
            ai.sach.rentup.data.backup.BackupEnvelopeKeys.normalize(renamed),
            BackupEnvelope::class.java
        )

        assertEquals(sampleEnvelope(), restored)
    }

    @Test
    fun `schema version round trips unchanged`() {
        val restored = gson.fromJson(gson.toJson(sampleEnvelope()), BackupEnvelope::class.java)
        assertEquals(BackupEnvelope.CURRENT_SCHEMA_VERSION, restored.schemaVersion)
    }

    @Test
    fun `Gson leaves a missing field null despite its non-null Kotlin type`() {
        // The exact gotcha BackupManager.validOrThrow exists to catch: a truncated or
        // hand-edited file with only some keys present. Kotlin's compiler will call
        // `tenants == null` "always false" here because the declared type is
        // `List<Tenant>`, not `List<Tenant>?` - that's a static guarantee Gson's
        // reflection-based deserializer does not honor at runtime.
        val truncatedJson = """{"schemaVersion":1,"appVersionName":"1.0","exportedAt":"x","properties":[]}"""
        val truncated = gson.fromJson(truncatedJson, BackupEnvelope::class.java)

        @Suppress("SENSELESS_COMPARISON")
        assertNull(truncated.tenants)
    }

    @Test
    fun `unparseable content raises a catchable exception rather than crashing`() {
        assertThrows(JsonSyntaxException::class.java) {
            gson.fromJson("not json at all {{{", BackupEnvelope::class.java)
        }
    }
}
