package com.example.goldshop

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class DriveBackupFile(val id: String, val name: String, val createdTime: String, val size: Long)

// پشتیبان‌گیری در Google Drive با دسترسی drive.file (فقط فایل‌های ساخته‌شده توسط خود برنامه)
object DriveBackup {
    private const val FOLDER_NAME = "GoldShopPro-Backups"
    private const val KEEP_LAST = 14
    private const val API = "https://www.googleapis.com/drive/v3/files"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
    private const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"

    private val client = OkHttpClient.Builder()
        .callTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun authRequest(): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(SCOPE_DRIVE_FILE)))
            .build()

    // توکن دسترسی؛ اگر هنوز رضایت کاربر گرفته نشده باشد null برمی‌گرداند
    suspend fun accessTokenOrNull(context: Context): String? = try {
        val result = Identity.getAuthorizationClient(context).authorize(authRequest()).await()
        if (result.hasResolution()) null else result.accessToken
    } catch (e: Exception) {
        null
    }

    private fun auth(b: Request.Builder, token: String) = b.header("Authorization", "Bearer $token")

    private fun execute(req: Request): String {
        return client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IOException("Drive HTTP " + resp.code + ": " + body.take(200))
            body
        }
    }

    private fun findFolder(token: String): String? {
        val url = API.toHttpUrl().newBuilder()
            .addQueryParameter("q", "name = '$FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
            .addQueryParameter("fields", "files(id,name)")
            .addQueryParameter("spaces", "drive")
            .build()
        val json = JSONObject(execute(auth(Request.Builder().url(url), token).build()))
        val arr = json.optJSONArray("files") ?: return null
        return if (arr.length() > 0) arr.getJSONObject(0).getString("id") else null
    }

    private fun createFolder(token: String): String {
        val meta = JSONObject()
            .put("name", FOLDER_NAME)
            .put("mimeType", "application/vnd.google-apps.folder")
            .toString()
            .toRequestBody("application/json; charset=UTF-8".toMediaType())
        val req = auth(Request.Builder().url(API).post(meta), token).build()
        return JSONObject(execute(req)).getString("id")
    }

    private fun folderId(token: String): String = findFolder(token) ?: createFolder(token)

    private fun uploadFile(token: String, folder: String, file: File, name: String, mime: String) {
        val meta = JSONObject()
            .put("name", name)
            .put("parents", org.json.JSONArray().put(folder))
            .toString()
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(meta.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(file.asRequestBody(mime.toMediaType()))
            .build()
        execute(auth(Request.Builder().url(UPLOAD).post(body), token).build())
    }

    fun listBackups(token: String): List<DriveBackupFile> {
        val folder = findFolder(token) ?: return emptyList()
        val url = API.toHttpUrl().newBuilder()
            .addQueryParameter("q", "'$folder' in parents and trashed = false and name contains 'goldshop-backup'")
            .addQueryParameter("orderBy", "createdTime desc")
            .addQueryParameter("pageSize", "50")
            .addQueryParameter("fields", "files(id,name,createdTime,size)")
            .build()
        val arr = JSONObject(execute(auth(Request.Builder().url(url), token).build())).optJSONArray("files")
            ?: return emptyList()
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            DriveBackupFile(o.getString("id"), o.getString("name"), o.optString("createdTime"), o.optString("size", "0").toLongOrNull() ?: 0L)
        }
    }

    private fun deleteFile(token: String, id: String) {
        val req = auth(Request.Builder().url("$API/$id").delete(), token).build()
        client.newCall(req).execute().close()
    }

    // ساخت نسخه پشتیبان از دیتابیس و آپلود در Drive؛ نسخه‌های قدیمی‌تر از ۱۴ عدد حذف می‌شوند
    suspend fun backupNow(context: Context, token: String): String = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "drive-backup.db")
        if (!BackupHelper.exportToFile(context, tmp)) throw IOException("ساخت فایل پشتیبان ناموفق بود")
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val name = "goldshop-backup-$stamp.db"
        val folder = folderId(token)
        uploadFile(token, folder, tmp, name, "application/octet-stream")
        tmp.delete()
        runCatching {
            listBackups(token).drop(KEEP_LAST).forEach { deleteFile(token, it.id) }
        }
        name
    }

    // آپلود گزارش PDF ماهانه در همان پوشه
    suspend fun uploadReport(token: String, file: File) = withContext(Dispatchers.IO) {
        uploadFile(token, folderId(token), file, file.name, "application/pdf")
    }

    // دانلود یک پشتیبان به فایل محلی
    suspend fun download(token: String, id: String, target: File) = withContext(Dispatchers.IO) {
        val req = auth(Request.Builder().url("$API/$id?alt=media"), token).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Drive HTTP " + resp.code)
            resp.body!!.byteStream().use { input -> target.outputStream().use { input.copyTo(it) } }
        }
    }
}
