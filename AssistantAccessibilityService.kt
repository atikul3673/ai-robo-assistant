package com.flowfi.assistant.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AssistantAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: AssistantAccessibilityService? = null
        @Volatile private var pendingLabel: String? = null
        @Volatile private var pendingUntil = 0L
        private val targets = setOf("com.whatsapp", "com.whatsapp.w4b", "com.google.android.gm")

        /** পরবর্তী ১৫ সেকেন্ডে WhatsApp/Gmail-এ এই লেবেলের বাটন নিজে ক্লিক হবে (যেমন "Send") */
        fun armAutoClick(label: String, ms: Long = 15000) { pendingLabel = label; pendingUntil = System.currentTimeMillis() + ms }
        fun clickText(text: String): Boolean = instance?.let { it.rootInActiveWindow?.let { r -> it.click(r, text) } } ?: false
        fun goHome() { instance?.performGlobalAction(GLOBAL_ACTION_HOME) }
    }

    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        val label = pendingLabel ?: return
        if (System.currentTimeMillis() > pendingUntil) { pendingLabel = null; return }
        if (e?.packageName?.toString() !in targets) return
        val root = rootInActiveWindow ?: return
        if (click(root, label)) pendingLabel = null
    }

    private fun click(root: AccessibilityNodeInfo, label: String): Boolean {
        // findAccessibilityNodeInfosByText text ও contentDescription দুটোতেই খোঁজে
        for (n in root.findAccessibilityNodeInfosByText(label)) {
            var cur: AccessibilityNodeInfo? = n
            while (cur != null) {
                if (cur.isClickable) return cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                cur = cur.parent
            }
        }
        return false
    }
}
