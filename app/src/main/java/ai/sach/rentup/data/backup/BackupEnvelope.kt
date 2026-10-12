package ai.sach.rentup.data.backup

import ai.sach.rentup.data.local.entity.PaymentHistory
import ai.sach.rentup.data.local.entity.Property
import ai.sach.rentup.data.local.entity.Tenant
import ai.sach.rentup.data.local.entity.TenantDocument
import ai.sach.rentup.data.local.entity.TenantFamilyInfo
import ai.sach.rentup.data.local.entity.TenantPropertyCrossRef

/**
 * The whole shape of a backup: one JSON document, everything a landlord's account holds,
 * restorable on any device. It travels as the `backup.json` entry inside the backup zip,
 * alongside the `attachments/` the rows point at - see [BackupManager].
 *
 * Deliberately reuses the Room entities directly rather than a parallel DTO layer - they
 * are already plain data classes, Gson serializes them the same way either way, and a
 * second set of types to keep in sync with the schema is exactly the complexity this
 * feature is meant to avoid.
 *
 * [NotificationLog] is not included: it is a delivery ledger the reminder sweep re-derives
 * on its own, not user data. Restoring stale entries would only suppress reminders that
 * are genuinely due.
 *
 * A [Tenant.avatarUri] and a [TenantDocument.documentPath] in here are not device URIs.
 * They are either blank, or a `rentup-backup:attachments/...` marker naming an entry in
 * the same zip, which [BackupManager] rewrites into a real URI on restore. A raw
 * `content://` path is never written: it names a sandbox that will not exist on the device
 * reading it.
 *
 * @param schemaVersion Bumped whenever this shape changes, so [BackupManager] can refuse
 *   (or one day migrate) a file from an incompatible version instead of silently
 *   misreading it.
 * @param appVersionName The app's own version at export time - shown to the user, not
 *   otherwise interpreted, so a support conversation about an old backup has something to
 *   go on.
 * @param exportedAt ISO-8601 instant, for the same reason.
 */
data class BackupEnvelope(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appVersionName: String,
    val exportedAt: String,
    val properties: List<Property>,
    val tenants: List<Tenant>,
    val tenantFamilyInfo: List<TenantFamilyInfo>,
    val paymentHistory: List<PaymentHistory>,
    val tenantPropertyCrossRef: List<TenantPropertyCrossRef>,
    val tenantDocuments: List<TenantDocument>
) {
    companion object {
        /**
         * 1 - JSON document; document paths blanked, tenant photos not carried.
         * 2 - zip container; photos and documents carried in `attachments/`.
         *
         * A version 1 file still restores every row it holds, so [BackupManager] reads one
         * rather than refusing it. Only a version newer than this is refused.
         */
        const val CURRENT_SCHEMA_VERSION = 2
    }
}
