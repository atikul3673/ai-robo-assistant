package com.flowfi.assistant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowfi.assistant.agent.PhoneActions
import com.flowfi.assistant.ai.AiClient
import com.flowfi.assistant.ai.SecureStore
import com.flowfi.assistant.voice.VoiceEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class Phase { Idle, Listening, Thinking, Speaking }

data class UiState(
    val phase: Phase = Phase.Idle,
    val transcript: String = "",
    val reply: String = "",
    val level: Float = 0f,
    val lang: String = "bn-BD"
)

class AssistantViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SecureStore(app)
    private val ai = AiClient()
    private val actions = PhoneActions(app)
    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    private val history = mutableListOf<Pair<String, String>>()
    private var active = false
    private var pendingAction: JSONObject? = null

    private val voice = VoiceEngine(app, object : VoiceEngine.Callback {
        override fun onPartial(t: String) { _ui.update { it.copy(transcript = t) } }
        override fun onFinal(t: String) { handle(t) }
        override fun onRms(v: Float) { _ui.update { it.copy(level = v) } }
        override fun onListenError(retry: Boolean) {
            if (active && retry) startListening() else stop()
        }
        override fun onSpeakDone() {
            pendingAction?.let { a ->
                pendingAction = null
                active = false
                _ui.update { it.copy(phase = Phase.Idle, level = 0f) }
                actions.execute(a)
                return
            }
            if (active) startListening() else _ui.update { it.copy(phase = Phase.Idle) }
        }
    })

    fun toggle() { if (active) stop() else { active = true; startListening() } }

    fun toggleLang() { _ui.update { it.copy(lang = if (it.lang.startsWith("bn")) "en-US" else "bn-BD") } }

    private fun startListening() {
        _ui.update { it.copy(phase = Phase.Listening, transcript = "", level = 0f) }
        voice.listen(_ui.value.lang)
    }

    private fun stop() {
        active = false
        voice.stopAll()
        _ui.update { it.copy(phase = Phase.Idle, level = 0f) }
    }

    private fun handle(text: String) {
        _ui.update { it.copy(phase = Phase.Thinking, transcript = text, level = 0f) }
        history.add("user" to text)
        viewModelScope.launch {
            runCatching { ai.chat(store.config(), history.takeLast(20)) }
                .onSuccess { r ->
                    history.add("assistant" to r.reply)
                    pendingAction = r.action
                    _ui.update { it.copy(phase = Phase.Speaking, reply = r.reply) }
                    voice.speak(r.reply)
                }
                .onFailure { e ->
                    active = false
                    _ui.update { it.copy(phase = Phase.Idle, reply = "ত্রুটি: ${e.message}") }
                }
        }
    }

    override fun onCleared() { voice.release() }
}
