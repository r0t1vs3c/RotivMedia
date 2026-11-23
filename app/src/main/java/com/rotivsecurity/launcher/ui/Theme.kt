package com.rotivsecurity.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.rotivsecurity.launcher.R

/** Paleta oficial do guia de estilo RotivSecurity. */
object Rotiv {
    val Dark = Color(0xFF070B14)
    val Dark2 = Color(0xFF0B1120)
    val Acc = Color(0xFF3B82F6)
    val AccL = Color(0xFF93C5FD)
    val AccD = Color(0xFF1D4ED8)
    val Red = Color(0xFFEF4444)
    val RedL = Color(0xFFFCA5A5)
    val Text = Color.White
    val Muted = Color(0xA6FFFFFF)
    val Border = Color(0x2E3B82F6)
    val Track = Color(0x1FFFFFFF)
    val Chip = Color(0x263B82F6)
    val MapRoad = Color(0xFF131C33)
    val MapStreet = Color(0xFF101829)
}

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.Bold),
)

/** Altura de referência do layout: tudo é desenhado numa tela de 400 "dp" de altura. */
private const val DESIGN_HEIGHT = 400f

/**
 * Tema + escala: ajusta a densidade para que o layout ocupe a tela igual em
 * qualquer DPI da central (1280x720 vira uma tela de ~711x400).
 */
@Composable
fun RotivTheme(
    /** A tela desenha atrás da barra da central: a escala desconta a altura dela. */
    edgeToEdge: Boolean = false,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Rotiv.Dark)) {
        val bar = if (edgeToEdge) WindowInsets.statusBars.getTop(LocalDensity.current) else 0
        val hPx = (constraints.maxHeight - bar).toFloat().coerceAtLeast(1f)
        CompositionLocalProvider(
            LocalDensity provides Density(hPx / DESIGN_HEIGHT, 1f),
            LocalContentColor provides Rotiv.Text,
            LocalTextStyle provides TextStyle(fontFamily = SpaceGrotesk, color = Rotiv.Text),
        ) {
            content()
        }
    }
}
