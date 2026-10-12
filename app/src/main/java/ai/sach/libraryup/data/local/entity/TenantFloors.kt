package ai.sach.libraryup.data.local.entity

/**
 * [Tenant.floor] holds one entry per property, comma-separated and matched by position to
 * [Tenant.propertyId]. A tenant renting several floors/units in one property has them joined
 * with "/" inside that property's entry, so commas never shift the other properties.
 */
object TenantFloors {
    /** "1, 2" or "1/2" typed in the form -> the stored entry "1/2". */
    fun encode(input: String): String =
        input.split(',', '/').map { it.trim() }.filter { it.isNotEmpty() }.joinToString("/")

    /** A stored entry -> "1, 2" for display and for editing. */
    fun display(entry: String): String =
        entry.split('/').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")

    fun entriesFor(tenant: Tenant): List<String> =
        if (tenant.propertyId.split(",").map { it.trim() }.count { it.isNotEmpty() } == 1) {
            listOf(tenant.floor.trim())
        } else {
            tenant.floor.split(",").filter { it.isNotBlank() }
        }

    fun entryFor(tenant: Tenant, propertyId: String): String {
        val index = tenant.propertyId.split(",").map { it.trim() }.indexOf(propertyId)
        return entriesFor(tenant).getOrNull(index)?.trim().orEmpty()
    }
}
