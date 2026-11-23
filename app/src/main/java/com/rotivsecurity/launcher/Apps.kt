package com.rotivsecurity.launcher

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.drawable.toBitmap

/** Pacotes dos apps usados pelo launcher. */
object Pkgs {
    const val MAPS = "com.google.android.apps.maps"
    const val YOUTUBE = "com.google.android.youtube"
    const val YT_MUSIC = "com.google.android.apps.youtube.music"
    const val RADARBOT = "com.vialsoft.radarbot_free"
    const val RETROARCH = "com.retroarch"
}

/** Links do YouTube Music abertos pelos botões de acesso rápido. */
object YtMusicLinks {
    const val MIX = "https://music.youtube.com/"
    const val CURTIDAS = "https://music.youtube.com/playlist?list=LM"
    const val BIBLIOTECA = "https://music.youtube.com/library"
}

data class AppEntry(
    val label: String,
    val pkg: String,
    val component: ComponentName,
    val icon: ImageBitmap,
)

object Apps {
    fun loadAll(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { ri ->
                val ai = ri.activityInfo
                AppEntry(
                    label = ri.loadLabel(pm).toString(),
                    pkg = ai.packageName,
                    component = ComponentName(ai.packageName, ai.name),
                    icon = ri.loadIcon(pm).toBitmap(96, 96).asImageBitmap(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun isInstalled(ctx: Context, pkg: String): Boolean =
        ctx.packageManager.getLaunchIntentForPackage(pkg) != null

    fun launchIntent(ctx: Context, pkg: String): Intent? =
        ctx.packageManager.getLaunchIntentForPackage(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun open(ctx: Context, pkg: String): Boolean {
        val i = launchIntent(ctx, pkg)
        if (i == null) {
            toast(ctx, "App não instalado")
            return false
        }
        ctx.startActivity(i)
        return true
    }

    fun open(ctx: Context, entry: AppEntry) {
        val i = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(entry.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        ctx.startActivity(i)
    }

    /** Abre um link direto no app (ex.: YouTube Music); se não der, só abre o app. */
    fun openUrlIn(ctx: Context, url: String, pkg: String) {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .setPackage(pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(i)
        } catch (e: ActivityNotFoundException) {
            open(ctx, pkg)
        }
    }

    /**
     * Tela dividida: abre [leftPkg] e, em seguida, [right] ao lado (FLAG_ACTIVITY_LAUNCH_ADJACENT).
     * Depende da central permitir multi-janela.
     */
    fun openSplit(ctx: Context, leftPkg: String, right: Intent?) {
        val left = launchIntent(ctx, leftPkg)
        if (left == null) {
            toast(ctx, "Google Maps não instalado")
            return
        }
        ctx.startActivity(left)
        if (right == null) return
        right.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        try {
            ctx.startActivity(right)
        } catch (e: Exception) {
            toast(ctx, "Não consegui abrir em tela dividida")
        }
    }

    /** Modo direção: Google Maps à esquerda + painel RotivMedia à direita. */
    fun openDriveMode(ctx: Context) {
        openBesideMap(ctx, Intent(ctx, PainelActivity::class.java))
    }

    /**
     * Abre o Maps à esquerda e [right] à direita. Com "Janelas 40/60" nos Ajustes,
     * usa janelas livres com o tamanho exato; senão, a tela dividida do sistema.
     */
    fun openBesideMap(ctx: Context, right: Intent?) {
        if (!Prefs.freeform.value) {
            openSplit(ctx, Pkgs.MAPS, right)
            return
        }
        val maps = launchIntent(ctx, Pkgs.MAPS)
        if (maps == null) {
            toast(ctx, "Google Maps não instalado")
            return
        }
        val dm = ctx.resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        val cut = (w * 0.4f).toInt()
        launchInBounds(ctx, maps, Rect(0, 0, cut, h))
        if (right != null) launchInBounds(ctx, right.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), Rect(cut, 0, w, h))
    }

    private fun launchInBounds(ctx: Context, i: Intent, bounds: Rect) {
        try {
            ctx.startActivity(i, ActivityOptions.makeBasic().setLaunchBounds(bounds).toBundle())
        } catch (e: Exception) {
            ctx.startActivity(i)
        }
    }

    fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}

/** Atalhos da grade da direita. O 6º bloco é sempre "Apps" (lista completa). */
data class Shortcut(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val pkgs: List<String>,
    val keywords: List<String>,
)

object Shortcuts {
    private const val PREFS = "atalhos"

    val list = listOf(
        Shortcut("maps", "Maps", Icons.Outlined.Map, listOf(Pkgs.MAPS), listOf("maps")),
        Shortcut("youtube", "YouTube", Icons.Outlined.SmartDisplay, listOf(Pkgs.YOUTUBE), listOf("youtube")),
        Shortcut("phone", "Telefone", Icons.Outlined.Phone, emptyList(), listOf("telefone", "phone", "bluetooth", "discador", "dialer")),
        Shortcut("camera", "Câmera", Icons.Outlined.PhotoCamera, emptyList(), listOf("câmera", "camera", "avm", "dvr")),
        Shortcut("carplay", "CarPlay", Icons.Outlined.DirectionsCar, emptyList(), listOf("carplay", "zlink", "autokit", "carlink", "android auto")),
    )

    /** Pacote do atalho: escolhido pelo usuário > pacote conhecido > nome parecido. */
    fun resolve(ctx: Context, s: Shortcut, apps: List<AppEntry>): String? {
        val saved = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(s.id, null)
        if (saved != null && Apps.isInstalled(ctx, saved)) return saved
        s.pkgs.firstOrNull { Apps.isInstalled(ctx, it) }?.let { return it }
        return apps.firstOrNull { a ->
            val l = a.label.lowercase()
            s.keywords.any { l.contains(it) }
        }?.pkg
    }

    fun assign(ctx: Context, id: String, pkg: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(id, pkg).apply()
    }
}
