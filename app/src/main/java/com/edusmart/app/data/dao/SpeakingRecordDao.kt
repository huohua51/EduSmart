package com.edusmart.app.data.dao

import androidx.room.*
import com.edusmart.app.data.entity.SpeakingRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeakingRecordDao {
    @Query("SELECT * FROM speaking_records ORDER BY createdAt DESC")
    fun getAllSpeakingRecords(): Flow<List<SpeakingRecordEntity>>
    
    @Query("SELECT * FROM speaking_records WHERE scene = :scene ORDER BY createdAt DESC")
    fun getSpeakingRecordsByScene(scene: String): Flow<List<SpeakingRecordEntity>>
    
    @Insert
    suspend fun insertSpeakingRecord(record: SpeakingRecordEntity)
}

