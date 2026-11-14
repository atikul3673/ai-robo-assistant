package com.flowfi.assistant.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object FlowColors {
    val Navy = Color(0xFF050B1F)
    val Deep = Color(0xFF0A1A44)
    val Blue = Color(0xFF3B82F6)
    val Cyan = Color(0xFF2DD4BF)
    val Green = Color(0xFF34D399)
    val Purple = Color(0xFF8B5CF6)
    val Glass = Color(0x22FFFFFF)
    val GlassBorder = Color(0x33FFFFFF)
}

@Composable
fun FlowFiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = FlowColors.Blue, secondary = FlowColors.Cyan,
            background = FlowColors.Navy, surface = FlowColors.Deep,
            onBackground = Color.White, onSurface = Color.White
        ),
        content = content
    )
}
