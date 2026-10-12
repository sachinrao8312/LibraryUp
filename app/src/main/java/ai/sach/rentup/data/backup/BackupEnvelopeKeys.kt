package ai.sach.rentup.data.backup

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Recovers the field names of a backup written by a minified release build.
 *
 * Until the R8 keep rule for this package existed, release builds renamed
 * [BackupEnvelope]'s fields, so their `backup.json` reads like
 * `{"a": 2, "b": "1.0", "d": [...], "e": [...]}` - and the renaming changes from one
 * release to the next. The rows inside the lists were never renamed (the Room entities
 * were always kept), so each list can still be identified by the fields its rows carry,
 * and the two strings and the version number by their shape.
 */
internal object BackupEnvelopeKeys {

    /** Returns [root] unchanged when it already uses the real field names. */
    fun normalize(root: JsonObject): JsonObject {
        if (root.has("schemaVersion")) return root

        val normalized = JsonObject()
        for ((_, value) in root.entrySet()) {
            when {
                value.isJsonArray -> {
                    val key = listKeyFor(value.asJsonArray) ?: continue
                    if (!normalized.has(key)) normalized.add(key, value)
                }
                value.isJsonPrimitive && value.asJsonPrimitive.isNumber ->
                    if (!normalized.has("schemaVersion")) normalized.add("schemaVersion", value)
                value.isJsonPrimitive && value.asJsonPrimitive.isString -> {
                    val key = if (looksLikeInstant(value.asString)) "exportedAt" else "appVersionName"
                    if (!normalized.has(key)) normalized.add(key, value)
                }
            }
        }

        // Nothing recognisable: leave it to the caller to reject as "not a RentUp backup".
        if (!normalized.has("schemaVersion")) return root

        // An empty list can't be told apart from any other, but empty is empty.
        LIST_KEYS.forEach { key -> if (!normalized.has(key)) normalized.add(key, JsonArray()) }
        return normalized
    }

    private val LIST_KEYS = listOf(
        "properties", "tenants", "tenantFamilyInfo", "paymentHistory",
        "tenantPropertyCrossRef", "tenantDocuments"
    )

    /**
     * Gson leaves out null fields, so each check looks for any of several fields unique to
     * that entity. Order matters: the more specific shapes are tested first.
     */
    private fun listKeyFor(array: JsonArray): String? {
        val row = array.firstOrNull { it.isJsonObject }?.asJsonObject ?: return null
        fun hasAny(vararg names: String) = names.any { row.has(it) }
        return when {
            hasAny("documentPath", "documentType", "uploadDate", "mimeType") -> "tenantDocuments"
            hasAny("rentAmount", "unitNumber", "startDate", "endDate", "isActive") -> "tenantPropertyCrossRef"
            hasAny("relationship", "isEmergencyContact", "age") -> "tenantFamilyInfo"
            row.has("tenantId") && hasAny("collectedBy", "remark", "date") -> "paymentHistory"
            !row.has("tenantId") && hasAny("isPaid", "dueDate", "permanentAddress", "avatarUri", "formId", "status") -> "tenants"
            !row.has("tenantId") && hasAny("totalUnits", "floors", "occupancyRate", "unitRent", "address") -> "properties"
            else -> null
        }
    }

    private fun looksLikeInstant(value: String): Boolean =
        Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}.*""").matches(value)

    private fun JsonArray.firstOrNull(predicate: (JsonElement) -> Boolean): JsonElement? {
        for (element in this) if (predicate(element)) return element
        return null
    }
}
