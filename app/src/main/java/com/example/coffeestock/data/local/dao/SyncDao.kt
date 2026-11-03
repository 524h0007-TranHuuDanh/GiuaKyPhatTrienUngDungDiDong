package com.example.coffeestock.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.coffeestock.data.local.entity.ConflictEntity
import com.example.coffeestock.data.local.entity.SyncMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSyncMeta(meta: SyncMetaEntity)

    @Query("SELECT value FROM sync_meta WHERE `key` = :key LIMIT 1")
    suspend fun getSyncMeta(key: String): String?

    @Insert
    suspend fun insertConflict(conflict: ConflictEntity)

    @Query("""
        SELECT * FROM conflict_log
        ORDER BY createdAt DESC
    """)
    fun getAllConflicts(): Flow<List<ConflictEntity>>
}