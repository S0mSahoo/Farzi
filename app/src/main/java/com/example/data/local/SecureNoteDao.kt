package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SecureNoteDao {
  @Query("SELECT * FROM secure_notes ORDER BY updatedAt DESC")
  fun getAllNotesFlow(): Flow<List<SecureNoteEntity>>

  @Query("SELECT * FROM secure_notes ORDER BY updatedAt DESC")
  suspend fun getAllNotes(): List<SecureNoteEntity>

  @Query("SELECT * FROM secure_notes WHERE id = :id LIMIT 1")
  suspend fun getNoteById(id: Long): SecureNoteEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNote(note: SecureNoteEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(notes: List<SecureNoteEntity>)

  @Update
  suspend fun updateNote(note: SecureNoteEntity)

  @Delete
  suspend fun deleteNote(note: SecureNoteEntity)

  @Query("DELETE FROM secure_notes WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM secure_notes")
  suspend fun clearAll()
}
