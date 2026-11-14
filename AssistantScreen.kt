package com.flowfi.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.*

@Composable
fun Orb(phase: Phase, level: Float, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "orb")
    val pulse by t.animateFloat(
        0.94f, 1.08f,
        infiniteRepeatable(tween(if (phase == Phase.Listening) 900 else 2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")
    val lv by animateFloatAsState(level, label = "lv")
    val glowAlpha by animateFloatAsState(if (phase == Phase.Idle) 0.35f else 0.8f, label = "ga")

    Canvas(modifier.size(300.dp)) {
        val c = center
        val r = size.minDimension / 4.2f
        val s = pulse * (1f + lv * 0.28f)
        // outer glow
        drawCircle(
            Brush.radialGradient(listOf(FlowColors.Blue.copy(alpha = glowAlpha), FlowColors.Purple.copy(alpha = glowAlpha * 0.3f), Color.Transparent), c, r * 2.7f * s),
            r * 2.7f * s, c
        )
        // body
        rotate(spin, c) {
            drawCircle(
                Brush.sweepGradient(listOf(FlowColors.Blue, FlowColors.Purple, FlowColors.Cyan, Color(0xFF93C5FD), FlowColors.Blue), c),
                r * s, c
            )
        }
        // glass highlight
        drawCircle(
            Brush.radialGradient(listOf(Color(0xCCFFFFFF), Color.Transparent), c - Offset(r * 0.3f, r * 0.45f), r * 1.0f * s),
            r * s, c
        )
        // rim
        drawCircle(Color.White.copy(alpha = 0.25f), r * s, c, style = Stroke(2f))
    }
}

@Composable
fun WaveBars(phase: Phase, level: Float, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "wave")
    val ph by t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "ph")
    val amp by animateFloatAsState(
        when (phase) {
            Phase.Listening -> 0.25f + level * 0.75f
            Phase.Speaking -> 0.65f
            Phase.Thinking -> 0.22f
            Phase.Idle -> 0.07f
        }, label = "amp"
    )
    Canvas(modifier) {
        val n = 30
        val slot = size.width / n
        for (i in 0 until n) {
            val h = size.height * (0.12f + 0.88f * amp * abs(sin(ph + i * 0.38f)))
            drawRoundRect(
                color = lerp(FlowColors.Cyan, FlowColors.Purple, i / (n - 1f)),
                topLeft = Offset(i * slot + slot * 0.2f, (size.height - h) / 2),
                size = Size(slot * 0.6f, h),
                cornerRadius = CornerRadius(slot * 0.3f)
            )
        }
    }
}

@Composable
fun AssistantScreen(vm: AssistantViewModel, onSettings: () -> Unit) {
    val s by vm.ui.collectAsState()
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r[Manifest.permission.RECORD_AUDIO] == true) vm.toggle()
    }
    fun onMic() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.toggle()
        else launcher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS, Manifest.permission.POST_NOTIFICATIONS))
    }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(FlowColors.Navy, FlowColors.Deep, Color(0xFF123A7A))))
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Hey 👋", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                AssistChip(onClick = { vm.toggleLang() }, label = { Text(if (s.lang.startsWith("bn")) "বাংলা" else "English") })
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onSettings, modifier = Modifier.clip(CircleShape).background(FlowColors.Glass)) {
                    Icon(Icons.Default.Settings, null)
                }
            }

            Spacer(Modifier.weight(1f))
            Orb(s.phase, s.level)
            Text("FlowFi Assistant", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                when (s.phase) {
                    Phase.Idle -> "Smart assistant made personal"
                    Phase.Listening -> "শুনছি…"
                    Phase.Thinking -> "ভাবছি…"
                    Phase.Speaking -> "বলছি…"
                }, color = Color(0xB3FFFFFF), fontSize = 14.sp
            )
            Spacer(Modifier.weight(1f))

            if (s.transcript.isNotBlank() || s.reply.isNotBlank()) {
                val shape = RoundedCornerShape(24.dp)
                Column(
                    Modifier.fillMaxWidth().clip(shape).background(FlowColors.Glass)
                        .border(1.dp, FlowColors.GlassBorder, shape).padding(16.dp)
                ) {
                    if (s.transcript.isNotBlank()) Text("তুমি: ${s.transcript}", color = Color(0xCCFFFFFF), fontSize = 14.sp)
                    if (s.reply.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(s.reply, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            WaveBars(s.phase, s.level, Modifier.fillMaxWidth().height(56.dp))
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.size(76.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(FlowColors.Blue, FlowColors.Purple)))
                    .clickable { onMic() },
                contentAlignment = Alignment.Center
            ) {
                Icon(if (s.phase == Phase.Idle) Icons.Default.Mic else Icons.Default.Stop, null, Modifier.size(34.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
