package com.rotivsecurity.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.rotivsecurity.launcher.ui.LauncherScreen
import com.rotivsecurity.launcher.ui.RotivTheme
import com.rotivsecurity.launcher.ui.WelcomeState

/** Base comum: pede GPS/notificação e liga o modo direção. */
abstract class BaseLauncherActivity : ComponentActivity() {
    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { startDrive() }

    protected fun ensurePermissionsAndDrive() {
        val wanted = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = wanted.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) startDrive() else permissions.launch(missing.toTypedArray())
    }

    private fun startDrive() {
        val ok = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (ok) ContextCompat.startForegroundService(this, Intent(this, DriveService::class.java))
    }

    override fun onResume() {
        super.onResume()
        MediaRepo.attach(this)
    }
}

/** Tela inicial: GPS à esquerda + variação (YouTube ou YouTube Music) à direita. */
class HomeActivity : BaseLauncherActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A barra de cima da central é transparente: o que a tela inicial desenha atrás
        // dela aparece. Assim a faixa RotivMedia fica "dentro" da barra da central.
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        setContent { RotivTheme(edgeToEdge = true) { LauncherScreen(full = true) } }
        ensurePermissionsAndDrive()
    }

    override fun onStart() {
        super.onStart()
        MapDock.requestDock(this)
        MusicDock.requestDock(this)
        SideDocks.all.forEach { it.requestDock(this) }
    }

    override fun onResume() {
        super.onResume()
        // a central acordou depois de um tempo dormindo (carro religado): boas-vindas por cima de tudo
        if (WelcomeState.takeWake() && Prefs.welcome.value) OverlayActivity.open(this, OverlayActivity.BOAS_VINDAS)
    }
}

/** Só a parte da direita, para ficar ao lado do Google Maps em tela dividida. */
class PainelActivity : BaseLauncherActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RotivTheme { LauncherScreen(full = false) } }
        ensurePermissionsAndDrive()
    }
}
