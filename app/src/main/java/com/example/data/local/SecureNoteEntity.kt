package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.SecureNote
import com.example.data.model.SecureNoteType
import com.example.util.CryptoManager
import org.json.JSONObject

@Entity(tableName = "secure_notes")
data class SecureNoteEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val title: String,
  val type: String = SecureNoteType.GENERIC.name,
  val encryptedPayload: String, // Encrypted JSON holding content and structured fields
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
) {
  fun toModel(): SecureNote {
    val noteType = try {
      SecureNoteType.valueOf(type)
    } catch (e: Exception) {
      SecureNoteType.GENERIC
    }

    val decryptedJson = CryptoManager.decryptLocalVaultData(encryptedPayload)
    var content = ""
    var maskedNumber: String? = null
    var expiryDate: String? = null
    var cvv: String? = null
    var ifscCode: String? = null
    var accountNumber: String? = null
    val additionalFields = mutableMapOf<String, String>()

    if (decryptedJson.isNotBlank()) {
      try {
        val json = JSONObject(decryptedJson)
        content = json.optString("content", "")
        maskedNumber = json.optString("maskedNumber", "").takeIf { it.isNotBlank() }
        expiryDate = json.optString("expiryDate", "").takeIf { it.isNotBlank() }
        cvv = json.optString("cvv", "").takeIf { it.isNotBlank() }
        ifscCode = json.optString("ifscCode", "").takeIf { it.isNotBlank() }
        accountNumber = json.optString("accountNumber", "").takeIf { it.isNotBlank() }

        val extra = json.optJSONObject("extra")
        if (extra != null) {
          val keys = extra.keys()
          while (keys.hasNext()) {
            val k = keys.next()
            additionalFields[k] = extra.optString(k, "")
          }
        }
      } catch (e: Exception) {
        content = decryptedJson
      }
    }

    return SecureNote(
      id = id,
      title = title,
      content = content,
      type = noteType,
      maskedNumber = maskedNumber,
      expiryDate = expiryDate,
      cvv = cvv,
      ifscCode = ifscCode,
      accountNumber = accountNumber,
      additionalFields = additionalFields,
      createdAt = createdAt,
      updatedAt = updatedAt
    )
  }

  companion object {
    fun fromModel(model: SecureNote): SecureNoteEntity {
      val json = JSONObject()
      json.put("content", model.content)
      model.maskedNumber?.let { json.put("maskedNumber", it) }
      model.expiryDate?.let { json.put("expiryDate", it) }
      model.cvv?.let { json.put("cvv", it) }
      model.ifscCode?.let { json.put("ifscCode", it) }
      model.accountNumber?.let { json.put("accountNumber", it) }

      if (model.additionalFields.isNotEmpty()) {
        val extra = JSONObject()
        model.additionalFields.forEach { (k, v) -> extra.put(k, v) }
        json.put("extra", extra)
      }

      val encrypted = CryptoManager.encryptLocalVaultData(json.toString())
      return SecureNoteEntity(
        id = model.id,
        title = model.title,
        type = model.type.name,
        encryptedPayload = encrypted,
        createdAt = model.createdAt,
        updatedAt = model.updatedAt
      )
    }
  }
}
