package com.example.data.drive

import android.content.Context
import android.content.Intent
import com.example.data.local.PaidRecurringOccurrenceEntity
import com.example.data.model.BudgetModel
import com.example.data.model.DriveStorageInfo
import com.example.data.model.RecurringRule
import com.example.data.model.SecureNote
import com.example.data.model.TransactionItem
import com.example.data.model.UserProfile
import com.example.util.CryptoManager
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
import java.util.Locale
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

data class BackupPayload(
  val version: String = "4.0.0",
  val exportTimestamp: Long = System.currentTimeMillis(),
  val userProfile: UserProfile,
  val transactions: List<TransactionItem>,
  val budgets: List<BudgetModel>,
  val recurringRules: List<RecurringRule>,
  val paidOccurrences: List<PaidRecurringOccurrenceEntity> = emptyList(),
  val secureNotes: List<SecureNote> = emptyList()
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
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(20, TimeUnit.SECONDS)
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
   * Fetches actual Google Drive storage quota using Drive About API.
   */
  suspend fun fetchStorageQuota(account: GoogleSignInAccount): DriveStorageInfo = withContext(Dispatchers.IO) {
    try {
      val accessToken = getAccessToken(account)
      val url = "https://www.googleapis.com/drive/v3/about?fields=storageQuota,user"
      val request = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $accessToken")
        .get()
        .build()

      val response = okHttpClient.newCall(request).execute()
      if (!response.isSuccessful) {
        response.close()
        return@withContext DriveStorageInfo(isAvailable = false)
      }

      val responseBody = response.body?.string() ?: ""
      val json = JSONObject(responseBody)
      val quota = json.optJSONObject("storageQuota") ?: return@withContext DriveStorageInfo(isAvailable = false)

      val limitStr = quota.optString("limit", "0")
      val usageStr = quota.optString("usage", "0")
      val limit = limitStr.toLongOrNull() ?: 0L
      val usage = usageStr.toLongOrNull() ?: 0L

      if (limit <= 0L) {
        val usedGb = usage.toDouble() / (1024.0 * 1024.0 * 1024.0)
        return@withContext DriveStorageInfo(
          totalBytes = 0L,
          usedBytes = usage,
          availableBytes = 0L,
          formattedSummary = String.format(Locale.getDefault(), "%.1f GB used (Unlimited)", usedGb),
          isAvailable = true
        )
      }

      val available = (limit - usage).coerceAtLeast(0L)
      val availGb = available.toDouble() / (1024.0 * 1024.0 * 1024.0)
      val totalGb = limit.toDouble() / (1024.0 * 1024.0 * 1024.0)
      val usedGb = usage.toDouble() / (1024.0 * 1024.0 * 1024.0)

      val formatted = String.format(Locale.getDefault(), "%.1f GB available of %.1f GB", availGb, totalGb)

      DriveStorageInfo(
        totalBytes = limit,
        usedBytes = usage,
        availableBytes = available,
        formattedSummary = formatted,
        isAvailable = true
      )
    } catch (e: Exception) {
      DriveStorageInfo(isAvailable = false)
    }
  }

  /**
   * Fetches the user's financial records from Google Drive AppData space.
   * Transparently decrypts the cloud payload.
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

      val rawCloudContent = response.body?.string() ?: ""
      if (rawCloudContent.isBlank()) return@withContext null

      val userIdentifier = account.id ?: account.email ?: "paisa_user"
      val decryptedJson = CryptoManager.decryptCloudPayload(rawCloudContent, userIdentifier)

      val adapter = moshi.adapter(BackupPayload::class.java)
      val payload = adapter.fromJson(decryptedJson)
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
   * Transparently encrypts the cloud payload before uploading.
   */
  suspend fun saveCloudData(
    account: GoogleSignInAccount,
    payload: BackupPayload
  ): Long = withContext(Dispatchers.IO) {
    val accessToken = getAccessToken(account)
    val adapter = moshi.adapter(BackupPayload::class.java)
    val plainJson = adapter.toJson(payload)

    val userIdentifier = account.id ?: account.email ?: "paisa_user"
    val encryptedPayloadString = CryptoManager.encryptCloudPayload(plainJson, userIdentifier)

    val existingFileId = findExistingBackupFileId(accessToken)

    val uploadSuccess = if (existingFileId != null) {
      updateFileContent(accessToken, existingFileId, encryptedPayloadString)
    } else {
      createNewFile(accessToken, encryptedPayloadString)
    }

    if (!uploadSuccess) {
      throw IOException("Google Drive rejected the cloud save request. Please check your internet connection.")
    }

    val timestamp = payload.exportTimestamp
    saveLastBackupTimestamp(timestamp)
    account.email?.let { saveConnectedEmail(it) }
    timestamp
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
      // Fallback
    }
    return null
  }

  private fun createNewFile(accessToken: String, fileContent: String): Boolean {
    val metadata = JSONObject().apply {
      put("name", BACKUP_FILE_NAME)
      put("parents", org.json.JSONArray().put("appDataFolder"))
      put("mimeType", "application/json")
    }.toString()

    val metadataBody = metadata.toRequestBody("application/json; charset=UTF-8".toMediaType())
    val mediaBody = fileContent.toRequestBody("application/json; charset=UTF-8".toMediaType())

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

  private fun updateFileContent(accessToken: String, fileId: String, fileContent: String): Boolean {
    val mediaBody = fileContent.toRequestBody("application/json; charset=UTF-8".toMediaType())

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
