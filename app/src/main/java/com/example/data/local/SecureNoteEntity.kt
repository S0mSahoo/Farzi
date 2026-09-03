package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.SecureNoteItem
import com.example.data.model.SecureNoteType

@Entity(tableName = "secure_notes")
data class SecureNoteEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val title: String,
  val type: String, // from SecureNoteType.name
  val encryptedContent: String, // AES-GCM encrypted JSON string of payload
  val iv: String,
  val updatedAt: Long = System.currentTimeMillis()
)
