package com.flowfi.assistant.agent

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import com.flowfi.assistant.service.AssistantAccessibilityService
import org.json.JSONObject

class PhoneActions(private val ctx: Context) {

    fun execute(a: JSONObject): String = try {
        when (a.optString("type")) {
            "call" -> call(a.optString("target"))
            "whatsapp_message" -> whatsapp(a.optString("target"), a.optString("text"), "Send")
            "whatsapp_call" -> whatsapp(a.optString("target"), "", "Voice call")
            "email" -> email(a.optString("to"), a.optString("subject"), a.optString("body"))
            "open_app" -> openApp(a.optString("name"))
            "open_settings" -> openSettings(a.optString("page"))
            "set_brightness" -> brightness(a.optInt("value", 50))
            "set_volume" -> volume(a.optInt("value", 50))
            "click_text" -> if (AssistantAccessibilityService.clickText(a.optString("text"))) "ok" else "not found"
            "go_home" -> { AssistantAccessibilityService.goHome(); "ok" }
            else -> "unknown action"
        }
    } catch (e: Exception) { "error: ${e.message}" }

    private fun start(i: Intent): String { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return "ok" }

    private fun resolveNumber(target: String): String? {
        val digits = target.filter { it.isDigit() || it == '+' }
        if (digits.length >= 6 && target.count { it.isLetter() } == 0) return digits
        val c = ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?", arrayOf("%$target%"), null
        )
        c.use { if (it != null && it.moveToFirst()) return it.getString(0).filter { ch -> ch.isDigit() || ch == '+' } }
        return null
    }

    private fun call(target: String): String {
        val n = resolveNumber(target) ?: return "contact not found"
        return start(Intent(Intent.ACTION_CALL, Uri.parse("tel:$n")))
    }

    private fun whatsapp(target: String, text: String, autoClick: String): String {
        val n = resolveNumber(target)?.removePrefix("+") ?: return "contact not found"
        AssistantAccessibilityService.armAutoClick(autoClick)
        val uri = Uri.parse("https://wa.me/$n" + if (text.isNotBlank()) "?text=${Uri.encode(text)}" else "")
        return start(Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp"))
    }

    private fun email(to: String, subject: String, body: String): String {
        AssistantAccessibilityService.armAutoClick("Send")
        return start(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        })
    }

    private fun openApp(name: String): String {
        val pm = ctx.packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val hit = pm.queryIntentActivities(q, 0).firstOrNull { it.loadLabel(pm).toString().contains(name, true) }
            ?: return "app not found"
        return start(pm.getLaunchIntentForPackage(hit.activityInfo.packageName)!!)
    }

    private fun openSettings(page: String): String = start(Intent(when (page.lowercase()) {
        "wifi" -> Settings.ACTION_WIFI_SETTINGS
        "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
        "display" -> Settings.ACTION_DISPLAY_SETTINGS
        "sound" -> Settings.ACTION_SOUND_SETTINGS
        "airplane" -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
        "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
        "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
        else -> Settings.ACTION_SETTINGS
    }))

    private fun brightness(v: Int): String {
        if (!Settings.System.canWrite(ctx)) return "no WRITE_SETTINGS permission"
        Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (v.coerceIn(0, 100) * 255 / 100))
        return "ok"
    }

    private fun volume(v: Int): String {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, v.coerceIn(0, 100) * max / 100, 0)
        return "ok"
    }
}
