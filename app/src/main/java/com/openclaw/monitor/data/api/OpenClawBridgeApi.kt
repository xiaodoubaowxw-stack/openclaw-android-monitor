package com.openclaw.monitor.data.api

import android.util.Log
import com.google.gson.Gson
import com.openclaw.monitor.data.model.OpenClawStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OpenClaw Bridge API Client
 * Bridge runs at: http://124.156.194.65:5568/bridge/
 */
class OpenClawBridgeApi(
    private val baseUrl: String = "http://124.156.194.65:5568"
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun pollMessages(chatId: String, after: Long = 0): Result<BridgeMessagesResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl}/bridge/poll?id=${chatId}&after=${after}"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(IOException("Empty body"))
                val data = gson.fromJson(body, BridgeMessagesResponse::class.java)
                Result.success(data)
            }
        } catch (e: Exception) {
            Log.e("BridgeApi", "pollMessages error", e)
            Result.failure(e)
        }
    }

    suspend fun getStatus(): Result<BridgeStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${baseUrl}/bridge/status")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(IOException("Empty body"))
                val data = gson.fromJson(body, BridgeStatusResponse::class.java)
                Result.success(data)
            }
        } catch (e: Exception) {
            Log.e("BridgeApi", "getStatus error", e)
            Result.failure(e)
        }
    }

    data class BridgeMessagesResponse(
        val messages: List<BridgeMessage> = emptyList(),
        val nextAfter: Long = 0
    )

    data class BridgeMessage(
        val id: String = "",
        val from: String = "",
        val text: String = "",
        val timestamp: Long = 0
    )

    data class BridgeStatusResponse(
        val status: String = "unknown",
        val uptime: String = "N/A",
        val model: String = "N/A",
        val channel: String = "N/A",
        val activeSessions: Int = 0,
        val version: String = "N/A"
    )
}
