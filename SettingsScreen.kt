package com.flowfi.assistant.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.flowfi.assistant.ai.SecureStore
import com.flowfi.assistant.service.OverlayService

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { SecureStore(ctx) }
    var provider by remember { mutableStateOf(store.provider) }
    var key by remember { mutableStateOf(store.apiKey) }
    var model by remember { mutableStateOf(store.model.ifBlank { SecureStore.defaultModel(store.provider) }) }
    var saved by remember { mutableStateOf(false) }

    fun open(a: Intent) = ctx.startActivity(a.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row { TextButton(onClick = onBack) { Text("← ফিরে যাও") } }
        Text("AI ব্রেইন", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(provider == "openai", { provider = "openai"; model = SecureStore.defaultModel("openai") }, { Text("OpenAI") })
            FilterChip(provider == "gemini", { provider = "gemini"; model = SecureStore.defaultModel("gemini") }, { Text("Gemini") })
        }
        OutlinedTextField(key, { key = it; saved = false }, label = { Text("API Key") },
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(model, { model = it; saved = false }, label = { Text("Model নাম (ইচ্ছামতো বদলানো যায়)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button({ store.save(provider, key.trim(), model.trim()); saved = true }, Modifier.fillMaxWidth()) {
            Text(if (saved) "সেভ হয়েছে ✓" else "সেভ করো")
        }

        HorizontalDivider()
        Text("ফোন কন্ট্রোল পারমিশন", style = MaterialTheme.typography.titleLarge)
        OutlinedButton({ open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, Modifier.fillMaxWidth()) { Text("Accessibility Service চালু করো") }
        OutlinedButton({ open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))) }, Modifier.fillMaxWidth()) { Text("Overlay (ভাসমান লোগো) পারমিশন") }
        OutlinedButton({ open(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))) }, Modifier.fillMaxWidth()) { Text("সিস্টেম সেটিংস বদলানোর পারমিশন") }

        HorizontalDivider()
        Text("ভাসমান লোগো", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({
                if (Settings.canDrawOverlays(ctx)) ctx.startForegroundService(Intent(ctx, OverlayService::class.java))
                else open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
            }) { Text("চালু") }
            OutlinedButton({ ctx.stopService(Intent(ctx, OverlayService::class.java)) }) { Text("বন্ধ") }
        }
    }
}
