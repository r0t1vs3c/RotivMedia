package com.rotivsecurity.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Serviço exigido pelo Android para ler a música tocando em outros apps. */
class MediaListener : NotificationListenerService() {
    override fun onListenerConnected() {
        MediaRepo.attach(applicationContext)
    }
}

data class Track(
    val pkg: String,
    val appLabel: String,
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val durationMs: Long,
    val positionMs: Long,
    val updatedAt: Long,
    val speed: Float,
    val playing: Boolean,
) {
    fun positionNow(): Long {
        if (!playing) return positionMs
        val p = positionMs + ((SystemClock.elapsedRealtime() - updatedAt) * speed).toLong()
        return if (durationMs > 0) p.coerceIn(0, durationMs) else p
    }
}

object MediaRepo {
    private val _current = MutableStateFlow<Track?>(null)
    private val _lastMusic = MutableStateFlow<Track?>(null)

    /** Sessão de mídia ativa agora (prioriza a que está tocando). */
    val current: StateFlow<Track?> = _current

    /** Última música (não YouTube) vista; usada na tela "parada". */
    val lastMusic: StateFlow<Track?> = _lastMusic

    private var msm: MediaSessionManager? = null
    private var component: ComponentName? = null
    private var appCtx: Context? = null
    private val controllers = mutableListOf<MediaController>()
    private val main = Handler(Looper.getMainLooper())

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() = refresh()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        setControllers(list ?: emptyList())
    }

    fun hasAccess(ctx: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    @Synchronized
    fun attach(ctx: Context) {
        if (msm != null) {
            refresh()
            return
        }
        if (!hasAccess(ctx)) return
        val c = ctx.applicationContext
        val m = c.getSystemService(MediaSessionManager::class.java) ?: return
        val comp = ComponentName(c, MediaListener::class.java)
        try {
            m.addOnActiveSessionsChangedListener(sessionsListener, comp, main)
            appCtx = c
            msm = m
            component = comp
            setControllers(m.getActiveSessions(comp))
        } catch (e: SecurityException) {
            msm = null
        }
    }

    fun refresh() {
        val m = msm ?: return
        val comp = component ?: return
        try {
            setControllers(m.getActiveSessions(comp))
        } catch (_: SecurityException) {
        }
    }

    private fun setControllers(list: List<MediaController>) {
        main.post {
            controllers.forEach { it.unregisterCallback(callback) }
            controllers.clear()
            list.forEach {
                it.registerCallback(callback, main)
                controllers.add(it)
            }
            publish()
        }
    }

    fun isPlaying(c: MediaController): Boolean {
        val s = c.playbackState?.state
        return s == PlaybackState.STATE_PLAYING || s == PlaybackState.STATE_BUFFERING
    }

    /**
     * Apps que abrem sessão de mídia sem ser player de música (ex.: o app Google / Assistente
     * deixa uma sessão vazia aberta). Só entram no player se estiverem tocando de verdade.
     */
    private val NOT_MUSIC = setOf(
        "com.google.android.googlequicksearchbox",
        "com.android.server.telecom",
    )

    /** Sessão que vale mostrar: tocando, ou parada mas de um player de verdade e sem erro. */
    private fun usable(c: MediaController): Boolean {
        if (isPlaying(c)) return true
        if (c.packageName in NOT_MUSIC) return false
        val s = c.playbackState?.state ?: return false
        return s != PlaybackState.STATE_NONE && s != PlaybackState.STATE_ERROR
    }

    private fun publish() {
        val list = controllers.filter { usable(it) }
        val chosen = list.firstOrNull { isPlaying(it) }
            ?: list.firstOrNull { it.packageName == Pkgs.YT_MUSIC || it.packageName == Pkgs.YOUTUBE }
            ?: list.firstOrNull()
        val t = chosen?.let { toTrack(it) }
        _current.value = t
        if (t != null && t.pkg != Pkgs.YOUTUBE && t.title.isNotBlank()) _lastMusic.value = t
    }

    private fun toTrack(c: MediaController): Track {
        val md = c.metadata
        val st = c.playbackState
        val art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        return Track(
            pkg = c.packageName,
            appLabel = appLabel(c.packageName),
            title = md?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
            art = art,
            durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            positionMs = st?.position ?: 0L,
            updatedAt = st?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = st?.playbackSpeed ?: 1f,
            playing = isPlaying(c),
        )
    }

    private fun appLabel(pkg: String): String {
        val ctx = appCtx ?: return pkg
        return try {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    fun controller(pkg: String): MediaController? = controllers.firstOrNull { it.packageName == pkg }

    /** Play/pausa. Sem sessão ativa: tenta retomar pelo botão de mídia e, se não der, abre o app. */
    fun playPause(ctx: Context, pkg: String) {
        val c = controller(pkg)
        if (c != null) {
            if (isPlaying(c)) c.transportControls.pause() else c.transportControls.play()
            return
        }
        sendMediaButton(ctx, pkg, KeyEvent.KEYCODE_MEDIA_PLAY)
        main.postDelayed({
            if (controller(pkg) == null) Apps.open(ctx, pkg)
        }, 1500)
    }

    fun next(pkg: String) {
        controller(pkg)?.transportControls?.skipToNext()
    }

    fun previous(pkg: String) {
        controller(pkg)?.transportControls?.skipToPrevious()
    }

    fun pause(pkg: String) {
        controller(pkg)?.transportControls?.pause()
    }

    fun play(pkg: String) {
        controller(pkg)?.transportControls?.play()
    }

    /** "Gostei" do YouTube Music, se o app expuser essa ação. */
    fun like(pkg: String): Boolean {
        val c = controller(pkg) ?: return false
        val action = c.playbackState?.customActions?.firstOrNull {
            val n = (it.name?.toString().orEmpty() + " " + it.action).lowercase()
            val positive = n.contains("like") || n.contains("thumb_up") || n.contains("thumbs_up") || n.contains("gostei")
            val negative = n.contains("dislike") || n.contains("thumb_down") || n.contains("não gostei")
            positive && !negative
        } ?: return false
        c.transportControls.sendCustomAction(action, null)
        return true
    }

    private fun sendMediaButton(ctx: Context, pkg: String, keyCode: Int) {
        for (action in intArrayOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            val i = Intent(Intent.ACTION_MEDIA_BUTTON)
                .setPackage(pkg)
                .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(action, keyCode))
            try {
                ctx.sendBroadcast(i)
            } catch (_: Exception) {
            }
        }
    }
}
