package com.flowfi.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class VoiceEngine(private val ctx: Context, private val cb: Callback) {
    interface Callback {
        fun onPartial(t: String)
        fun onFinal(t: String)
        fun onRms(v: Float)
        fun onListenError(retry: Boolean)
        fun onSpeakDone()
    }

    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(ctx) { }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { main.post { cb.onSpeakDone() } }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) { main.post { cb.onSpeakDone() } }
        })
    }

    fun listen(langTag: String) {
        main.post {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(ctx).also { r ->
                r.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(p: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rms: Float) { cb.onRms(((rms + 2f) / 12f).coerceIn(0f, 1f)) }
                    override fun onBufferReceived(b: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onEvent(t: Int, p: Bundle?) {}
                    override fun onError(code: Int) {
                        val retry = code == SpeechRecognizer.ERROR_NO_MATCH || code == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        cb.onListenError(retry)
                    }
                    override fun onPartialResults(b: Bundle?) {
                        b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(cb::onPartial)
                    }
                    override fun onResults(b: Bundle?) {
                        val t = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (t.isNullOrBlank()) cb.onListenError(true) else cb.onFinal(t)
                    }
                })
                r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                })
            }
        }
    }

    fun speak(text: String) {
        val bn = Regex("[\\u0980-\\u09FF]").containsMatchIn(text)
        tts?.language = if (bn) Locale("bn", "BD") else Locale.US
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "flowfi")
    }

    fun stopAll() {
        main.post { recognizer?.destroy(); recognizer = null }
        tts?.stop()
    }

    fun release() {
        main.post { recognizer?.destroy() }
        tts?.shutdown()
    }
}
