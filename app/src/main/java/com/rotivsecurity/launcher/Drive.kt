package com.rotivsecurity.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.rotivsecurity.launcher.obd.Obd
import com.rotivsecurity.launcher.obd.ObdData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * Velocidade atual (OBD2 quando conectado, senão GPS), trava de vídeo do YouTube
 * (CONTRAN 242/2007: vídeo só com o carro parado) e aviso sonoro de velocidade.
 */
object Drive {
    /** Abaixo disso, por 3 leituras seguidas, o vídeo pausado pela trava volta a tocar. */
    const val RESUME_BELOW_KMH = 2

    private val _speedKmh = MutableStateFlow<Int?>(null)
    private val _guardPaused = MutableStateFlow(false)
    val speedKmh: StateFlow<Int?> = _speedKmh
    val guardPaused: StateFlow<Boolean> = _guardPaused

    private var slowReadings = 0
    private var lastAlert = 0L
    private var tone: ToneGenerator? = null

    /** Recebe uma leitura de velocidade (de qualquer fonte) e aplica trava e aviso. */
    @Synchronized
    fun onSpeed(ctx: Context, kmh: Int) {
        _speedKmh.value = kmh
        guard(ctx, kmh)
        speedAlert(kmh)
    }

    private fun guard(ctx: Context, kmh: Int) {
        MediaRepo.attach(ctx)
        val yt = MediaRepo.controller(Pkgs.YOUTUBE)
        // 0 = modo passageiro: nunca pausa
        if (Prefs.pauseAbove.value == 0) {
            _guardPaused.value = false
            slowReadings = 0
            return
        }
        if (kmh > Prefs.pauseAbove.value) {
            slowReadings = 0
            if (yt != null && MediaRepo.isPlaying(yt)) {
                yt.transportControls.pause()
                _guardPaused.value = true
            }
        } else if (kmh <= RESUME_BELOW_KMH && _guardPaused.value) {
            slowReadings++
            if (slowReadings >= 3) {
                yt?.transportControls?.play()
                _guardPaused.value = false
                slowReadings = 0
            }
        }
    }

    /** Bipe quando passa do limite escolhido nos Ajustes (repete a cada 8 s enquanto acima). */
    private fun speedAlert(kmh: Int) {
        val limit = Prefs.speedAlert.value
        if (limit <= 0 || kmh <= limit) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastAlert < 8_000) return
        lastAlert = now
        try {
            val tg = tone ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100).also { tone = it }
            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
        } catch (_: Exception) {
        }
    }

    fun release() {
        tone?.release()
        tone = null
    }
}

/** Serviço em primeiro plano: GPS + OBD2 + trava de vídeo, mesmo com outro app na frente. */
class DriveService : Service(), LocationListener {
    private var lm: LocationManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        startLocation()
        Obd.startIfConfigured(this)
        return START_STICKY
    }

    private fun startAsForeground() {
        val nm = NotificationManagerCompat.from(this)
        nm.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("Modo direção")
                .build()
        )
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("RotivMedia")
            .setContentText("Modo direção ativo")
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        ServiceCompat.startForeground(this, 1, n, type)
    }

    @SuppressLint("MissingPermission")
    private fun startLocation() {
        if (lm != null) return
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val m = getSystemService(LocationManager::class.java) ?: return
        try {
            m.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper())
            lm = m
        } catch (_: Exception) {
        }
    }

    override fun onLocationChanged(location: Location) {
        // Com o OBD2 conectado, a velocidade vem do carro (mais precisa).
        if (Obd.data.value.status == ObdData.Status.CONNECTED) return
        if (!location.hasSpeed()) return
        Drive.onSpeed(this, (location.speed * 3.6f).roundToInt())
    }

    override fun onDestroy() {
        lm?.removeUpdates(this)
        lm = null
        Obd.stop()
        Drive.release()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "modo_direcao"
    }
}
