package com.flowfi.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowfi.assistant.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlowFiTheme {
                val vm: AssistantViewModel = viewModel()
                var showSettings by remember { mutableStateOf(false) }
                if (showSettings) SettingsScreen(onBack = { showSettings = false })
                else AssistantScreen(vm, onSettings = { showSettings = true })
            }
        }
    }
}
