package ai.sach.rentup.data.firestore.model

/**
 * A tenant onboarding submission from Firestore, normalised.
 *
 * The backend has written submissions in two shapes: flat, with `name`, `phone` and
 * `files` at the top level, and nested, with everything under `tenantData` and the
 * uploads split into `documents` and `photos`. The importer read only the flat shape,
 * so a nested submission arrived as a tenant with a blank name and no documents.
 * Reading both here keeps that in one testable place and lets submissions already
 * stored in the old shape still import.
 */
data class TenantSubmission(
    /** The onboarding form this came from; also its document id. */
    val formId: String?,
    /** The owner's local tenant id, when the link was generated to update a tenant. */
    val tenantId: String?,
    val name: String,
    val phone: String,
    val address: String,
    val propertyAddress: String,
    val status: String,
    /** True once this app has imported it, so syncs do not redo the work. */
    val processed: Boolean,
    val familyMembers: List<Map<String, Any>>,
    val emergencyContacts: List<Map<String, Any>>,
    /** Documents and photos together, each tagged `type` = "document" or "photo". */
    val files: List<Map<String, Any>>,
    val floor: String?,
    val unitNumber: String?,
    val rentAmount: String?,
    /** Avatar keys some older submissions carried instead of a photo file. */
    val legacyAvatarUrl: String? = null
) {

    /** The tenant's photograph: a tagged photo, else any image, else the older keys. */
    val avatarUrl: String?
        get() {
            // A file with no URL is not a picture of anything - submissions stored
            // before the uploads were wired up carry names and sizes only.
            val usable = files.filter { (it["url"] as? String)?.isNotBlank() == true }
            val photo = usable.firstOrNull {
                (it["type"] as? String).equals("photo", ignoreCase = true)
            } ?: usable.firstOrNull {
                val fileName = it["originalName"] as? String ?: it["name"] as? String ?: ""
                IMAGE_EXTENSIONS.any { ext -> fileName.endsWith(ext, ignoreCase = true) }
            }
            return photo?.get("url") as? String ?: legacyAvatarUrl
        }

    companion object {
        private val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp")

        /** @param raw the document data, with its id merged in under "id". */
        @Suppress("UNCHECKED_CAST")
        fun from(raw: Map<String, Any>): TenantSubmission {
            val nested = raw["tenantData"] as? Map<String, Any> ?: emptyMap()

            fun field(key: String): Any? = raw[key] ?: nested[key]
            fun string(key: String): String? = (field(key) as? String)?.takeIf { it.isNotBlank() }
            fun list(key: String): List<Map<String, Any>> =
                (field(key) as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()

            // Older submissions split uploads into `documents` and `photos` with no
            // `type` on them, so tag those as they merge - the avatar lookup needs it.
            val files = list("files").ifEmpty {
                list("documents").map { it.withType("document") } +
                    list("photos").map { it.withType("photo") }
            }

            return TenantSubmission(
                formId = string("formId") ?: raw["id"] as? String,
                tenantId = string("tenantId"),
                name = string("name").orEmpty(),
                phone = string("phone").orEmpty(),
                address = string("address").orEmpty(),
                propertyAddress = string("propertyAddress").orEmpty(),
                status = string("status") ?: "new",
                processed = field("processed") as? Boolean ?: false,
                familyMembers = list("familyMembers"),
                emergencyContacts = list("emergencyContacts"),
                files = files,
                floor = string("floor"),
                unitNumber = string("unitNumber"),
                rentAmount = string("rentAmount"),
                legacyAvatarUrl = string("imageUrl") ?: string("avatarUrl") ?: string("tenantImage")
            )
        }

        private fun Map<String, Any>.withType(type: String): Map<String, Any> =
            if (containsKey("type")) this else this + ("type" to type)
    }
}
