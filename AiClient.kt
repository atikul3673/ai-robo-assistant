package com.flowfi.assistant.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AgentReply(val reply: String, val action: JSONObject?)

class AiClient {
    private val http = OkHttpClient.Builder().callTimeout(45, TimeUnit.SECONDS).build()
    private val jsonType = "application/json".toMediaType()

    suspend fun chat(cfg: AiConfig, history: List<Pair<String, String>>): AgentReply = withContext(Dispatchers.IO) {
        require(cfg.apiKey.isNotBlank()) { "Settings থেকে API Key সেট করুন" }
        val raw = if (cfg.provider == "openai") openai(cfg, history) else gemini(cfg, history)
        parse(raw)
    }

    private fun openai(cfg: AiConfig, h: List<Pair<String, String>>): String {
        val msgs = JSONArray().put(JSONObject().put("role", "system").put("content", SYSTEM))
        h.forEach { msgs.put(JSONObject().put("role", it.first).put("content", it.second)) }
        val body = JSONObject().put("model", cfg.model).put("messages", msgs)
            .put("response_format", JSONObject().put("type", "json_object"))
        val req = Request.Builder().url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer ${cfg.apiKey}")
            .post(body.toString().toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { r ->
            val s = r.body?.string().orEmpty()
            if (!r.isSuccessful) error("OpenAI ${r.code}: ${s.take(200)}")
            return JSONObject(s).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    private fun gemini(cfg: AiConfig, h: List<Pair<String, String>>): String {
        val contents = JSONArray()
        h.forEach {
            val role = if (it.first == "assistant") "model" else "user"
            contents.put(JSONObject().put("role", role).put("parts", JSONArray().put(JSONObject().put("text", it.second))))
        }
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM))))
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/${cfg.model}:generateContent")
            .header("x-goog-api-key", cfg.apiKey)
            .post(body.toString().toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { r ->
            val s = r.body?.string().orEmpty()
            if (!r.isSuccessful) error("Gemini ${r.code}: ${s.take(200)}")
            return JSONObject(s).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        }
    }

    private fun parse(raw: String): AgentReply {
        val clean = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            val o = JSONObject(clean)
            val a = o.optJSONObject("action")?.takeIf { it.optString("type").isNotBlank() && it.optString("type") != "none" }
            AgentReply(o.optString("reply", ""), a)
        } catch (e: Exception) {
            AgentReply(raw, null)
        }
    }

    companion object {
        val SYSTEM = """
You are FlowFi, a friendly voice assistant on an Android phone. The user speaks Bangla or English.
Always reply in the SAME language the user used. Keep replies short, natural and conversational (1-3 sentences) because they are read aloud. No markdown, no emojis.
You can control the phone. ALWAYS answer with ONLY a JSON object:
{"reply":"<what to say aloud>","action":{"type":"...", ...}}
Use "action":{"type":"none"} when no phone action is needed. Available actions:
- {"type":"call","target":"<contact name or phone number>"}
- {"type":"whatsapp_message","target":"<contact name or number with country code>","text":"<message>"}
- {"type":"whatsapp_call","target":"<contact name or number>"}
- {"type":"email","to":"<address>","subject":"<subject>","body":"<body>"}
- {"type":"open_app","name":"<app name>"}
- {"type":"open_settings","page":"wifi|bluetooth|display|sound|airplane|location|battery|main"}
- {"type":"set_brightness","value":0-100}
- {"type":"set_volume","value":0-100}
- {"type":"click_text","text":"<visible button text on current screen>"}
- {"type":"go_home"}
In "reply", confirm what you are about to do (e.g. "ঠিক আছে, রহিমকে কল করছি"). If required info is missing, ask for it and use action none.
""".trimIndent()
    }
}
