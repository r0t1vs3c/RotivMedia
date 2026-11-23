package com.rotivsecurity.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotivsecurity.launcher.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Garante uma única animação por inicialização da central.
 *
 * O Android pode fechar o processo do launcher a qualquer momento (outro app pesado,
 * economia de bateria da Unisoc). Por isso a marca de "já mostrou" fica salva junto com
 * o número do boot, e não só na memória: voltar para a tela inicial não repete a animação.
 * Ao acordar a central depois de um tempo com a tela apagada (se ela dormir em vez de
 * desligar com o carro), também conta como ligar o carro.
 */
object WelcomeState {
    private const val PREFS = "boas_vindas"
    private const val KEY_BOOT = "boot_mostrado"
    /** Tela apagada por pelo menos este tempo = o carro foi desligado. */
    private const val SLEEP_MS = 5 * 60_000L
    /** A tela ligou e ainda vale mostrar (até 30 s depois). */
    private const val WAKE_WINDOW_MS = 30_000L

    /** Pedido de "ver boas-vindas agora" vindo da tela de Ajustes. */
    val previewRequest = kotlinx.coroutines.flow.MutableStateFlow(false)

    @Volatile private var screenOffAt = 0L
    @Volatile private var wakeAt = 0L

    private fun bootId(ctx: Context): String {
        val count = android.provider.Settings.Global.getInt(
            ctx.contentResolver, android.provider.Settings.Global.BOOT_COUNT, -1,
        )
        if (count >= 0) return "n$count"
        // sem contador: hora do boot, arredondada para o minuto
        return "t" + (System.currentTimeMillis() - SystemClock.elapsedRealtime()) / 60_000L
    }

    /** Ainda não mostrou neste boot. */
    fun pendingThisBoot(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_BOOT, null) != bootId(ctx)

    fun markShown(ctx: Context) {
        wakeAt = 0L
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_BOOT, bootId(ctx)).apply()
    }

    /** Escuta a tela apagar e acender (chamado uma vez, no início do app). */
    fun watchScreen(app: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        app.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val now = SystemClock.elapsedRealtime()
                when (i.action) {
                    Intent.ACTION_SCREEN_OFF -> screenOffAt = now
                    Intent.ACTION_SCREEN_ON ->
                        if (screenOffAt > 0 && now - screenOffAt >= SLEEP_MS) wakeAt = now
                }
            }
        }, filter)
    }

    /** A central acabou de acordar de um tempo dormindo (consome o pedido). */
    fun takeWake(): Boolean {
        val w = wakeAt
        if (w == 0L) return false
        wakeAt = 0L
        return SystemClock.elapsedRealtime() - w <= WAKE_WINDOW_MS
    }
}

/**
 * Boas-vindas ao ligar o carro: o dragão aparece, depois o nome e a tagline
 * oficial, e tudo some. Tocar na tela pula a animação.
 */
@Composable
fun WelcomeOverlay(name: String, onDone: () -> Unit) {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.82f) }
    val textAlpha = remember { Animatable(0f) }
    val lineWidth = remember { Animatable(0f) }
    val all = remember { Animatable(1f) }
    val carX = remember { Animatable(-520f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(700)) }
        launch { carX.animateTo(0f, tween(1100, easing = FastOutSlowInEasing)) }
        logoScale.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        launch { lineWidth.animateTo(44f, tween(500, easing = FastOutSlowInEasing)) }
        textAlpha.animateTo(1f, tween(500))
        delay(1400)
        all.animateTo(0f, tween(450))
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .alpha(all.value)
            .background(Rotiv.Dark)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painterResource(R.drawable.logo_dragon),
                contentDescription = "RotivMedia",
                modifier = Modifier
                    .size(80.dp, 64.dp)
                    .alpha(logoAlpha.value)
                    .scale(logoScale.value),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(6.dp))
            MyCar(
                Modifier
                    .size(300.dp, 128.dp)
                    .offset(x = carX.value.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (name.isBlank()) "BEM-VINDO" else "BEM-VINDO, ${name.uppercase()}",
                modifier = Modifier.alpha(textAlpha.value),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.height(10.dp))
            Box(Modifier.width(lineWidth.value.dp).height(3.dp).background(Rotiv.Acc))
            Spacer(Modifier.height(10.dp))
            Text(
                "ROTIVMEDIA · POR ROTIVSECURITY",
                modifier = Modifier.alpha(textAlpha.value),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
                color = Rotiv.AccL,
            )
        }
    }
}
