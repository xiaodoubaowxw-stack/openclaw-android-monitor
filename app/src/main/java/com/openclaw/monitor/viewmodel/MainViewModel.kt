package com.openclaw.monitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.openclaw.monitor.data.api.OpenClawBridgeApi
import com.openclaw.monitor.data.api.TelegramApiClient
import com.openclaw.monitor.data.model.ChatMessage
import com.openclaw.monitor.data.model.OpenClawStatus
import com.openclaw.monitor.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = repository.messages
    val status: StateFlow<OpenClawStatus> = repository.status

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _connectionState = MutableStateFlow("未连接")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private var pollingJob: Job? = null
    private var statusJob: Job? = null
    private var tgPollingJob: Job? = null

    private var botToken: String = ""
    private var myChatId: Long = 0L

    fun init(botToken: String, chatId: Long) {
        this.botToken = botToken
        this.myChatId = chatId
        repository.setMyChatId(chatId)
        startPolling()
        startStatusPolling()
    }

    fun updateInput(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return
        _inputText.value = ""

        viewModelScope.launch {
            _isLoading.value = true
            repository.sendMessage(text).onFailure {
                // Message failed, remove optimistic entry
            }
            _isLoading.value = false
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                repository.pollBridgeMessages(myChatId.toString())
                delay(2000)
            }
        }
    }

    private fun startStatusPolling() {
        statusJob?.cancel()
        statusJob = viewModelScope.launch {
            repository.refreshStatus()
            while (isActive) {
                delay(10000)
                repository.refreshStatus()
                _connectionState.value = if (status.value.isRunning) "🟢 运行中" else "🔴 未连接"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        statusJob?.cancel()
        tgPollingJob?.cancel()
    }

    class Factory(
        private val botToken: String,
        private val bridgeUrl: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val bridgeApi = OpenClawBridgeApi(bridgeUrl)
            val tgApi = TelegramApiClient(bridgeUrl, botToken)
            val repo = ChatRepository(bridgeApi, tgApi)
            return MainViewModel(repo) as T
        }
    }
}
