package com.rotivsecurity.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Mantém a "Depuração USB" ligada e o Tailscale conectado para o ADB remoto funcionar.
 *
 * A central desliga a depuração a cada boot. Com a porta TCP já gravada em
 * persist.adb.tcp.port, basta religar adb_enabled que o adbd sobe ouvindo na rede.
 * Precisa de WRITE_SECURE_SETTINGS, liberada uma vez pelo ADB:
 *   adb shell pm grant com.rotivsecurity.launcher android.permission.WRITE_SECURE_SETTINGS
 */
object RemoteAdb {
    /** 5555 = a nossa (persist.adb.tcp.port); 9876 = a que a central usa ao ligar. */
    private val PORTS = intArrayOf(5555, 9876)
    private var observing = false
    @Volatile private var lastToggle = 0L
    private val main = Handler(Looper.getMainLooper())

    fun canWrite(ctx: Context) =
        ctx.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun isOn(ctx: Context) =
        Settings.Global.getInt(ctx.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1

    /** O adbd está ouvindo na rede? (testa as portas conhecidas) */
    private fun portOpen(): Boolean = PORTS.any { port ->
        try {
            Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 1500); true }
        } catch (_: Exception) {
            false
        }
    }

    private fun putAdb(ctx: Context, v: Int) {
        try {
            Settings.Global.putInt(ctx.contentResolver, Settings.Global.ADB_ENABLED, v)
        } catch (_: Exception) {
        }
    }

    /**
     * Garante o ADB remoto (em segundo plano). Liga a depuração se estiver desligada;
     * se já estiver "ligada" mas a porta fechada (a central derruba o adbd no boot),
     * desliga e liga de novo para o adbd subir ouvindo na rede.
     */
    fun ensure(ctx: Context, force: Boolean = false) {
        if (!canWrite(ctx) || (!force && !Prefs.remoteAdb.value)) return
        val app = ctx.applicationContext
        Thread {
            synchronized(this) {
                // não reagir à própria troca (o observer dispara com o 0 → 1)
                if (!force && SystemClock.elapsedRealtime() - lastToggle < 20_000) return@Thread
                lastToggle = SystemClock.elapsedRealtime()
                if (!isOn(app)) {
                    putAdb(app, 1)
                } else if (force || !portOpen()) {
                    putAdb(app, 0)
                    Thread.sleep(1500)
                    putAdb(app, 1)
                }
            }
        }.start()
    }

    /** Observa a configuração: se a central desligar depois do boot, liga de novo. */
    fun watch(ctx: Context) {
        if (observing) return
        observing = true
        val app = ctx.applicationContext
        app.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ADB_ENABLED),
            false,
            object : ContentObserver(main) {
                override fun onChange(selfChange: Boolean) {
                    // pequena espera para não brigar com quem acabou de mudar
                    main.postDelayed({ ensure(app) }, 3000)
                }
            },
        )
        // a central pode desligar alguns segundos depois do boot: confere de novo
        for (delay in longArrayOf(0, 15_000, 45_000, 90_000)) main.postDelayed({ ensure(app) }, delay)
        // a central não abre o Tailscale sozinha ao ligar: pede para ele conectar
        for (delay in longArrayOf(2_000, 30_000)) main.postDelayed({ connectTailscale(app) }, delay)
    }

    /** Usa o comando de automação do próprio Tailscale (não faz nada se já estiver conectado). */
    fun connectTailscale(ctx: Context) {
        if (!Prefs.remoteAdb.value) return
        try {
            ctx.sendBroadcast(
                Intent("com.tailscale.ipn.CONNECT_VPN")
                    .setClassName("com.tailscale.ipn", "com.tailscale.ipn.IPNReceiver"),
            )
        } catch (_: Exception) {
        }
    }
}

/** Sobe o app logo após ligar a central, mesmo antes da tela inicial. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        // RotivApp.onCreate já rodou: Prefs carregado e o watch() ativo
        RemoteAdb.watch(ctx)
    }
}
