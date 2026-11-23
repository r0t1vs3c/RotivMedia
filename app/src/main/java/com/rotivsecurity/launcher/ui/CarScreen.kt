package com.rotivsecurity.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rotivsecurity.launcher.Prefs
import com.rotivsecurity.launcher.obd.Dtc
import com.rotivsecurity.launcher.obd.Obd
import com.rotivsecurity.launcher.obd.ObdData
import java.util.Locale

private val BR = Locale("pt", "BR")

private fun dec(v: Number?, digits: Int = 1): String =
    if (v == null) "--" else String.format(BR, "%.${digits}f", v.toDouble())

private fun int(v: Int?): String = v?.let { String.format(BR, "%,d", it) } ?: "--"

/** Tela "Meu carro": desenho/foto, OBD2 ao vivo, consumo da viagem e códigos de falha. */
@Composable
fun CarScreen(onClose: () -> Unit, onOpenSettings: () -> Unit) {
    val ctx = LocalContext.current
    val d by Obd.data.collectAsState()
    val model by Prefs.carModel.collectAsState()
    val price by Prefs.fuelPrice.collectAsState()
    val address by Prefs.obdAddress.collectAsState()
    val demo by Prefs.obdDemo.collectAsState()
    val active = d.status == ObdData.Status.CONNECTED || d.status == ObdData.Status.DEMO ||
        d.status == ObdData.Status.CONNECTING

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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("MEU CARRO", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                if (model.isNotBlank()) Text(model, fontSize = 13.sp, color = Rotiv.Muted)
                StatusPill(d)
            }
            Row(
                Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClose).padding(4.dp),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Fechar", modifier = Modifier.size(24.dp))
            }
        }
        Row(
            Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Esquerda: o carro
            Card(Modifier.weight(0.42f).fillMaxHeight()) {
                MyCar(Modifier.fillMaxWidth().weight(1f))
                Text(
                    when {
                        d.message.isNotBlank() -> d.message
                        address.isBlank() && !demo -> "Escolha o adaptador OBD2 nos Ajustes do carro."
                        else -> "Adaptador pronto. Toque em Conectar com a ignição ligada."
                    },
                    fontSize = 11.sp,
                    color = Rotiv.Muted,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (active) {
                        Button("Desconectar", Modifier.weight(1f)) { Obd.stop() }
                    } else {
                        Button("Conectar", Modifier.weight(1f), primary = true) { Obd.start(ctx) }
                    }
                    Button("Ajustes do carro", Modifier.weight(1f), onClick = onOpenSettings)
                }
            }
            // Direita: dados
            Column(
                Modifier.weight(0.58f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Gauge("Giro", int(d.rpm), "rpm", Modifier.weight(1f))
                    Gauge("Velocidade", d.speed?.toString() ?: "--", "km/h", Modifier.weight(1f))
                    Gauge("Motor", d.coolant?.toString() ?: "--", "°C", Modifier.weight(1f), alert = (d.coolant ?: 0) >= 105)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val v = d.voltage
                    Gauge("Bateria", dec(v), "V", Modifier.weight(1f), alert = v != null && (v < 12.0f || v > 15.0f))
                    Gauge("Carga", d.load?.toString() ?: "--", "%", Modifier.weight(1f))
                    Gauge("Admissão", d.intake?.toString() ?: "--", "°C", Modifier.weight(1f))
                }

                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Consumo da viagem")
                        SmallText("Zerar", Rotiv.AccL) { Obd.resetTrip() }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Metric("Média", dec(d.avgKml), "km/l", Modifier.weight(1f))
                        Metric("Agora", dec(d.instantKml), "km/l", Modifier.weight(1f))
                        Metric("Gasto", dec(d.tripLiters, 2), "litros", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Metric("Distância", dec(d.tripKm), "km", Modifier.weight(1f))
                        Metric("Tempo", formatDuration(d.tripSeconds), "", Modifier.weight(1f))
                        Metric(
                            "Custo",
                            if (price > 0f) "R$ " + dec(d.tripLiters * price, 2) else "--",
                            if (price > 0f) "" else "informe o preço",
                            Modifier.weight(1f),
                        )
                    }
                    Text(
                        "Estimativa pelo fluxo de ar do motor (ou pressão do coletor). Pode variar alguns por cento.",
                        fontSize = 9.sp,
                        color = Rotiv.Muted,
                    )
                }

                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Luz de injeção")
                        SmallText("Ler códigos", Rotiv.AccL) { Obd.requestDtc() }
                    }
                    when {
                        !d.dtcRead -> Text("Toque em \"Ler códigos\" com o OBD2 conectado.", fontSize = 11.sp, color = Rotiv.Muted)
                        d.dtcs.isEmpty() -> Text("Nenhum código de falha registrado.", fontSize = 12.sp, color = Rotiv.AccL)
                        else -> d.dtcs.forEach { code ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    code,
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Rotiv.Red.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Rotiv.RedL,
                                )
                                Text(Dtc.explain(code), fontSize = 11.sp)
                            }
                        }
                    }
                    if (d.dtcRead && d.dtcs.isNotEmpty()) {
                        Text(
                            "Os códigos são uma pista, não um diagnóstico. Leve a um mecânico de confiança.",
                            fontSize = 9.sp,
                            color = Rotiv.Muted,
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    return if (h > 0) "${h}h${"%02d".format(m)}" else "${m} min"
}

@Composable
private fun StatusPill(d: ObdData) {
    val (text, color) = when (d.status) {
        ObdData.Status.CONNECTED -> "OBD2 conectado" to Rotiv.AccL
        ObdData.Status.DEMO -> "Demonstração" to Rotiv.AccL
        ObdData.Status.CONNECTING -> "Conectando…" to Rotiv.AccL
        ObdData.Status.ERROR -> "Erro no OBD2" to Rotiv.RedL
        ObdData.Status.OFF -> "OBD2 desligado" to Rotiv.Muted
    }
    Text(
        text,
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (d.status == ObdData.Status.ERROR) Rotiv.Red.copy(alpha = 0.2f) else Rotiv.Chip)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        fontSize = 10.sp,
        color = color,
    )
}

@Composable
private fun Card(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.clip(shape).background(Rotiv.Dark2).border(1.dp, Rotiv.Border, shape).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(BR), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Rotiv.AccL)
}

@Composable
private fun Gauge(label: String, value: String, unit: String, modifier: Modifier, alert: Boolean = false) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .clip(shape)
            .background(Rotiv.Dark2)
            .border(1.dp, if (alert) Rotiv.Red else Rotiv.Border, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label.uppercase(BR), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Rotiv.AccL)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (alert) Rotiv.Red else Rotiv.Text)
            Text(" $unit", fontSize = 10.sp, color = Rotiv.Muted, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun Metric(label: String, value: String, unit: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, fontSize = 10.sp, color = Rotiv.Muted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (unit.isNotEmpty()) Text(" $unit", fontSize = 9.sp, color = Rotiv.Muted, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
private fun SmallText(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text,
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Rotiv.Chip)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        fontSize = 11.sp,
        color = color,
    )
}

@Composable
private fun Button(text: String, modifier: Modifier, primary: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (primary) Rotiv.Acc else Rotiv.Chip)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text, fontSize = 12.sp, color = if (primary) Rotiv.Text else Rotiv.AccL, maxLines = 1)
    }
}
