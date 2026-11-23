package com.rotivsecurity.launcher.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.rotivsecurity.launcher.CarPhoto
import com.rotivsecurity.launcher.obd.Obd
import java.util.Locale
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.rotivsecurity.launcher.MediaRepo
import com.rotivsecurity.launcher.Prefs
import com.rotivsecurity.launcher.RemoteAdb

/** Tela de Ajustes: personalização, direção, permissões e diagnóstico da central. */
@Composable
fun SettingsScreen(onClose: () -> Unit, onPreviewWelcome: () -> Unit) {
    val ctx = LocalContext.current
    val name by Prefs.name.collectAsState()
    val welcome by Prefs.welcome.collectAsState()
    val pause by Prefs.pauseAbove.collectAsState()
    val alert by Prefs.speedAlert.collectAsState()
    val freeform by Prefs.freeform.collectAsState()
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }

    Column(
        Modifier
            .fillMaxSize()
            .background(Rotiv.Dark)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("AJUSTES", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Box(Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClose).padding(4.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Fechar", modifier = Modifier.size(24.dp))
            }
        }
        Row(
            Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Coluna 1: personalização e direção
            Section(Modifier.weight(1f)) {
                Title("Personalização")
                Caption("Nome nas boas-vindas")
                TextInput(name, "Seu nome") { Prefs.setName(it.take(20)) }
                SwitchRow("Boas-vindas ao ligar", welcome) { Prefs.setWelcome(it) }
                ActionChip("Ver boas-vindas agora", onPreviewWelcome)

                Title("Direção")
                Caption("Pausar o YouTube acima de")
                Options(listOf(0, 3, 5, 10, 15), pause, { if (it == 0) "Nunca" else "$it km/h" }) {
                    Prefs.setPauseAbove(it)
                }
                if (pause == 0) Caption("Modo passageiro: o vídeo não pausa com o carro andando. A tela fica à vista do motorista.")
                Caption("Aviso sonoro de velocidade")
                Options(listOf(0, 60, 80, 100, 110, 120), alert, { if (it == 0) "Desl." else "$it" }) {
                    Prefs.setSpeedAlert(it)
                }
                Caption("Modo direção (botão Dirigir)")
                Options(listOf(false, true), freeform, { if (it) "Janelas 40/60" else "Tela dividida" }) {
                    Prefs.setFreeform(it)
                }
                Caption(
                    if (freeform) "Precisa de \"janelas de formato livre\" ativo nas opções do desenvolvedor."
                    else "Usa a tela dividida do sistema (em algumas centrais é fixa em 50/50)."
                )
            }
            // Coluna 2: meu carro e OBD2
            Section(Modifier.weight(1f)) {
                CarSettings()
            }
            // Coluna 3: permissões e diagnóstico
            Section(Modifier.weight(1f)) {
                Title("Permissões")
                key(tick) {
                    StatusRow("Tela inicial padrão", isDefaultHome(ctx), "Escolher") {
                        open(ctx, Intent(Settings.ACTION_HOME_SETTINGS))
                    }
                    StatusRow("Acesso à música", MediaRepo.hasAccess(ctx), "Liberar") {
                        open(ctx, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                    StatusRow("Localização (velocidade)", hasLocation(ctx), "Permitir") {
                        open(ctx, appDetails(ctx))
                    }
                    StatusRow("Janelas de formato livre", freeformEnabled(ctx), "Abrir") {
                        open(ctx, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    }
                }
                Title("Diagnóstico da central")
                InfoRow("Android declarado", Build.VERSION.RELEASE)
                InfoRow("Android real (API)", "${Build.VERSION.SDK_INT} · ${androidName(Build.VERSION.SDK_INT)}")
                InfoRow("Hardware", listOf(Build.HARDWARE, Build.BOARD).distinct().joinToString(" · "))
                InfoRow("Modelo", "${Build.MANUFACTURER} ${Build.MODEL}")
                InfoRow("Launcher", "RotivMedia ${appVersion(ctx)}")
                Title("Mapa")
                val mapDock by Prefs.mapDock.collectAsState()
                SwitchRow("Maps fixo no painel esquerdo", mapDock) { Prefs.setMapDock(it) }
                Caption("Abre o Google Maps numa janela no lugar do mapa desenhado, como no launcher de fábrica.")
                val musicDock by Prefs.musicDock.collectAsState()
                SwitchRow("YouTube Music fixo no centro", musicDock) {
                    Prefs.setMusicDock(it)
                    if (!it) com.rotivsecurity.launcher.MusicDock.undock()
                }
                Caption("Mostra o app do YouTube Music no quadro do centro, para escolher música sem sair da tela inicial.")
                Title("ADB remoto")
                val remoteAdb by Prefs.remoteAdb.collectAsState()
                SwitchRow("Religar depuração ao ligar", remoteAdb) {
                    Prefs.setRemoteAdb(it)
                    if (it) RemoteAdb.ensure(ctx)
                }
                key(tick) {
                    InfoRow("Permissão", if (RemoteAdb.canWrite(ctx)) "liberada" else "falta liberar pelo ADB")
                    InfoRow("Depuração agora", if (RemoteAdb.isOn(ctx)) "ligada" else "desligada")
                }
                ActionChip("Reiniciar ADB agora") { RemoteAdb.ensure(ctx, force = true) }
                Caption("Use só em redes suas: com o ADB aberto, quem estiver na mesma rede pode acessar a central.")
                Title("Atalhos do sistema")
                ActionChip("Opções de programador") { openFirst(ctx, devSettingsIntents()) }
                ActionChip("Definições do Android") {
                    openFirst(ctx, listOf(settingsComponent("Settings"), Intent(Settings.ACTION_SETTINGS)))
                }
                ActionChip("Acerca do dispositivo") {
                    openFirst(ctx, listOf(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS), settingsComponent("Settings\$MyDeviceInfoActivity")))
                }
                ActionChip("Idioma") {
                    openFirst(ctx, listOf(Intent(Settings.ACTION_LOCALE_SETTINGS), settingsComponent("Settings\$LanguageAndInputSettingsActivity")))
                }
                Caption("Abrem telas do Android que a central pode esconder.")
                Caption("Dica: segure um atalho da tela inicial para trocar o app dele.")
            }
        }
    }
}

/* ---------- Peças ---------- */

@Composable
private fun Section(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .fillMaxHeight()
            .clip(shape)
            .background(Rotiv.Dark2)
            .border(1.dp, Rotiv.Border, shape)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun Title(text: String) {
    Text(
        text.uppercase(),
        modifier = Modifier.padding(top = 4.dp),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = Rotiv.AccL,
    )
}

@Composable
private fun Caption(text: String) {
    Text(text, fontSize = 11.sp, color = Rotiv.Muted)
}

/* ---------- Meu carro + OBD2 ---------- */

@Composable
private fun CarSettings() {
    val ctx = LocalContext.current
    val model by Prefs.carModel.collectAsState()
    val body by Prefs.carBody.collectAsState()
    val color by Prefs.carColor.collectAsState()
    val usePhoto by Prefs.carPhoto.collectAsState()
    val fuel by Prefs.fuelType.collectAsState()
    val price by Prefs.fuelPrice.collectAsState()
    val cc by Prefs.engineCc.collectAsState()
    val obdAddress by Prefs.obdAddress.collectAsState()
    val demo by Prefs.obdDemo.collectAsState()
    var priceText by remember { mutableStateOf(if (price > 0f) String.format(Locale("pt", "BR"), "%.2f", price) else "") }
    var devices by remember { mutableStateOf<List<Pair<String, String>>?>(null) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && !CarPhoto.import(ctx, uri)) {
            Toast.makeText(ctx, "Não consegui abrir essa imagem", Toast.LENGTH_SHORT).show()
        }
    }
    val btPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        devices = if (ok) bondedDevices(ctx) else emptyList()
    }

    Title("Meu carro")
    MyCar(Modifier.fillMaxWidth().height(70.dp))
    TextInput(model, "Modelo (ex.: Civic G8)") { Prefs.setCarModel(it.take(24)) }
    Caption("Carroceria")
    Options(listOf(0, 1, 2, 3), body, { CarBodies[it] }) { Prefs.setCarBody(it) }
    Caption("Cor da lataria")
    CarColors.chunked(7).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { (label, argb) ->
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(2.dp, if (argb == color) Rotiv.Acc else Rotiv.Border, CircleShape)
                        .clickable { Prefs.setCarColor(argb) }
                        .semantics { contentDescription = label },
                )
            }
            repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    Caption("Cor livre")
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(color.toInt(), it) }
    Slider(
        value = hsv[0],
        onValueChange = { h ->
            val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, 0.75f, 0.8f)).toLong() and 0xFFFFFFFFL
            Prefs.setCarColor(argb)
        },
        valueRange = 0f..359f,
        colors = SliderDefaults.colors(thumbColor = Color(color), activeTrackColor = Rotiv.Acc, inactiveTrackColor = Rotiv.Chip),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionChip("Usar foto do meu carro") {
            try {
                pickPhoto.launch("image/*")
            } catch (e: Exception) {
                Toast.makeText(ctx, "Instale um app de arquivos ou galeria", Toast.LENGTH_SHORT).show()
            }
        }
        if (usePhoto) ActionChip("Usar desenho") { CarPhoto.remove(ctx) }
    }

    Title("Consumo")
    Options(listOf(0, 1), fuel, { if (it == 1) "Etanol" else "Gasolina" }) { Prefs.setFuelType(it) }
    Caption("Preço do litro (R$)")
    TextInput(priceText, "6,29", KeyboardType.Decimal) { t ->
        priceText = t.filter { it.isDigit() || it == ',' || it == '.' }.take(6)
        Prefs.setFuelPrice(priceText.replace(',', '.').toFloatOrNull() ?: 0f)
    }
    Caption("Motor (para carros sem sensor MAF)")
    Options(listOf(1000, 1400, 1600, 2000), cc, { "%.1f".format(it / 1000f) }) { Prefs.setEngineCc(it) }

    Title("Adaptador OBD2")
    Caption("ELM327 Bluetooth clássico. Pareie antes nas configurações de Bluetooth da central (senha 1234 ou 0000).")
    ActionChip(if (devices == null) "Buscar adaptadores pareados" else "Atualizar lista") {
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            btPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            devices = bondedDevices(ctx)
        }
    }
    devices?.let { list ->
        if (list.isEmpty()) Caption("Nenhum aparelho pareado encontrado.")
        list.forEach { (devName, addr) ->
            val on = addr == obdAddress
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) Rotiv.Acc else Rotiv.Chip)
                    .clickable {
                        Prefs.setObdDevice(addr, devName)
                        Prefs.setObdDemo(false)
                        Obd.start(ctx)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(devName, fontSize = 11.sp, color = if (on) Rotiv.Text else Rotiv.AccL, maxLines = 1)
            }
        }
    }
    if (obdAddress.isNotBlank()) Caption("Escolhido: ${Prefs.obdName.value}")
    SwitchRow("Modo demonstração", demo) {
        Prefs.setObdDemo(it)
        Obd.start(ctx)
    }
}

@SuppressLint("MissingPermission")
private fun bondedDevices(ctx: Context): List<Pair<String, String>> = try {
    ctx.getSystemService(BluetoothManager::class.java)?.adapter?.bondedDevices
        ?.map { (it.name ?: it.address) to it.address }
        ?.sortedBy { (n, _) -> if (n.contains("OBD", true) || n.contains("ELM", true) || n.contains("V-LINK", true)) 0 else 1 }
        ?: emptyList()
} catch (e: Exception) {
    emptyList()
}

@Composable
private fun TextInput(
    value: String,
    placeholder: String,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        textStyle = TextStyle(fontFamily = SpaceGrotesk, fontSize = 13.sp, color = Rotiv.Text),
        cursorBrush = SolidColor(Rotiv.Acc),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Rotiv.Dark)
            .border(1.dp, Rotiv.Border, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, fontSize = 13.sp, color = Rotiv.Muted)
            inner()
        },
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.sp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Rotiv.Acc,
                checkedThumbColor = Rotiv.Text,
                uncheckedTrackColor = Rotiv.Dark,
                uncheckedThumbColor = Rotiv.Muted,
                uncheckedBorderColor = Rotiv.Border,
            ),
        )
    }
}

@Composable
private fun <T> Options(values: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { v ->
            val on = v == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) Rotiv.Acc else Rotiv.Chip)
                    .clickable { onPick(v) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(v), fontSize = 11.sp, color = if (on) Rotiv.Text else Rotiv.AccL, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ActionChip(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Rotiv.Chip)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text, fontSize = 11.sp, color = Rotiv.AccL)
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp)
            Text(
                if (ok) "Ativo" else "Desativado",
                fontSize = 10.sp,
                color = if (ok) Rotiv.AccL else Rotiv.RedL,
            )
        }
        if (!ok) ActionChip(action, onClick)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 11.sp, color = Rotiv.Muted)
        Text(value, fontSize = 11.sp, maxLines = 1)
    }
}

/* ---------- Verificações ---------- */

private fun open(ctx: Context, i: Intent) {
    try {
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun settingsComponent(cls: String) =
    Intent().setClassName("com.android.settings", "com.android.settings.$cls")

/** Opções de programador: tenta a tela direta (mesmo escondida) antes da ação pública. */
private fun devSettingsIntents() = listOf(
    settingsComponent("Settings\$DevelopmentSettingsDashboardActivity"),
    settingsComponent("Settings\$DevelopmentSettingsActivity"),
    settingsComponent("DevelopmentSettings"),
    Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
)

/** Abre o primeiro intent que a central aceitar. */
private fun openFirst(ctx: Context, intents: List<Intent>) {
    for (i in intents) {
        try {
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) {
        }
    }
    Toast.makeText(ctx, "A central bloqueou essa tela", Toast.LENGTH_LONG).show()
}

private fun appDetails(ctx: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))

private fun hasLocation(ctx: Context) =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun isDefaultHome(ctx: Context): Boolean {
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val r = ctx.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)
    return r?.activityInfo?.packageName == ctx.packageName
}

fun freeformEnabled(ctx: Context): Boolean = try {
    Settings.Global.getInt(ctx.contentResolver, "enable_freeform_support", 0) == 1 ||
        Settings.Global.getInt(ctx.contentResolver, "force_resizable_activities", 0) == 1
} catch (e: Exception) {
    false
}

private fun appVersion(ctx: Context): String = try {
    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
} catch (e: Exception) {
    "?"
}

private fun androidName(api: Int): String = when (api) {
    26 -> "Android 8.0"
    27 -> "Android 8.1"
    28 -> "Android 9"
    29 -> "Android 10"
    30 -> "Android 11"
    31 -> "Android 12"
    32 -> "Android 12L"
    33 -> "Android 13"
    34 -> "Android 14"
    35 -> "Android 15"
    36 -> "Android 16"
    else -> "API $api"
}
