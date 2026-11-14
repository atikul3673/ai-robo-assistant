package com.flowfi.assistant.service

import android.animation.ValueAnimator
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.IBinder
import android.view.*

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var orb: OrbView? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("flowfi", "FlowFi", NotificationManager.IMPORTANCE_MIN))
        startForeground(1, Notification.Builder(this, "flowfi")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("FlowFi সক্রিয়").build())

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val d = resources.displayMetrics
        val size = (64 * d.density).toInt()
        val lp = WindowManager.LayoutParams(
            size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = d.widthPixels / 2 - size / 2
            y = d.heightPixels - (200 * d.density).toInt()
        }
        orb = OrbView(this).also { v ->
            var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
            v.setOnTouchListener { _, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = lp.x; oy = lp.y; moved = false }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (e.rawX - sx).toInt(); val dy = (e.rawY - sy).toInt()
                        if (Math.abs(dx) + Math.abs(dy) > 12) moved = true
                        lp.x = ox + dx; lp.y = oy + dy; wm.updateViewLayout(v, lp)
                    }
                    MotionEvent.ACTION_UP -> if (!moved) startActivity(
                        Intent(this, com.flowfi.assistant.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    )
                }
                true
            }
            wm.addView(v, lp)
        }
    }

    override fun onDestroy() { orb?.let { wm.removeView(it) }; super.onDestroy() }
}

/** ছোট জ্বলজ্বলে অর্ব — কোনো ইমেজ ফাইল লাগে না */
class OrbView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var pulse = 1f
    private val anim = ValueAnimator.ofFloat(0.88f, 1f).apply {
        duration = 1500; repeatMode = ValueAnimator.REVERSE; repeatCount = ValueAnimator.INFINITE
        addUpdateListener { pulse = it.animatedValue as Float; invalidate() }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); anim.start() }
    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f; val cy = height / 2f; val r = width / 2f * pulse
        p.shader = RadialGradient(cx, cy, r, intArrayOf(0xFF8B5CF6.toInt(), 0xFF3B82F6.toInt(), 0x003B82F6), floatArrayOf(0.35f, 0.7f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p)
        p.shader = RadialGradient(cx - r * 0.2f, cy - r * 0.25f, r * 0.45f, 0xAAFFFFFF.toInt(), 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r * 0.6f, p)
    }
}
