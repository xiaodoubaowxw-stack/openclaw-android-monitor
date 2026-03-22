package com.openclaw.monitor.data.api

import android.util.Log
import com.google.gson.Gson
import com.openclaw.monitor.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class TelegramApiClient(
    private val baseUrl: String,
    private val botToken: String
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(35, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun apiUrl(method: String): String = "$baseUrl/bot$botToken/$method"

    suspend fun getUpdates(offset: Long? = null): Result<List<TgUpdate>> = withContext(Dispatchers.IO) {
        try {
            val requestBody = GetUpdatesRequest(offset = offset).let {
                gson.toJson(it).toRequestBody(jsonMediaType)
            }
            val request = Request.Builder()
                .url(apiUrl("getUpdates"))
                .post(requestBody)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(IOException("Empty body"))
                val updatesWrapper = gson.fromJson(body, UpdatesResponse::class.java)
                Result.success(updatesWrapper.result ?: emptyList())
            }
        } catch (e: Exception) {
            Log.e("TelegramApi", "getUpdates error", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(chatId: Long, text: String): Result<TgMessage> = withContext(Dispatchers.IO) {
        try {
            val requestBody = SendMessageRequest(chatId = chatId, text = text).let {
                gson.toJson(it).toRequestBody(jsonMediaType)
            }
            val request = Request.Builder()
                .url(apiUrl("sendMessage"))
                .post(requestBody)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(IOException("Empty body"))
                val sendResp = gson.fromJson(body, SendMessageResponse::class.java)
                if (sendResp.ok && sendResp.result != null) {
                    Result.success(sendResp.result)
                } else {
                    Result.failure(IOException("Send failed: ${sendResp.description}"))
                }
            }
        } catch (e: Exception) {
            Log.e("TelegramApi", "sendMessage error", e)
            Result.failure(e)
        }
    }

    suspend fun getMe(): Result<TgUser> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(apiUrl("getMe"))
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(IOException("Empty body"))
                val resp = gson.fromJson(body, GetMeResponse::class.java)
                if (resp.ok && resp.result != null) {
                    Result.success(resp.result)
                } else {
                    Result.failure(IOException(resp.description ?: "getMe failed"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setWebhook(url: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val requestBody = SetWebhookRequest(url = url).let {
                gson.toJson(it).toRequestBody(jsonMediaType)
            }
            val request = Request.Builder()
                .url(apiUrl("setWebhook"))
                .post(requestBody)
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val resp = gson.fromJson(body, GenericResponse::class.java)
                    Result.success(resp.ok)
                } else {
                    Result.failure(IOException("HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class UpdatesResponse(val ok: Boolean, val result: List<TgUpdate>?)
    private data class SendMessageResponse(val ok: Boolean, val result: TgMessage?, val description: String? = null)
    private data class GetMeResponse(val ok: Boolean, val result: TgUser?, val description: String? = null)
    private data class GenericResponse(val ok: Boolean, val description: String? = null)
}
