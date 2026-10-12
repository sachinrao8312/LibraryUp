package ai.sach.rentup.data.local.dao

import ai.sach.rentup.data.local.entity.NotificationLog
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationLogDao {

    @Query("SELECT * FROM notification_log WHERE tenantId = :tenantId AND cycleKey = :cycleKey AND kind = :kind LIMIT 1")
    suspend fun find(tenantId: Long, cycleKey: String, kind: String): NotificationLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: NotificationLog)

    @Query("DELETE FROM notification_log WHERE tenantId = :tenantId")
    suspend fun clearForTenant(tenantId: Long)

    /** Drops ledger rows for cycles older than [oldestCycleKey] so the table stays small. */
    @Query("DELETE FROM notification_log WHERE cycleKey < :oldestCycleKey")
    suspend fun pruneBefore(oldestCycleKey: String)
}
