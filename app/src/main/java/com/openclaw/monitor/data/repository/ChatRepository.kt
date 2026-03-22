package com.openclaw.monitor.data.repository

import android.util.Log
import com.openclaw.monitor.data.api.OpenClawBridgeApi
import com.openclaw.monitor.data.api.TelegramApiClient
import com.openclaw.monitor.data.model.BridgeMessage
import com.openclaw.monitor.data.model.ChatMessage
import com.openclaw.monitor.data.model.OpenClawStatus
import com.openclaw.monitor.data.model.TgUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatRepository(
    private val bridgeApi: OpenClawBridgeApi,
    private val tgApi: TelegramApiClient
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _status = MutableStateFlow(OpenClawStatus(
        isRunning = false,
        uptime = "检查中...",
        model = "检查中...",
        channel = "检查中...",
        memoryUsage = "检查中...",
        activeSessions = 0
    ))
    val status: StateFlow<OpenClawStatus> = _status.asStateFlow()

    private var myChatId: Long = 0L
    private var lastUpdateId: Long = 0L
    private var lastBridgeAfter: Long = 0L
    private val maxMessages = 200

    fun setMyChatId(chatId: Long) {
        myChatId = chatId
    }

    suspend fun pollBridgeMessages(chatId: String) {
        bridgeApi.pollMessages(chatId, lastBridgeAfter).onSuccess { response ->
            val newMessages = response.messages.mapNotNull { msg ->
                if (msg.text.isBlank()) null
                else ChatMessage(
                    id = msg.id.hashCode().toLong(),
                    senderName = msg.from.ifBlank { "OpenClaw" },
                    text = msg.text,
                    timestamp = msg.timestamp,
                    isFromMe = false,
                    isBot = false
                )
            }
            if (newMessages.isNotEmpty()) {
                val current = _messages.value.toMutableList()
                current.addAll(newMessages)
                if (current.size > maxMessages) {
                    _messages.value = current.takeLast(maxMessages)
                } else {
                    _messages.value = current
                }
            }
            lastBridgeAfter = response.nextAfter
        }.onFailure { e ->
            Log.e("ChatRepo", "pollBridgeMessages failed: ${e.message}")
        }
    }

    suspend fun fetchTgUpdates() {
        tgApi.getUpdates(offset = lastUpdateId + 1).onSuccess { updates ->
            updates.forEach { update ->
                processUpdate(update)
                lastUpdateId = maxOf(lastUpdateId, update.updateId)
            }
        }.onFailure { e ->
            Log.e("ChatRepo", "fetchTgUpdates failed: ${e.message}")
        }
    }

    private fun processUpdate(update: TgUpdate) {
        val message = update.message ?: update.editedMessage ?: return
        if (message.text.isNullOrBlank()) return

        val chatMsg = ChatMessage(
            id = message.messageId,
            senderName = message.from?.firstName
                ?: message.chat.title
                ?: message.chat.firstName
                ?: "Unknown",
            text = message.text,
            timestamp = message.date.toLong() * 1000,
            isFromMe = message.from?.id == myChatId,
            isBot = message.from?.isBot == true
        )

        val current = _messages.value.toMutableList()
        if (current.none { it.id == chatMsg.id }) {
            current.add(chatMsg)
            if (current.size > maxMessages) {
                _messages.value = current.takeLast(maxMessages)
            } else {
                _messages.value = current
            }
        }
    }

    suspend fun sendMessage(text: String): Result<Unit> {
        if (myChatId == 0L) return Result.failure(Exception("未设置 Chat ID"))

        // Add to local list immediately (optimistic)
        val localMsg = ChatMessage(
            id = System.currentTimeMillis(),
            senderName = "我",
            text = text,
            timestamp = System.currentTimeMillis(),
            isFromMe = true
        )
        _messages.value = _messages.value + localMsg

        return tgApi.sendMessage(myChatId, text).map { }
    }

    suspend fun refreshStatus() {
        bridgeApi.getStatus().onSuccess { s ->
            _status.value = OpenClawStatus(
                isRunning = s.status == "running" || s.status == "online",
                uptime = s.uptime,
                model = s.model,
                channel = s.channel,
                memoryUsage = "活跃会话: ${s.activeSessions}",
                activeSessions = s.activeSessions
            )
        }.onFailure {
            _status.value = _status.value.copy(
                isRunning = false,
                uptime = "连接失败",
                model = "—",
                channel = "—",
                memoryUsage = "—"
            )
        }
    }
}
