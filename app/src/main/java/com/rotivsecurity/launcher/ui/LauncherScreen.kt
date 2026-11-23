package com.rotivsecurity.launcher.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.VerticalSplit
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import com.rotivsecurity.launcher.MapDock
import com.rotivsecurity.launcher.YtDock
import com.rotivsecurity.launcher.RetroDock
import com.rotivsecurity.launcher.MusicDock
import com.rotivsecurity.launcher.SideDock
import com.rotivsecurity.launcher.SideDocks
import com.rotivsecurity.launcher.LocalShell
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.outlined.SportsEsports
import com.rotivsecurity.launcher.DockState
import com.rotivsecurity.launcher.WindowDock
import com.rotivsecurity.launcher.OverlayActivity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.rotivsecurity.launcher.AppEntry
import com.rotivsecurity.launcher.Apps
import com.rotivsecurity.launcher.Drive
import com.rotivsecurity.launcher.MediaRepo
import com.rotivsecurity.launcher.Pkgs
import com.rotivsecurity.launcher.Prefs
import com.rotivsecurity.launcher.R
import com.rotivsecurity.launcher.Shortcut
import com.rotivsecurity.launcher.Shortcuts
import com.rotivsecurity.launcher.Track
import com.rotivsecurity.launcher.YtMusicLinks
import com.rotivsecurity.launcher.obd.Obd
import com.rotivsecurity.launcher.obd.ObdData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela do launcher.
 * full = true  → tela inicial: GPS (40%) + variação à direita.
 * full = false → só a variação, para ficar ao lado do Maps em tela dividida.
 */
@Composable
fun LauncherScreen(full: Boolean) {
    val ctx = LocalContext.current
    val current by MediaRepo.current.collectAsState()
    val last by MediaRepo.lastMusic.collectAsState()
    val speed by Drive.speedKmh.collectAsState()
    val guard by Drive.guardPaused.collectAsState()
    val name by Prefs.name.collectAsState()
    val pauseAbove by Prefs.pauseAbove.collectAsState()
    val alertLimit by Prefs.speedAlert.collectAsState()
    val overLimit = alertLimit > 0 && (speed ?: 0) > alertLimit
    var settingsOpen by remember { mutableStateOf(false) }
    var carOpen by remember { mutableStateOf(false) }
    val obd by Obd.data.collectAsState()
    var showWelcome by remember {
        // bloqueia o Maps já na primeira medida, antes dos efeitos rodarem
        mutableStateOf(full && Prefs.welcome.value && WelcomeState.pendingThisBoot(ctx)).also { DockState.blocked = it.value }
    }
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
    var access by remember { mutableStateOf(MediaRepo.hasAccess(ctx)) }
    // null = fechada; "" = lista de apps; id = escolher app para aquele atalho
    var drawer by remember { mutableStateOf<String?>(null) }
    val activity = ctx as? android.app.Activity
    val view = LocalView.current
    // a janela do app encaixado fica um pouco para dentro do quadro, mostrando a borda
    // arredondada igual à dos outros botões (raio 12 + borda 1 → ~4 de recuo)
    val dockInset = with(LocalDensity.current) { 4.dp.roundToPx() }
    val mapDock by Prefs.mapDock.collectAsState()
    val musicDock by Prefs.musicDock.collectAsState()
    val ytDocked by YtDock.active.collectAsState()
    val retroDocked by RetroDock.active.collectAsState()
    // app encaixado à direita (só na tela inicial com o Maps fixo)
    val side: SideDock? = when {
        !(full && mapDock) -> null
        ytDocked -> YtDock
        retroDocked -> RetroDock
        else -> null
    }
    fun openYouTube(pkg: String = Pkgs.YOUTUBE) {
        if (full && mapDock && activity != null) YtDock.open(activity)
        else Apps.openBesideMap(ctx, Apps.launchIntent(ctx, pkg))
    }
    /** Abre o app; YouTube e RetroArch ficam encaixados ao lado do Maps. */
    fun launch(pkg: String) {
        val dock = SideDocks.forPkg(pkg)
        when {
            pkg == Pkgs.YOUTUBE -> openYouTube(pkg)
            pkg == Pkgs.YT_MUSIC && full && mapDock && musicDock && activity != null -> MusicDock.requestDock(activity)
            dock != null && full && mapDock && activity != null -> dock.open(activity)
            else -> Apps.open(ctx, pkg)
        }
    }
    // o app encaixado foi fechado por dentro dele (ex.: "Sair" do RetroArch): volta ao normal
    LaunchedEffect(side) {
        val d = side
        if (d == null) {
            // nada encaixado à direita: o YouTube Music volta para o quadro do centro
            if (activity != null) MusicDock.requestDock(activity)
            return@LaunchedEffect
        }
        delay(8_000)
        while (true) {
            val list = withContext(Dispatchers.IO) { LocalShell.exec("am stack list") }
            if (list != null && !list.contains("${d.pkg}/")) {
                d.forget()
                break
            }
            delay(3_000)
        }
    }
    // Com o Maps fixo, as telas cheias abrem por cima dele numa atividade própria
    val overlayApart = full && mapDock
    fun openSettings() { if (overlayApart) OverlayActivity.open(ctx, OverlayActivity.AJUSTES) else settingsOpen = true }
    fun openCar() { if (overlayApart) OverlayActivity.open(ctx, OverlayActivity.CARRO) else carOpen = true }
    fun openDrawer(id: String) {
        if (overlayApart) OverlayActivity.open(ctx, OverlayActivity.APPS, id.ifEmpty { null }) else drawer = id
    }
    val previewWelcome by WelcomeState.previewRequest.collectAsState()
    LaunchedEffect(previewWelcome) {
        if (previewWelcome && full) {
            WelcomeState.previewRequest.value = false
            showWelcome = true
        }
    }
    // espera a animação de boas-vindas acabar antes de colocar o Maps
    LaunchedEffect(showWelcome) {
        DockState.blocked = showWelcome
        if (!showWelcome && activity != null) {
            MapDock.tryDock(activity)
            MusicDock.tryDock(activity)
            SideDocks.all.forEach { it.tryDock(activity) }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        access = MediaRepo.hasAccess(ctx)
        reload++
    }
    LaunchedEffect(reload) {
        apps = withContext(Dispatchers.IO) { Apps.loadAll(ctx) }
    }

    val yt = current?.takeIf { it.pkg == Pkgs.YOUTUBE }
    val music = current?.takeIf { it.pkg != Pkgs.YOUTUBE } ?: last
    val musicPkg = music?.pkg ?: Pkgs.YT_MUSIC

    // Na tela inicial, "voltar" não faz nada (é o launcher).
    BackHandler(enabled = full) {}

    Box(Modifier.fillMaxSize()) {
        if (full) {
            // marca RotivMedia atrás dos ícones da barra da central
            Image(
                painterResource(R.drawable.barra_central),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars),
                contentScale = ContentScale.FillBounds,
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .then(if (full) Modifier.statusBarsPadding() else Modifier)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!full) TopBar(
                access = access,
                speed = if (full) null else speed,
                overLimit = overLimit,
                onAccess = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                onSettings = { openSettings() },
                onCar = { openCar() },
                dock = side?.let { d ->
                    DockBar(
                        label = d.label,
                        title = if (d.mediaControls) yt?.title else null,
                        playing = if (d.mediaControls) yt?.playing == true else null,
                        confirmClose = d.confirmClose,
                        onPlayPause = { MediaRepo.playPause(ctx, d.pkg) },
                        onClose = { d.close() },
                    )
                },
            )
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val right = if (full) 0.6f else 1f
                if (full) {
                    Column(
                        Modifier.weight(0.4f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (mapDock) {
                            // o Maps fica por cima: aqui só o quadro liso, para a borda aparecer limpa
                            DockFrame(
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .dockArea(activity, view, MapDock, dockInset),
                                label = "Maps",
                            )
                        } else GpsPanel(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .dockArea(activity, view, MapDock, dockInset),
                            speed = if (obd.status == ObdData.Status.DEMO) obd.speed else speed,
                            overLimit = overLimit,
                            obd = obd,
                            onCar = { openCar() },
                            onDrive = { Apps.openDriveMode(ctx) },
                            onMapOnly = { Apps.open(ctx, Pkgs.MAPS) },
                            onRadar = { Apps.open(ctx, Pkgs.RADARBOT) },
                        )
                        BrandStrip(
                            access = access,
                            onAccess = {
                                ctx.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            },
                            onSettings = { openSettings() },
                            onApps = { openDrawer("") },
                            dock = side?.let { d ->
                                DockBar(
                                    label = d.label,
                                    title = null,
                                    playing = if (d.mediaControls) yt?.playing == true else null,
                                    confirmClose = d.confirmClose,
                                    onPlayPause = { MediaRepo.playPause(ctx, d.pkg) },
                                    onClose = { d.close() },
                                )
                            },
                        )
                    }
                }
                if (side != null) {
                    DockFrame(
                        Modifier
                            .weight(right)
                            .fillMaxHeight()
                            .dockArea(activity, view, side, dockInset),
                        label = side.label,
                    )
                } else if (yt != null) {
                    YouTubePanel(
                        Modifier.weight(right).fillMaxHeight(),
                        t = yt,
                        speed = speed,
                        guard = guard,
                        pauseAbove = pauseAbove,
                        full = full,
                        onSplit = { openYouTube() },
                        onOpen = { Apps.open(ctx, Pkgs.YOUTUBE) },
                        onPlayPause = { MediaRepo.playPause(ctx, Pkgs.YOUTUBE) },
                    )
                } else {
                    // centro + atalhos num bloco só: o quadro do Maps fica do mesmo tamanho
                    // em todas as variações (mudar 5 px fazia o Maps reabrir e roubar o foco)
                    Row(
                        Modifier.weight(right).fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (full && mapDock && musicDock) {
                            // YouTube Music de verdade, numa janela no quadro do centro
                            DockFrame(
                                Modifier
                                    .weight(0.57f)
                                    .fillMaxHeight()
                                    .dockArea(activity, view, MusicDock, dockInset),
                                label = "YouTube Music",
                            )
                        } else MusicPanel(
                            Modifier.weight(0.57f).fillMaxHeight(),
                            t = music,
                            onPlayPause = { MediaRepo.playPause(ctx, musicPkg) },
                            onPrev = { MediaRepo.previous(musicPkg) },
                            onNext = { MediaRepo.next(musicPkg) },
                            onLike = { if (!MediaRepo.like(musicPkg)) Apps.toast(ctx, "Curtir indisponível agora") },
                            // abre o app da música; se ele não abre sozinho (ex.: Bluetooth), vai pro YouTube Music
                            onOpenApp = {
                                Apps.open(ctx, if (Apps.launchIntent(ctx, musicPkg) != null) musicPkg else Pkgs.YT_MUSIC)
                            },
                            onLink = { url -> Apps.openUrlIn(ctx, url, Pkgs.YT_MUSIC) },
                        )
                        // dados do carro (OBD2) no lugar dos atalhos; tocar abre o "Meu carro"
                        CarDataPanel(
                            Modifier.weight(0.43f).fillMaxHeight(),
                            d = obd,
                            onOpen = { openCar() },
                        )
                    }
                }
            }
        }

        drawer?.let { mode ->
            BackHandler { drawer = null }
            val title = if (mode.isEmpty()) "Apps"
            else "Escolha o app para ${Shortcuts.list.firstOrNull { it.id == mode }?.label ?: "o atalho"}"
            AppDrawer(
                apps = apps,
                title = title,
                onPick = { a ->
                    if (mode.isEmpty()) {
                        if (SideDocks.forPkg(a.pkg) != null || a.pkg == Pkgs.YT_MUSIC) launch(a.pkg) else Apps.open(ctx, a)
                    } else {
                        Shortcuts.assign(ctx, mode, a.pkg)
                    }
                    drawer = null
                },
                onClose = { drawer = null },
            )
        }

        if (carOpen) {
            BackHandler { carOpen = false }
            CarScreen(
                onClose = { carOpen = false },
                onOpenSettings = {
                    carOpen = false
                    settingsOpen = true
                },
            )
        }

        if (settingsOpen) {
            BackHandler { settingsOpen = false }
            SettingsScreen(
                onClose = { settingsOpen = false },
                onPreviewWelcome = {
                    settingsOpen = false
                    showWelcome = true
                },
            )
        }

        if (showWelcome) {
            WelcomeOverlay(name) {
                WelcomeState.markShown(ctx)
                showWelcome = false
            }
        }
    }
}

/* ---------- Faixa RotivMedia (embaixo do Maps) ---------- */

/**
 * Substitui a barra de cima na tela inicial: a central já tem a barra dela (hora,
 * volume, Wi-Fi...), então aqui ficam só a marca e os botões do RotivMedia.
 * Com YouTube ou RetroArch encaixado, os controles dele aparecem aqui, sem cobrir o vídeo.
 */
@Composable
private fun BrandStrip(
    access: Boolean,
    onAccess: () -> Unit,
    onSettings: () -> Unit,
    onApps: () -> Unit,
    dock: DockBar?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Rotiv.Dark2)
            .border(1.dp, Rotiv.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (dock == null) {
            SmallButton("Apps", Icons.Outlined.Apps, fontSize = 12.sp, onClick = onApps)
            SmallButton("Ajustes", Icons.Outlined.Settings, fontSize = 12.sp, onClick = onSettings)
        } else {
            // com vídeo ou jogo aberto, só os ícones: o "Fechar" precisa caber inteiro
            IconTap(Icons.Outlined.Apps, "Apps", 20.dp, Rotiv.AccL, onApps)
            IconTap(Icons.Outlined.Settings, "Ajustes", 20.dp, Rotiv.AccL, onSettings)
        }
        Spacer(Modifier.weight(1f))
        if (dock != null) {
            DockControls(dock)
        } else if (!access) {
            Text(
                "Liberar música",
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Rotiv.Red.copy(alpha = 0.2f))
                    .clickable(onClick = onAccess)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 11.sp,
                color = Rotiv.RedL,
                maxLines = 1,
            )
        }
    }
}

/* ---------- Barra superior (só no painel da tela dividida) ---------- */

@Composable
private fun TopBar(
    access: Boolean,
    speed: Int?,
    overLimit: Boolean,
    onAccess: () -> Unit,
    onSettings: () -> Unit,
    onCar: () -> Unit,
    dock: DockBar? = null,
) {
    val time = rememberClock()
    val conn = rememberConnectivity()
    Box(Modifier.fillMaxWidth().height(30.dp).padding(horizontal = 4.dp)) {
        Row(
            Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(
                painterResource(R.drawable.logo_dragon),
                contentDescription = "RotivMedia",
                modifier = Modifier.size(24.dp, 19.dp),
                contentScale = ContentScale.Fit,
            )
            Text("ROTIVMEDIA", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        Text(time, Modifier.align(Alignment.Center), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(
            Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (dock != null) DockControls(dock)
            if (!access && dock == null) {
                Text(
                    "Liberar acesso à música",
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Rotiv.Red.copy(alpha = 0.2f))
                        .clickable(onClick = onAccess)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    color = Rotiv.RedL,
                )
            }
            Icon(
                if (conn.second) Icons.Outlined.Bluetooth else Icons.Outlined.BluetoothDisabled,
                contentDescription = "Bluetooth",
                modifier = Modifier.size(16.dp),
                tint = Rotiv.Muted,
            )
            if (speed != null) {
                Text(
                    "$speed km/h",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (overLimit) Rotiv.Red else Rotiv.Acc,
                )
            }
            Icon(
                if (conn.first) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
                contentDescription = "Wi-Fi",
                modifier = Modifier.size(16.dp),
                tint = Rotiv.Muted,
            )
            IconTap(Icons.Outlined.DirectionsCar, "Meu carro", 18.dp, Rotiv.Muted, onCar)
            IconTap(Icons.Outlined.Settings, "Ajustes", 18.dp, Rotiv.Muted, onSettings)
        }
    }
}

@Composable
private fun rememberClock(): String {
    val fmt = remember { SimpleDateFormat("HH:mm", Locale("pt", "BR")) }
    val time by produceState(fmt.format(Date())) {
        while (true) {
            value = fmt.format(Date())
            delay(5_000)
        }
    }
    return time
}

/** (Wi-Fi conectado, Bluetooth ligado) */
@Composable
private fun rememberConnectivity(): Pair<Boolean, Boolean> {
    val ctx = LocalContext.current
    val state by produceState(false to false) {
        while (true) {
            value = wifiOn(ctx) to bluetoothOn(ctx)
            delay(10_000)
        }
    }
    return state
}

private fun wifiOn(ctx: Context): Boolean = try {
    val cm = ctx.getSystemService(ConnectivityManager::class.java)
    cm?.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
} catch (e: Exception) {
    false
}

@SuppressLint("MissingPermission")
private fun bluetoothOn(ctx: Context): Boolean = try {
    ctx.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
} catch (e: Exception) {
    false
}

/* ---------- GPS (esquerda) ---------- */

@Composable
private fun GpsPanel(
    modifier: Modifier,
    speed: Int?,
    overLimit: Boolean,
    obd: ObdData,
    onCar: () -> Unit,
    onDrive: () -> Unit,
    onMapOnly: () -> Unit,
    onRadar: () -> Unit,
) {
    Panel(modifier.clickable(onClick = onDrive)) {
        MapSketch(Modifier.fillMaxSize())
        MyCar(
            Modifier
                .align(Alignment.Center)
                .padding(bottom = 36.dp)
                .size(200.dp, 84.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onCar),
        )
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Rotiv.AccD)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.Navigation, contentDescription = null, modifier = Modifier.size(22.dp))
            Column {
                Text("Google Maps", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Mapa offline + Radarbot", fontSize = 10.sp, color = Color(0xFFBFDBFE))
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Rotiv.Dark.copy(alpha = 0.92f))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    speed?.toString() ?: "--",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (overLimit) Rotiv.Red else Rotiv.Acc,
                    lineHeight = 28.sp,
                )
                Text(" km/h", fontSize = 11.sp, color = Rotiv.Muted, modifier = Modifier.padding(bottom = 3.dp))
            }
            if (obd.status == ObdData.Status.CONNECTED || obd.status == ObdData.Status.DEMO) {
                val br = Locale("pt", "BR")
                val parts = listOfNotNull(
                    obd.rpm?.let { String.format(br, "%,d rpm", it) },
                    obd.coolant?.let { "$it °C" },
                    obd.voltage?.let { String.format(br, "%.1f V", it) },
                    obd.instantKml?.let { String.format(br, "%.1f km/l", it) },
                )
                Text(
                    parts.joinToString("  ·  "),
                    Modifier.clickable(onClick = onCar),
                    fontSize = 11.sp,
                    color = Rotiv.AccL,
                    maxLines = 1,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallButton("Radar", Icons.Outlined.Radar, Modifier.weight(1f), onClick = onRadar)
                SmallButton("Só mapa", Icons.Outlined.Map, Modifier.weight(1f), onClick = onMapOnly)
                SmallButton("Dirigir", Icons.Outlined.VerticalSplit, Modifier.weight(1.2f), primary = true, onClick = onDrive)
            }
        }
    }
}

/** Mapa ilustrativo (o mapa de verdade é o Google Maps na tela dividida). */
@Composable
private fun MapSketch(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        fun p(x: Float, y: Float) = Offset(x * w, y * h)
        val road = w * 0.04f
        val street = w * 0.02f
        listOf(
            p(-0.04f, 0.233f) to p(1.04f, 0.133f),
            p(-0.04f, 0.633f) to p(1.04f, 0.767f),
            p(0.23f, -0.03f) to p(0.346f, 1.03f),
            p(0.77f, -0.03f) to p(0.654f, 1.03f),
        ).forEach { (a, b) -> drawLine(Rotiv.MapRoad, a, b, road) }
        listOf(
            p(-0.04f, 0.433f) to p(1.04f, 0.4f),
            p(0.5f, -0.03f) to p(0.52f, 1.03f),
            p(-0.04f, 0.867f) to p(1.04f, 0.933f),
        ).forEach { (a, b) -> drawLine(Rotiv.MapStreet, a, b, street) }
        val route = Path().apply {
            val pts = listOf(p(0.33f, 1f), p(0.308f, 0.667f), p(0.519f, 0.653f), p(0.508f, 0.417f), p(0.723f, 0.4f))
            moveTo(pts[0].x, pts[0].y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(route, Rotiv.Acc, style = Stroke(width = w * 0.023f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Rotiv.AccL, radius = w * 0.023f, center = p(0.723f, 0.4f))
    }
}

/* ---------- Variação 1: YouTube ---------- */

@Composable
private fun YouTubePanel(
    modifier: Modifier,
    t: Track,
    speed: Int?,
    guard: Boolean,
    pauseAbove: Int,
    full: Boolean,
    onSplit: () -> Unit,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
) {
    val art = remember(t.art) { t.art?.asImageBitmap() }
    Panel(modifier) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Label("YouTube · modo carona")
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1A33)),
                contentAlignment = Alignment.Center,
            ) {
                if (art != null) {
                    Image(art, null, Modifier.fillMaxSize().alpha(0.45f), contentScale = ContentScale.Crop)
                }
                Box(
                    Modifier
                        .size(58.dp, 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Rotiv.Red)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (t.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Tocar ou pausar",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column {
                Text(
                    t.title.ifBlank { "YouTube" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(t.artist.ifBlank { t.appLabel }, fontSize = 11.sp, color = Rotiv.Muted, maxLines = 1)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val msg = if (guard) "Pausado: carro em movimento (${speed ?: "--"} km/h)"
                else if (pauseAbove == 0) "Modo passageiro: não pausa andando"
                else "Pausa sozinho acima de $pauseAbove km/h"
                Text(
                    msg,
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (guard) Rotiv.Red.copy(alpha = 0.2f) else Rotiv.Chip)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    fontSize = 10.sp,
                    color = if (guard) Rotiv.RedL else Rotiv.AccL,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (full) {
                    SmallButton("Ao lado do mapa", Icons.Outlined.VerticalSplit, primary = true, onClick = onSplit)
                } else {
                    SmallButton("Abrir YouTube", Icons.Outlined.VerticalSplit, primary = true, onClick = onOpen)
                }
            }
        }
    }
}

/* ---------- Variação 2: YouTube Music ---------- */

@Composable
private fun MusicPanel(
    modifier: Modifier,
    t: Track?,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onLike: () -> Unit,
    onOpenApp: () -> Unit,
    onLink: (String) -> Unit,
) {
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(t) {
        while (true) {
            now = t?.positionNow() ?: 0L
            delay(500)
        }
    }
    val art = remember(t?.art) { t?.art?.asImageBitmap() }
    Panel(modifier) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Label(t?.appLabel ?: "YouTube Music")
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpenApp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(58.dp).clip(RoundedCornerShape(8.dp)).background(Rotiv.AccD),
                    contentAlignment = Alignment.Center,
                ) {
                    if (art != null) {
                        Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Outlined.MusicNote, null, Modifier.size(24.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        t?.title?.ifBlank { null } ?: "Nada tocando",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        t?.artist?.ifBlank { null } ?: "Toque para escolher",
                        fontSize = 11.sp,
                        color = Rotiv.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column {
                val duration = t?.durationMs ?: 0L
                ProgressBar(if (duration > 0) now.toFloat() / duration else 0f)
                Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(now), fontSize = 10.sp, color = Rotiv.Muted)
                    Text(formatTime(duration), fontSize = 10.sp, color = Rotiv.Muted)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTap(Icons.Outlined.ThumbUp, "Curtir", 18.dp, Rotiv.Muted, onLike)
                IconTap(Icons.Filled.SkipPrevious, "Anterior", 26.dp, Rotiv.Text, onPrev)
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(Rotiv.Acc).clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (t?.playing == true) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Tocar ou pausar",
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconTap(Icons.Filled.SkipNext, "Próxima", 26.dp, Rotiv.Text, onNext)
                IconTap(Icons.AutoMirrored.Outlined.QueueMusic, "Fila", 18.dp, Rotiv.Muted, onOpenApp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                QuickChip("Mix", Modifier.weight(1f)) { onLink(YtMusicLinks.MIX) }
                QuickChip("Curtidas", Modifier.weight(1f)) { onLink(YtMusicLinks.CURTIDAS) }
                QuickChip("Biblioteca", Modifier.weight(1f)) { onLink(YtMusicLinks.BIBLIOTECA) }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/* ---------- Dados do carro (direita) ---------- */

/**
 * Painel com os dados do OBD2 ao vivo, no lugar dos antigos atalhos.
 * Tocar em qualquer parte abre a tela "Meu carro" (detalhes, conectar, códigos de falha).
 */
@Composable
private fun CarDataPanel(modifier: Modifier, d: ObdData, onOpen: () -> Unit) {
    val br = Locale("pt", "BR")
    fun dec(v: Number?, digits: Int = 1) = if (v == null) "--" else String.format(br, "%.${digits}f", v.toDouble())
    val live = d.status == ObdData.Status.CONNECTED || d.status == ObdData.Status.DEMO
    val (status, statusColor) = when (d.status) {
        ObdData.Status.CONNECTED -> "Conectado" to Rotiv.AccL
        ObdData.Status.DEMO -> "Demonstração" to Rotiv.AccL
        ObdData.Status.CONNECTING -> "Conectando…" to Rotiv.AccL
        ObdData.Status.ERROR -> "Erro no OBD2" to Rotiv.RedL
        ObdData.Status.OFF -> "OBD2 desligado" to Rotiv.Muted
    }
    val coolantHot = (d.coolant ?: 0) >= 105
    val v = d.voltage
    val batteryBad = v != null && (v < 12.0f || v > 15.0f)

    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Rotiv.Dark2)
            .border(1.dp, Rotiv.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.DirectionsCar, null, Modifier.size(18.dp), tint = Rotiv.Acc)
            Column(Modifier.weight(1f)) {
                Label("Meu carro")
                Text(status, fontSize = 10.sp, color = statusColor, maxLines = 1)
            }
            if (d.dtcRead && d.dtcs.isNotEmpty()) {
                Text(
                    "${d.dtcs.size} falha" + if (d.dtcs.size > 1) "s" else "",
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Rotiv.Red.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Rotiv.RedL,
                    maxLines = 1,
                )
            }
        }
        val cells = listOf(
            listOf(
                CarCell("Giro", d.rpm?.let { String.format(br, "%,d", it) } ?: "--", "rpm"),
                CarCell("Motor", d.coolant?.toString() ?: "--", "°C", coolantHot),
            ),
            listOf(
                CarCell("Bateria", dec(v), "V", batteryBad),
                CarCell("Agora", dec(d.instantKml), "km/l"),
            ),
            listOf(
                CarCell("Média", dec(d.avgKml), "km/l"),
                CarCell("Viagem", dec(d.tripKm), "km"),
            ),
        )
        for (row in cells) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in row) CarValue(Modifier.weight(1f).fillMaxHeight(), c, dim = !live)
            }
        }
        if (!live) {
            Text(
                if (d.status == ObdData.Status.CONNECTING) "Lendo o OBD2…" else "Toque para conectar o OBD2",
                Modifier.fillMaxWidth(),
                fontSize = 10.sp,
                color = Rotiv.Muted,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private class CarCell(val label: String, val value: String, val unit: String, val alert: Boolean = false)

@Composable
private fun CarValue(modifier: Modifier, c: CarCell, dim: Boolean) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .clip(shape)
            .background(Rotiv.Dark)
            .border(1.dp, if (c.alert) Rotiv.Red else Rotiv.Border, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(c.label.uppercase(Locale("pt", "BR")), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Rotiv.AccL, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                c.value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    c.alert -> Rotiv.Red
                    dim -> Rotiv.Muted
                    else -> Rotiv.Text
                },
                maxLines = 1,
            )
            Text(" ${c.unit}", fontSize = 9.sp, color = Rotiv.Muted, maxLines = 1, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

/* ---------- Lista de apps ---------- */

@Composable
internal fun AppDrawer(
    apps: List<AppEntry>,
    title: String,
    onPick: (AppEntry) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Rotiv.Dark)
            // bloqueia toques na tela de baixo
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            IconTap(Icons.Outlined.Close, "Fechar", 26.dp, Rotiv.Text, onClose)
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(84.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(apps, key = { it.component.flattenToString() }) { a ->
                Column(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Rotiv.Dark2)
                        .clickable { onPick(a) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(a.icon, contentDescription = null, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(a.label, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/* ---------- Peças básicas ---------- */

@Composable
private fun Panel(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.clip(shape).background(Rotiv.Dark2).border(1.dp, Rotiv.Border, shape),
        content = content,
    )
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(Locale("pt", "BR")),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = Rotiv.AccL,
        maxLines = 1,
    )
}

@Composable
private fun ProgressBar(fraction: Float) {
    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Rotiv.Track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(Rotiv.Acc))
    }
}

@Composable
private fun IconTap(icon: ImageVector, description: String, size: Dp, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape).clickable(onClick = onClick).padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(size), tint = tint)
    }
}

@Composable
private fun SmallButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    fontSize: TextUnit = 10.sp,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (primary) Rotiv.Acc else Rotiv.Chip)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (primary) Rotiv.Text else Rotiv.AccL)
        Text(text, fontSize = fontSize, color = if (primary) Rotiv.Text else Rotiv.AccL, maxLines = 1)
    }
}

@Composable
private fun QuickChip(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Rotiv.Chip)
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 11.sp, color = Rotiv.AccL, maxLines = 1)
    }
}


/* ---------- Janelas encaixadas (Maps / YouTube) ---------- */

/** Mede o quadro na tela e entrega para o encaixe colocar a janela do app em cima dele. */
private fun Modifier.dockArea(
    activity: android.app.Activity?,
    view: android.view.View,
    dock: WindowDock,
    /** Recuo da janela para dentro do quadro: deixa a borda arredondada aparecer em volta. */
    inset: Int = 0,
): Modifier =
    onGloballyPositioned { c ->
        if (activity == null) return@onGloballyPositioned
        val b = c.boundsInWindow()
        // boundsInWindow já é relativo à janela; soma só a posição da janela na tela
        val onScreen = IntArray(2)
        val inWindow = IntArray(2)
        view.getLocationOnScreen(onScreen)
        view.getLocationInWindow(inWindow)
        val x = onScreen[0] - inWindow[0]
        val y = onScreen[1] - inWindow[1]
        dock.updateRect(
            activity,
            android.graphics.Rect(
                x + b.left.toInt() + inset,
                y + b.top.toInt() + inset,
                x + b.right.toInt() - inset,
                y + b.bottom.toInt() - inset,
            ),
        )
    }

/** Controles do app encaixado, mostrados na barra de cima (não tiram espaço da janela). */
private class DockBar(
    val label: String,
    val title: String?,
    /** null = sem botão tocar/pausar */
    val playing: Boolean?,
    val confirmClose: Boolean,
    val onPlayPause: () -> Unit,
    val onClose: () -> Unit,
)

@Composable
private fun DockControls(d: DockBar) {
    var armed by remember(d.label) { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(3_000)
            armed = false
        }
    }
    val title = d.title?.ifBlank { null }
    if (title != null) {
        Text(
            title,
            Modifier.widthIn(max = 220.dp),
            fontSize = 11.sp,
            color = Rotiv.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (d.playing != null) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Rotiv.Acc)
                .clickable(onClick = d.onPlayPause)
                .semantics { contentDescription = "Tocar ou pausar" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (d.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    Row(
        Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (armed) Rotiv.Red else Rotiv.Chip)
            .clickable {
                if (d.confirmClose && !armed) {
                    armed = true
                } else {
                    armed = false
                    d.onClose()
                }
            }
            .padding(horizontal = 12.dp)
            .semantics { contentDescription = "Fechar ${d.label}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.Outlined.Close,
            null,
            modifier = Modifier.size(16.dp),
            tint = if (armed) Color.White else Rotiv.Text,
        )
        Text(
            if (armed) "Toque de novo" else "Fechar ${d.label}",
            fontSize = 12.sp,
            color = if (armed) Color.White else Rotiv.Text,
            maxLines = 1,
        )
    }
}

/** Quadro do app encaixado: a janela dele fica por cima; isto só aparece enquanto abre. */
@Composable
private fun DockFrame(modifier: Modifier, label: String) {
    Panel(modifier) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                when (label) {
                    "YouTube" -> Icons.Outlined.SmartDisplay
                    "Maps" -> Icons.Outlined.Map
                    "YouTube Music" -> Icons.Outlined.MusicNote
                    else -> Icons.Outlined.SportsEsports
                },
                null,
                tint = Rotiv.Acc,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text("Abrindo o $label…", fontSize = 13.sp, color = Rotiv.Muted)
        }
    }
}
