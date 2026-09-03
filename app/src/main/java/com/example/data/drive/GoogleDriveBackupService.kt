package com.example.data.drive

import android.content.Context
import android.content.Intent
import com.example.data.local.PaidRecurringOccurrenceEntity
import com.example.data.model.BudgetModel
import com.example.data.model.DriveStorageQuota
import com.example.data.model.RecurringRule
import com.example.data.model.SecureNoteItem
import com.example.data.model.TransactionItem
import com.example.data.model.UserProfile
import com.example.util.CryptoManager
import com.example.util.EncryptedBundle
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ConsentRequiredException(
  val consentIntent: Intent
) : IOException("Google Drive permission required. Please grant permission to sync.")

sealed class GoogleDriveState {
  object NotConnected : GoogleDriveState()
  object Connecting : GoogleDriveState()
  data class Connected(
    val email: String,
    val lastBackupTimestampMillis: Long?
  ) : GoogleDriveState()
  object BackingUp : GoogleDriveState()
  data class BackupSuccess(
    val email: String,
    val timestampMillis: Long
  ) : GoogleDriveState()
  data class BackupFailed(
    val email: String?,
    val errorMessage: String
  ) : GoogleDriveState()
}

data class EncryptedCloudEnvelope(
  val version: String = "4.0.0",
  val encrypted: Boolean = true,
  val exportTimestamp: Long,
  val saltBase64: String,
  val ivBase64: String,
  val ciphertextBase64: String,
  val userEmail: String? = null
)

data class BackupPayload(
  val version: String = "4.0.0",
  val exportTimestamp: Long = System.currentTimeMillis(),
  val userProfile: UserProfile,
  val transactions: List<TransactionItem>,
  val budgets: List<BudgetModel>,
  val recurringRules: List<RecurringRule>,
  val secureNotes: List<SecureNoteItem> = emptyList(),
  val paidOccurrences: List<PaidRecurringOccurrenceEntity> = emptyList()
)

class GoogleDriveBackupService(private val context: Context) {

  companion object {
    const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    const val BACKUP_FILE_NAME = "paisa_financial_backup.json"
    private const val PREFS_NAME = "paisa_drive_prefs"
    private const val KEY_LAST_BACKUP = "key_last_backup_timestamp"
    private const val KEY_CONNECTED_EMAIL = "key_connected_email"
  }

  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(25, TimeUnit.SECONDS)
    .writeTimeout(25, TimeUnit.SECONDS)
    .build()

  private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  val signInClient: GoogleSignInClient
    get() {
      val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .requestScopes(Scope(SCOPE_DRIVE_APPDATA))
        .build()
      return GoogleSignIn.getClient(context, gso)
    }

  fun getSignInIntent(): Intent = signInClient.signInIntent

  fun getLastSignedInAccount(): GoogleSignInAccount? {
    return GoogleSignIn.getLastSignedInAccount(context)
  }

  fun hasDrivePermission(account: GoogleSignInAccount?): Boolean {
    if (account == null) return false
    return GoogleSignIn.hasPermissions(account, Scope(SCOPE_DRIVE_APPDATA))
  }

  fun getSavedEmail(): String? {
    return prefs.getString(KEY_CONNECTED_EMAIL, null) ?: getLastSignedInAccount()?.email
  }

  fun getLastBackupTimestamp(): Long? {
    val ts = prefs.getLong(KEY_LAST_BACKUP, 0L)
    return if (ts > 0L) ts else null
  }

  fun saveLastBackupTimestamp(timestamp: Long) {
    prefs.edit().putLong(KEY_LAST_BACKUP, timestamp).apply()
  }

  fun saveConnectedEmail(email: String) {
    prefs.edit().putString(KEY_CONNECTED_EMAIL, email).apply()
  }

  suspend fun signOut(): Boolean = withContext(Dispatchers.IO) {
    try {
      signInClient.signOut()
      prefs.edit().remove(KEY_CONNECTED_EMAIL).apply()
      true
    } catch (e: Exception) {
      false
    }
  }

  private fun getAccessToken(account: GoogleSignInAccount): String {
    val androidAccount = account.account
      ?: throw IllegalStateException("Google Account credential not found. Please reconnect.")

    val oauthScope = "oauth2:$SCOPE_DRIVE_APPDATA"
    return try {
      val token = GoogleAuthUtil.getToken(context, androidAccount, oauthScope)
      if (token.isNullOrBlank()) {
        throw IOException("Failed to obtain a valid access token from Google Play Services.")
      }
      token
    } catch (e: UserRecoverableAuthException) {
      val intent = e.intent ?: signInClient.signInIntent
      throw ConsentRequiredException(intent)
    } catch (e: Exception) {
      if (e is IOException) throw e
      throw IOException("Google Authentication error: ${e.localizedMessage ?: "Could not acquire access token"}", e)
    }
  }

  /**
   * Fetches Google Drive available storage quota information.
   */
  suspend fun fetchStorageQuota(account: GoogleSignInAccount): DriveStorageQuota? = withContext(Dispatchers.IO) {
    try {
      val accessToken = getAccessToken(account)
      val url = "https://www.googleapis.com/drive/v3/about?fields=storageQuota"
      val request = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $accessToken")
        .get()
        .build()

      val response = okHttpClient.newCall(request).execute()
      if (!response.isSuccessful) {
        response.close()
        return@withContext null
      }

      val bodyString = response.body?.string() ?: ""
      val json = JSONObject(bodyString)
      val quotaObj = json.optJSONObject("storageQuota") ?: return@withContext null
      val limit = quotaObj.optLong("limit", 0L)
      val usage = quotaObj.optLong("usage", 0L)
      val usageInDrive = quotaObj.optLong("usageInDrive", 0L)
      DriveStorageQuota(
        totalBytes = limit,
        usedBytes = usage,
        usageInDriveBytes = usageInDrive
      )
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Fetches the user's financial records from Google Drive AppData space.
   * Decrypts AES-256-GCM encrypted cloud payloads using cross-device key derivation.
   * Returns the BackupPayload if found and parsed, or null if no cloud backup exists yet.
   */
  suspend fun fetchCloudData(account: GoogleSignInAccount): BackupPayload? = withContext(Dispatchers.IO) {
    val accessToken = getAccessToken(account)
    val existingFileId = findExistingBackupFileId(accessToken) ?: return@withContext null

    try {
      val url = "https://www.googleapis.com/drive/v3/files/$existingFileId?alt=media"
      val request = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $accessToken")
        .get()
        .build()

      val response = okHttpClient.newCall(request).execute()
      if (!response.isSuccessful) {
        response.close()
        throw IOException("Failed to download cloud records from Google Drive (${response.code})")
      }

      val jsonContent = response.body?.string() ?: ""
      if (jsonContent.isBlank()) return@withContext null

      val userKeyId = account.id ?: account.email ?: "paisa_authenticated_user"
      val envelopeAdapter = moshi.adapter(EncryptedCloudEnvelope::class.java)
      val envelope = try { envelopeAdapter.fromJson(jsonContent) } catch (e: Exception) { null }

      val payloadJson = if (envelope != null && envelope.encrypted) {
        // Decrypt using cross-device PBKDF2 derived key
        val bundle = EncryptedBundle(
          saltBase64 = envelope.saltBase64,
          ivBase64 = envelope.ivBase64,
          ciphertextBase64 = envelope.ciphertextBase64
        )
        CryptoManager.decryptCloudPayload(bundle, userKeyId)
      } else {
        // Fallback for older unencrypted backup files
        jsonContent
      }

      val adapter = moshi.adapter(BackupPayload::class.java)
      val payload = adapter.fromJson(payloadJson)
      if (payload != null) {
        saveLastBackupTimestamp(payload.exportTimestamp)
        account.email?.let { saveConnectedEmail(it) }
      }
      payload
    } catch (e: Exception) {
      if (e is IOException) throw e
      throw IOException("Error parsing cloud financial backup: ${e.localizedMessage}", e)
    }
  }

  /**
   * Saves the user's complete financial payload to Google Drive AppData space.
   * Encrypts payload with AES-256-GCM before writing to Google Drive.
   * Returns the timestamp of successful save.
   */
  suspend fun saveCloudData(
    account: GoogleSignInAccount,
    payload: BackupPayload
  ): Long = withContext(Dispatchers.IO) {
    val accessToken = getAccessToken(account)
    val payloadAdapter = moshi.adapter(BackupPayload::class.java)
    val plainJson = payloadAdapter.toJson(payload)

    // Encrypt payload before saving to Google Drive
    val userKeyId = account.id ?: account.email ?: "paisa_authenticated_user"
    val encryptedBundle = CryptoManager.encryptCloudPayload(plainJson, userKeyId)

    val envelope = EncryptedCloudEnvelope(
      version = payload.version,
      encrypted = true,
      exportTimestamp = payload.exportTimestamp,
      saltBase64 = encryptedBundle.saltBase64,
      ivBase64 = encryptedBundle.ivBase64,
      ciphertextBase64 = encryptedBundle.ciphertextBase64,
      userEmail = account.email
    )
    val envelopeAdapter = moshi.adapter(EncryptedCloudEnvelope::class.java)
    val encryptedJsonContent = envelopeAdapter.toJson(envelope)

    val existingFileId = findExistingBackupFileId(accessToken)

    val uploadSuccess = if (existingFileId != null) {
      updateFileContent(accessToken, existingFileId, encryptedJsonContent)
    } else {
      createNewFile(accessToken, encryptedJsonContent)
    }

    if (!uploadSuccess) {
      throw IOException("Google Drive rejected the cloud save request. Please check your internet connection.")
    }

    val timestamp = payload.exportTimestamp
    saveLastBackupTimestamp(timestamp)
    account.email?.let { saveConnectedEmail(it) }
    timestamp
  }

  /**
   * Performs an actual backup upload to the user's Google Drive App Data / Files folder.
   * Returns the timestamp of successful backup or throws an Exception with a human-readable message.
   */
  suspend fun performBackup(
    account: GoogleSignInAccount,
    userProfile: UserProfile,
    transactions: List<TransactionItem>,
    budgets: List<BudgetModel>,
    recurringRules: List<RecurringRule>,
    secureNotes: List<SecureNoteItem> = emptyList(),
    paidOccurrences: List<PaidRecurringOccurrenceEntity> = emptyList()
  ): Long = withContext(Dispatchers.IO) {
    val payload = BackupPayload(
      version = "4.0.0",
      exportTimestamp = System.currentTimeMillis(),
      userProfile = userProfile,
      transactions = transactions,
      budgets = budgets,
      recurringRules = recurringRules,
      secureNotes = secureNotes,
      paidOccurrences = paidOccurrences
    )
    saveCloudData(account, payload)
  }

  private fun findExistingBackupFileId(accessToken: String): String? {
    try {
      val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name='$BACKUP_FILE_NAME'+and+trashed=false&fields=files(id,name)"
      val request = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $accessToken")
        .get()
        .build()

      val response = okHttpClient.newCall(request).execute()
      if (!response.isSuccessful) {
        response.close()
        return null
      }

      val responseBody = response.body?.string() ?: ""
      val json = JSONObject(responseBody)
      val files = json.optJSONArray("files")
      if (files != null && files.length() > 0) {
        return files.getJSONObject(0).optString("id")
      }
    } catch (e: Exception) {
      // Fallback to creating a new file
    }
    return null
  }

  private fun createNewFile(accessToken: String, jsonContent: String): Boolean {
    val metadata = JSONObject().apply {
      put("name", BACKUP_FILE_NAME)
      put("parents", org.json.JSONArray().put("appDataFolder"))
      put("mimeType", "application/json")
    }.toString()

    val metadataBody = metadata.toRequestBody("application/json; charset=UTF-8".toMediaType())
    val mediaBody = jsonContent.toRequestBody("application/json; charset=UTF-8".toMediaType())

    val requestBody = MultipartBody.Builder()
      .setType(MultipartBody.FORM)
      .addPart(metadataBody)
      .addPart(mediaBody)
      .build()

    val request = Request.Builder()
      .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
      .header("Authorization", "Bearer $accessToken")
      .post(requestBody)
      .build()

    val response = okHttpClient.newCall(request).execute()
    val isOk = response.isSuccessful
    response.close()
    return isOk
  }

  private fun updateFileContent(accessToken: String, fileId: String, jsonContent: String): Boolean {
    val mediaBody = jsonContent.toRequestBody("application/json; charset=UTF-8".toMediaType())

    val request = Request.Builder()
      .url("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
      .header("Authorization", "Bearer $accessToken")
      .patch(mediaBody)
      .build()

    val response = okHttpClient.newCall(request).execute()
    val isOk = response.isSuccessful
    response.close()
    return isOk
  }
}
