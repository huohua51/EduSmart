package com.edusmart.app.data.dao

import androidx.room.*
import com.edusmart.app.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>
    
    @Query("SELECT * FROM notes WHERE subject = :subject ORDER BY updatedAt DESC")
    fun getNotesBySubject(subject: String): Flow<List<NoteEntity>>
    
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?
    
    @Query("SELECT * FROM notes WHERE title LIKE :keyword OR content LIKE :keyword OR transcript LIKE :keyword ORDER BY updatedAt DESC")
    suspend fun searchNotes(keyword: String): List<NoteEntity>
    
    @Query("SELECT DISTINCT subject FROM notes ORDER BY subject")
    suspend fun getAllSubjects(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)
    
    @Update
    suspend fun updateNote(note: NoteEntity)
    
    @Delete
    suspend fun deleteNote(note: NoteEntity)
}

