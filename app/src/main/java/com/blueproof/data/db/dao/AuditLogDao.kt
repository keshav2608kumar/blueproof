package com.blueproof.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.blueproof.data.db.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity): Long

    @Query("SELECT * FROM audit_log WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    suspend fun getLogsForSession(sessionId: String): List<AuditLogEntity>

    @Query("SELECT * FROM audit_log WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getLogsForSessionFlow(sessionId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 50): List<AuditLogEntity>
}
