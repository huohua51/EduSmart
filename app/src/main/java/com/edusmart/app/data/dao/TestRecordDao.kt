package com.edusmart.app.data.dao

import androidx.room.*
import com.edusmart.app.data.entity.TestRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TestRecordDao {
    @Query("SELECT * FROM test_records ORDER BY testTime DESC")
    fun getAllTestRecords(): Flow<List<TestRecordEntity>>
    
    @Query("SELECT * FROM test_records WHERE subject = :subject ORDER BY testTime DESC")
    fun getTestRecordsBySubject(subject: String): Flow<List<TestRecordEntity>>
    
    @Insert
    suspend fun insertTestRecord(record: TestRecordEntity)
}

