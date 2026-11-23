package com.rotivsecurity.launcher

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rotivsecurity.launcher.ui.AppDrawer
import com.rotivsecurity.launcher.ui.CarScreen
import com.rotivsecurity.launcher.ui.RotivTheme
import com.rotivsecurity.launcher.ui.SettingsScreen
import com.rotivsecurity.launcher.ui.WelcomeOverlay
import com.rotivsecurity.launcher.ui.WelcomeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Janela de outro app encaixada num quadro da tela inicial, do mesmo jeito que o
 * launcher de fábrica faz com o mapa: uma janela livre (freeform) com o tamanho e a
 * posição exatos do quadro. A janela fica por cima da tela inicial; por isso Ajustes,
 * Apps e Meu carro abrem numa atividade própria (OverlayActivity), que cobre tudo.
 */
open class WindowDock(val pkg: String, private val tag: String) {
    /** Área do quadro na tela, em pixels. */
    @Volatile private var rect: Rect? = null
    /** A tela inicial apareceu (ou pediram) e a janela ainda não foi colocada. */
    @Volatile private var pending = false
    private var lastLaunch = 0L
    private var retryScheduled = false

    open fun enabled() = true

    /** Chamado no onStart da tela inicial (ela voltou a ficar visível). */
    fun requestDock(activity: Activity) {
        pending = true
        tryDock(activity)
    }

    /** O quadro foi medido (ou mudou de lugar). */
    fun updateRect(activity: Activity, r: Rect) {
        if (r.width() < 50 || r.height() < 50) return
        val old = rect
        val changed = r != old
        if (changed) android.util.Log.i("RotivDock", "$tag: quadro medido $r")
        rect = r
        if (changed && enabled()) {
            if (old != null && lastLaunch > 0L && !pending) {
                // a janela já está na tela: só ajusta o tamanho. Reabrir o app tiraria o
                // foco de quem está em uso (ex.: o RetroArch ainda carregando travava).
                val target = Rect(r)
                Thread { fixBounds(activity, target) }.start()
                return
            }
            pending = true
        }
        tryDock(activity)
    }

    fun tryDock(activity: Activity) {
        val r = rect ?: return
        if (!pending || DockState.blocked || !enabled()) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastLaunch < 1500) {
            // pedido logo depois de outro (ex.: o quadro mudou de tamanho): tenta de novo em seguida
            if (!retryScheduled) {
                retryScheduled = true
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    retryScheduled = false
                    tryDock(activity)
                }, 1500 - (now - lastLaunch) + 50)
            }
            return
        }
        val launch = activity.packageManager.getLaunchIntentForPackage(pkg) ?: return
        pending = false
        lastLaunch = now
        android.util.Log.i("RotivDock", "$tag em $r")
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opts = ActivityOptions.makeBasic().setLaunchBounds(Rect(r))
        try {
            // WINDOWING_MODE_FREEFORM = 5 (API escondida; o launcher de fábrica usa a mesma)
            ActivityOptions::class.java
                .getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                .invoke(opts, 5)
        } catch (_: Throwable) {
        }
        try {
            activity.startActivity(launch, opts.toBundle())
        } catch (_: Exception) {
        }
        // se a janela já existia com outro tamanho, o Android mantém o antigo: corrige
        val target = Rect(r)
        Thread {
            Thread.sleep(900)
            fixBounds(activity, target)
        }.start()
    }

    private var lastRecreate = 0L

    private fun fixBounds(activity: Activity, r: Rect) {
        val list = LocalShell.exec("am stack list") ?: return
        val lines = list.lines()
        val idx = lines.indexOfFirst { it.contains("$pkg/") }
        if (idx < 0) {
            // a janela não existe mais (o app foi fechado): abre de novo no quadro
            val now = SystemClock.elapsedRealtime()
            if (now - lastRecreate < 10_000) return
            lastRecreate = now
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                pending = true
                tryDock(activity)
            }
            return
        }
        val line = lines[idx]
        // O app já estava aberto em tela cheia (ex.: aberto pela lista de apps): o Android
        // não transforma essa janela em janela livre. Fecha e abre de novo dentro do quadro.
        val before = lines.subList(0, idx)
        val cfg = before.lastOrNull { it.contains("mWindowingMode=") }
        val stackId = before.lastOrNull { it.contains("Stack id=") }
            ?.let { Regex("""Stack id=(\d+)""").find(it)?.groupValues?.get(1) }
        if (cfg != null && cfg.contains("mWindowingMode=fullscreen") && stackId != null) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastRecreate < 10_000) return
            lastRecreate = now
            android.util.Log.i("RotivDock", "$tag estava em tela cheia: reabrindo no quadro")
            LocalShell.exec("am stack remove $stackId")
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                pending = true
                lastLaunch = 0L
                tryDock(activity)
            }, 700)
            return
        }
        val id = Regex("""taskId=(\d+)""").find(line)?.groupValues?.get(1) ?: return
        val b = Regex("""bounds=\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]""").find(line)?.groupValues
        val now = b?.let { Rect(it[1].toInt(), it[2].toInt(), it[3].toInt(), it[4].toInt()) }
        if (now != r) LocalShell.exec("am task resize $id ${r.left} ${r.top} ${r.right} ${r.bottom}")
    }

    /** Tira a janela da tela (remove a tarefa do app; o app continua instalado). */
    fun undock() {
        pending = false
        Thread {
            LocalShell.stackOf(pkg)?.let { LocalShell.exec("am stack remove $it") }
        }.start()
    }
}

object DockState {
    /** Boas-vindas na tela: espera terminar para não cobrir a animação. */
    @Volatile var blocked = false
}

/** Google Maps fixo no painel esquerdo. */
object MapDock : WindowDock(Pkgs.MAPS, "Maps") {
    override fun enabled() = Prefs.mapDock.value
}

/**
 * App encaixado no lado direito, ao lado do Maps (YouTube, RetroArch...).
 * Só um de cada vez: abrir um fecha o outro.
 */
open class SideDock(pkg: String, tag: String, val label: String) : WindowDock(pkg, tag) {
    private val _active = MutableStateFlow(false)
    /** A tela inicial está na variação deste app. */
    val active: StateFlow<Boolean> = _active

    override fun enabled() = _active.value && Prefs.mapDock.value

    /** Pede para fechar só depois de um segundo toque (jogo: evita perder o progresso sem querer). */
    open val confirmClose = false
    /** Mostra o botão tocar/pausar na barra de cima. */
    open val mediaControls = false

    fun open(ctx: Context) {
        SideDocks.all.filter { it !== this && it.active.value }.forEach { it.close() }
        // o YouTube Music do centro sai: senão a janela dele pode ficar por cima do vídeo/jogo
        // (e o vídeo ou jogo vai tomar o áudio de qualquer jeito). Volta quando este fechar.
        MusicDock.undock()
        _active.value = true
        if (ctx is HomeActivity) {
            requestDock(ctx)
        } else {
            // vindo da lista de apps (outra atividade): volta para a tela inicial, que encaixa no onStart
            ctx.startActivity(
                Intent(ctx, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    open fun close() {
        _active.value = false
        undock()
    }

    /** O app foi fechado por conta própria: só volta a tela ao normal. */
    fun forget() {
        _active.value = false
    }
}

/**
 * YouTube Music fixo no quadro do centro: dá para escolher e trocar música sem sair
 * da tela inicial. Fica de fora enquanto YouTube ou RetroArch ocupam o lado direito.
 */
object MusicDock : WindowDock(Pkgs.YT_MUSIC, "YouTube Music") {
    override fun enabled() =
        Prefs.musicDock.value && Prefs.mapDock.value && SideDocks.all.none { it.active.value }
}

/** YouTube encaixado no lado direito (variação do passageiro). */
object YtDock : SideDock(Pkgs.YOUTUBE, "YouTube", "YouTube") {
    override val mediaControls = true
    override fun close() {
        MediaRepo.pause(Pkgs.YOUTUBE)
        super.close()
    }
}

/** RetroArch (jogos) encaixado no lado direito. */
object RetroDock : SideDock(Pkgs.RETROARCH, "RetroArch", "RetroArch") {
    override val confirmClose = true
}

object SideDocks {
    val all: List<SideDock> get() = listOf(YtDock, RetroDock)
    fun forPkg(pkg: String?): SideDock? = all.firstOrNull { it.pkg == pkg }
}

/** Telas cheias (Ajustes, Apps, Meu carro) que precisam ficar por cima do Maps fixo. */
class OverlayActivity : ComponentActivity() {
    companion object {
        const val AJUSTES = "ajustes"
        const val APPS = "apps"
        const val CARRO = "carro"
        const val BOAS_VINDAS = "boas_vindas"

        /** Abre a tela; [shortcut] = id do atalho quando a lista de apps é para escolher o app dele. */
        fun open(ctx: Context, kind: String, shortcut: String? = null) {
            ctx.startActivity(
                Intent(ctx, OverlayActivity::class.java)
                    .putExtra("tipo", kind)
                    .putExtra("atalho", shortcut)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render(intent)
    }

    private fun render(i: Intent) {
        val kind = i.getStringExtra("tipo") ?: AJUSTES
        val shortcut = i.getStringExtra("atalho")
        setContent {
            RotivTheme {
                when (kind) {
                    BOAS_VINDAS -> {
                        val name by Prefs.name.collectAsState()
                        WelcomeOverlay(name) {
                            WelcomeState.markShown(this@OverlayActivity)
                            finish()
                        }
                    }
                    CARRO -> CarScreen(
                        onClose = { finish() },
                        onOpenSettings = { open(this, AJUSTES) },
                    )
                    APPS -> {
                        var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
                        LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { Apps.loadAll(this@OverlayActivity) } }
                        val title = if (shortcut == null) "Apps"
                        else "Escolha o app para ${Shortcuts.list.firstOrNull { it.id == shortcut }?.label ?: "o atalho"}"
                        AppDrawer(
                            apps = apps,
                            title = title,
                            onPick = { a ->
                                finish()
                                val dock = SideDocks.forPkg(a.pkg)
                                when {
                                    shortcut != null -> Shortcuts.assign(this, shortcut, a.pkg)
                                    dock != null && Prefs.mapDock.value -> dock.open(this)
                                    // YouTube Music fixo: volta para a tela inicial, onde ele fica no quadro do centro
                                    a.pkg == Pkgs.YT_MUSIC && Prefs.mapDock.value && Prefs.musicDock.value ->
                                        startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    else -> Apps.open(this, a)
                                }
                            },
                            onClose = { finish() },
                        )
                    }
                    else -> SettingsScreen(
                        onClose = { finish() },
                        onPreviewWelcome = {
                            WelcomeState.previewRequest.value = true
                            finish()
                        },
                    )
                }
            }
        }
    }
}
