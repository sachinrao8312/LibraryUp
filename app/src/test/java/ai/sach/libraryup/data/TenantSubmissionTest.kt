package ai.sach.libraryup.data

import ai.sach.libraryup.data.firestore.model.TenantSubmission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Both shapes pinned down here are real: the flat one the submit route writes now, and
 * the nested `tenantData` one it wrote before. Reading only the flat shape is what
 * turned a filled-in form into a blank tenant row.
 */
class TenantSubmissionTest {

    private fun flat(): Map<String, Any> = mapOf(
        "id" to "form-123",
        "formId" to "form-123",
        "name" to "Asha Menon",
        "phone" to "9876543210",
        "address" to "4 Green Park, Delhi",
        "propertyAddress" to "12 Baker Street",
        "familyMembers" to listOf(mapOf("name" to "Ravi Menon", "age" to "34")),
        "emergencyContacts" to listOf(mapOf("name" to "Priya Menon", "phone" to "9998887770")),
        "files" to listOf(
            mapOf("originalName" to "aadhaar.pdf", "url" to "https://r2/doc-1", "type" to "document"),
            mapOf("originalName" to "me.jpg", "url" to "https://r2/photo-1", "type" to "photo")
        )
    )

    @Test
    fun `reads the flat shape the submit route writes`() {
        val submission = TenantSubmission.from(flat())

        assertEquals("Asha Menon", submission.name)
        assertEquals("9876543210", submission.phone)
        assertEquals("4 Green Park, Delhi", submission.address)
        assertEquals("12 Baker Street", submission.propertyAddress)
        assertEquals(1, submission.familyMembers.size)
        assertEquals(1, submission.emergencyContacts.size)
        assertEquals("https://r2/photo-1", submission.avatarUrl)
    }

    @Test
    fun `reads the nested shape, merging and tagging its split uploads`() {
        val submission = TenantSubmission.from(
            mapOf(
                "id" to "form-456",
                "tenantData" to mapOf(
                    "name" to "Asha Menon",
                    "phone" to "9876543210",
                    "familyMembers" to listOf(mapOf("name" to "Ravi Menon")),
                    "documents" to listOf(mapOf("originalName" to "pan.pdf", "url" to "https://r2/doc-9")),
                    "photos" to listOf(mapOf("originalName" to "me.png", "url" to "https://r2/photo-9"))
                )
            )
        )

        assertEquals("Asha Menon", submission.name)
        assertEquals("9876543210", submission.phone)
        assertEquals(1, submission.familyMembers.size)
        assertEquals("document", submission.files[0]["type"])
        assertEquals("photo", submission.files[1]["type"])
        assertEquals("https://r2/photo-9", submission.avatarUrl)
        assertEquals("form-456", submission.formId)
    }

    @Test
    fun `falls back through the avatar chain`() {
        // An image file with no photo tag.
        assertEquals(
            "https://r2/img-1",
            TenantSubmission.from(flat() + ("files" to listOf(
                mapOf("originalName" to "selfie.jpeg", "url" to "https://r2/img-1", "type" to "document")
            ))).avatarUrl
        )
        // The loose key older submissions used.
        assertEquals(
            "https://r2/legacy-1",
            TenantSubmission.from(
                flat() + ("files" to emptyList<Map<String, Any>>()) + ("imageUrl" to "https://r2/legacy-1")
            ).avatarUrl
        )
        // A photo entry stored before uploads were wired up carries no url.
        assertNull(
            TenantSubmission.from(flat() + ("files" to listOf(
                mapOf("originalName" to "me.jpg", "url" to "", "type" to "photo")
            ))).avatarUrl
        )
    }

    @Test
    fun `keeps the local tenant id and the processed flag`() {
        val submission = TenantSubmission.from(flat() + ("tenantId" to "42") + ("processed" to true))

        assertEquals("42", submission.tenantId)
        assertTrue(submission.processed)
        assertTrue(!TenantSubmission.from(flat()).processed)
    }

    @Test
    fun `survives a sparse or malformed document`() {
        val submission = TenantSubmission.from(
            mapOf("id" to "form-789", "name" to 42, "familyMembers" to "not a list")
        )

        assertEquals("form-789", submission.formId)
        assertEquals("", submission.name)
        assertEquals("new", submission.status)
        assertTrue(submission.familyMembers.isEmpty())
        assertTrue(submission.files.isEmpty())
        assertNull(submission.avatarUrl)
    }
}
