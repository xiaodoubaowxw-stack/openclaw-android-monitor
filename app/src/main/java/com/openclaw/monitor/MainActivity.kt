package com.openclaw.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.openclaw.monitor.ui.screens.ChatScreen
import com.openclaw.monitor.ui.theme.OpenClawMonitorTheme
import com.openclaw.monitor.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    companion object {
        // 配置区：修改以下值以连接你的 Bot 和 OpenClaw Bridge
        const val BRIDGE_URL = "http://124.156.194.65:5568"
        // TG Bot Token（通过 @BotFather 获取）
        const val BOT_TOKEN = "YOUR_BOT_TOKEN_HERE"
        // 你的 TG User ID（通过 @userinfobot 获取）
        const val MY_CHAT_ID = 0L  // 设为 0，首次启动会显示配置界面
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 读取本地配置
        val prefs = getSharedPreferences("openclaw_prefs", MODE_PRIVATE)
        val savedBotToken = prefs.getString("bot_token", BOT_TOKEN) ?: BOT_TOKEN
        val savedChatId = prefs.getLong("chat_id", MY_CHAT_ID)

        // 如果没有配置，显示设置界面
        if (savedBotToken == "YOUR_BOT_TOKEN_HERE" || savedChatId == 0L) {
            setContent {
                OpenClawMonitorTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        SetupScreen(
                            onConfigured = { token, chatId ->
                                prefs.edit()
                                    .putString("bot_token", token)
                                    .putLong("chat_id", chatId)
                                    .apply()
                                recreate()
                            }
                        )
                    }
                }
            }
            return
        }

        val viewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(savedBotToken, BRIDGE_URL)
        )[MainViewModel::class.java]

        viewModel.init(savedBotToken, savedChatId)

        setContent {
            OpenClawMonitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val messages by viewModel.messages.collectAsState()
                    val status by viewModel.status.collectAsState()
                    val inputText by viewModel.inputText.collectAsState()
                    val isLoading by viewModel.isLoading.collectAsState()
                    val connectionState by viewModel.connectionState.collectAsState()

                    ChatScreen(
                        messages = messages,
                        status = status,
                        inputText = inputText,
                        isLoading = isLoading,
                        connectionState = connectionState,
                        onInputChange = viewModel::updateInput,
                        onSend = viewModel::sendMessage
                    )
                }
            }
        }
    }
}
