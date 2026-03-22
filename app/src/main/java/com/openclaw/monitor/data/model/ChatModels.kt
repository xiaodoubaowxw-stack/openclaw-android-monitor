package com.openclaw.monitor.data.model

import com.google.gson.annotations.SerializedName

/**
 * Telegram Bot API Update object
 */
data class TgUpdate(
    @SerializedName("update_id") val updateId: Long,
    @SerializedName("message") val message: TgMessage? = null,
    @SerializedName("edited_message") val editedMessage: TgMessage? = null
)

data class TgMessage(
    @SerializedName("message_id") val messageId: Long,
    @SerializedName("from") val from: TgUser? = null,
    @SerializedName("chat") val chat: TgChat,
    @SerializedName("date") val date: Long,
    @SerializedName("text") val text: String? = null,
    @SerializedName("entities") val entities: List<TgMessageEntity>? = null
)

data class TgUser(
    @SerializedName("id") val id: Long,
    @SerializedName("is_bot") val isBot: Boolean,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("username") val username: String? = null
)

data class TgChat(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("first_name") val firstName: String? = null
)

data class TgMessageEntity(
    @SerializedName("type") val type: String,
    @SerializedName("offset") val offset: Int,
    @SerializedName("length") val length: Int
)

/**
 * Send Message request
 */
data class SendMessageRequest(
    @SerializedName("chat_id") val chatId: Long,
    @SerializedName("text") val text: String,
    @SerializedName("parse_mode") val parseMode: String = "Markdown"
)

/**
 * GetUpdates request
 */
data class GetUpdatesRequest(
    @SerializedName("offset") val offset: Long? = null,
    @SerializedName("limit") val limit: Int = 100,
    @SerializedName("timeout") val timeout: Int = 30
)

/**
 * SetWebhook request
 */
data class SetWebhookRequest(
    @SerializedName("url") val url: String,
    @SerializedName("max_connections") val maxConnections: Int = 40
)

/**
 * App-level Chat Message
 */
data class ChatMessage(
    val id: Long,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val isBot: Boolean = false
)

/**
 * OpenClaw Status
 */
data class OpenClawStatus(
    val isRunning: Boolean,
    val uptime: String,
    val model: String,
    val channel: String,
    val memoryUsage: String,
    val activeSessions: Int,
    val lastChecked: Long = System.currentTimeMillis()
)
