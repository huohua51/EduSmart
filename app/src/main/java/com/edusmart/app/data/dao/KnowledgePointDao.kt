package com.edusmart.app.data.dao

import androidx.room.*
import com.edusmart.app.data.entity.KnowledgePointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgePointDao {
    @Query("SELECT * FROM knowledge_points WHERE subject = :subject")
    fun getKnowledgePointsBySubject(subject: String): Flow<List<KnowledgePointEntity>>
    
    @Query("SELECT * FROM knowledge_points WHERE id = :id")
    suspend fun getKnowledgePointById(id: String): KnowledgePointEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgePoint(point: KnowledgePointEntity)
    
    @Update
    suspend fun updateKnowledgePoint(point: KnowledgePointEntity)
}

