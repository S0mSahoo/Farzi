package com.example.data.drive

import android.content.Context
import android.content.Intent
import com.example.data.model.BudgetModel
import com.example.data.model.RecurringRule
import com.example.data.model.TransactionItem
import com.example.data.model.UserProfile
import com.google.android.gms.auth.GoogleAuthUtil
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
  val version: String = "3.0.0",
  val exportTimestamp: Long = System.currentTimeMillis(),
  val userProfile: UserProfile,
  val transactions: List<TransactionItem>,
  val budgets: List<BudgetModel>,
  val recurringRules: List<RecurringRule>
)

class GoogleDriveBackupService(private val context: Context) {

  companion object {
    const val SCOPE_DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
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
        .requestScopes(Scope(SCOPE_DRIVE_APPDATA), Scope(SCOPE_DRIVE_FILE))
        .build()
      return GoogleSignIn.getClient(context, gso)
    }

  fun getSignInIntent(): Intent = signInClient.signInIntent

  fun getLastSignedInAccount(): GoogleSignInAccount? {
    return GoogleSignIn.getLastSignedInAccount(context)
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

  /**
   * Performs an actual backup upload to the user's Google Drive App Data / Files folder.
   * Returns the timestamp of successful backup or throws an Exception with a human-readable message.
   */
  suspend fun performBackup(
    account: GoogleSignInAccount,
    userProfile: UserProfile,
    transactions: List<TransactionItem>,
    budgets: List<BudgetModel>,
    recurringRules: List<RecurringRule>
  ): Long = withContext(Dispatchers.IO) {
    val androidAccount = account.account
      ?: throw IllegalStateException("Google Account credential not found. Please reconnect.")

    // 1. Retrieve OAuth2 access token for Drive scopes
    val oauthScope = "oauth2:$SCOPE_DRIVE_APPDATA $SCOPE_DRIVE_FILE"
    val accessToken = try {
      GoogleAuthUtil.getToken(context, androidAccount, oauthScope)
    } catch (e: Exception) {
      throw IOException("Google Authentication error: ${e.localizedMessage ?: "Could not acquire access token"}", e)
    }

    if (accessToken.isNullOrBlank()) {
      throw IOException("Failed to obtain a valid access token from Google Play Services.")
    }

    // 2. Serialize payload to JSON
    val payload = BackupPayload(
      userProfile = userProfile,
      transactions = transactions,
      budgets = budgets,
      recurringRules = recurringRules
    )
    val adapter = moshi.adapter(BackupPayload::class.java)
    val jsonContent = adapter.toJson(payload)

    // 3. Check if file already exists in AppDataFolder
    val existingFileId = findExistingBackupFileId(accessToken)

    val uploadSuccess = if (existingFileId != null) {
      // Update existing file
      updateFileContent(accessToken, existingFileId, jsonContent)
    } else {
      // Create new file
      createNewFile(accessToken, jsonContent)
    }

    if (!uploadSuccess) {
      throw IOException("Google Drive rejected the backup upload request. Please check your internet connection.")
    }

    val timestamp = System.currentTimeMillis()
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
