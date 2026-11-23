package com.rotivsecurity.launcher.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.SystemClock
import com.rotivsecurity.launcher.Drive
import com.rotivsecurity.launcher.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.sin

/** Tudo o que a tela "Meu carro" mostra. */
data class ObdData(
    val status: Status = Status.OFF,
    val message: String = "",
    val rpm: Int? = null,
    val speed: Int? = null,
    val coolant: Int? = null,
    val intake: Int? = null,
    val load: Int? = null,
    val voltage: Float? = null,
    val fuelLevel: Int? = null,
    /** Consumo instantâneo em litros por hora. */
    val fuelRateLh: Float? = null,
    /** Consumo instantâneo em km/l (só com o carro andando). */
    val instantKml: Float? = null,
    val tripKm: Double = 0.0,
    val tripLiters: Double = 0.0,
    val tripSeconds: Long = 0,
    val dtcs: List<String> = emptyList(),
    val dtcRead: Boolean = false,
) {
    enum class Status { OFF, CONNECTING, CONNECTED, DEMO, ERROR }

    val avgKml: Double? get() = if (tripLiters > 0.02) tripKm / tripLiters else null
}

/**
 * Leitor OBD2 por adaptador ELM327 Bluetooth clássico (SPP).
 * Lê os PIDs padrão do modo 01 e os códigos de falha do modo 03.
 * Só leitura: não apaga códigos nem envia comandos ao carro.
 */
object Obd {
    private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val _data = MutableStateFlow(ObdData())
    val data: StateFlow<ObdData> = _data

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var wantDtc = false
    private var hasMaf: Boolean? = null
    private var appCtx: Context? = null

    /** Liga o leitor se houver adaptador escolhido ou modo demonstração. */
    fun startIfConfigured(ctx: Context) {
        if (job?.isActive == true) return
        start(ctx)
    }

    /** (Re)inicia com os ajustes atuais. */
    fun start(ctx: Context) {
        stop()
        appCtx = ctx.applicationContext
        val demo = Prefs.obdDemo.value
        val address = Prefs.obdAddress.value
        if (!demo && address.isBlank()) return
        job = scope.launch {
            if (demo) runDemo() else runReal(address)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        closeSocket()
        _data.update { it.copy(status = ObdData.Status.OFF, message = "") }
    }

    fun requestDtc() {
        wantDtc = true
    }

    fun resetTrip() {
        _data.update { it.copy(tripKm = 0.0, tripLiters = 0.0, tripSeconds = 0) }
    }

    private fun closeSocket() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }

    private fun setStatus(s: ObdData.Status, msg: String) = _data.update { it.copy(status = s, message = msg) }

    /* ---------- Conexão real ---------- */

    private suspend fun runReal(address: String) {
        while (currentCoroutineContext().isActive) {
            try {
                setStatus(ObdData.Status.CONNECTING, "Conectando ao adaptador…")
                val s = openSocket(address)
                socket = s
                val elm = Elm(s.inputStream, s.outputStream)
                elm.cmd("ATZ", 3000)
                for (c in listOf("ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")) elm.cmd(c)
                // A primeira consulta faz o ELM327 procurar o protocolo do carro.
                val probe = elm.cmd("0100", 10_000)
                if (!probe.contains("4100")) throw IOException("Carro não respondeu (ignição ligada?)")
                hasMaf = null
                setStatus(ObdData.Status.CONNECTED, "Conectado")
                loop(elm)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setStatus(ObdData.Status.ERROR, e.message ?: "Falha na conexão")
                closeSocket()
                delay(5_000)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openSocket(address: String): BluetoothSocket {
        val ctx = appCtx ?: throw IOException("Sem contexto")
        val bt = ctx.getSystemService(BluetoothManager::class.java)?.adapter
            ?: throw IOException("Bluetooth indisponível")
        if (!bt.isEnabled) throw IOException("Bluetooth desligado")
        try {
            bt.cancelDiscovery()
        } catch (_: Exception) {
        }
        val dev = bt.getRemoteDevice(address)
        return try {
            dev.createRfcommSocketToServiceRecord(SPP).also { it.connect() }
        } catch (e: IOException) {
            dev.createInsecureRfcommSocketToServiceRecord(SPP).also { it.connect() }
        }
    }

    private suspend fun loop(elm: Elm) {
        var last = SystemClock.elapsedRealtime()
        var cycle = 0
        var iat = 25
        while (currentCoroutineContext().isActive) {
            val rpm = elm.pid("0C")?.let { (it[0] * 256 + it[1]) / 4 }
            val speed = elm.pid("0D")?.get(0)

            var maf: Float? = null
            var map: Int? = null
            if (hasMaf != false) {
                maf = elm.pid("10")?.let { (it[0] * 256 + it[1]) / 100f }
                if (hasMaf == null) hasMaf = maf != null
            }
            if (hasMaf == false) map = elm.pid("0B")?.get(0)

            if (cycle % 6 == 0) {
                val coolant = elm.pid("05")?.let { it[0] - 40 }
                val intake = elm.pid("0F")?.let { it[0] - 40 }
                val load = elm.pid("04")?.let { it[0] * 100 / 255 }
                val fuel = elm.pid("2F")?.let { it[0] * 100 / 255 }
                val volt = elm.cmd("ATRV").replace("V", "").toFloatOrNull()
                if (intake != null) iat = intake
                _data.update {
                    it.copy(coolant = coolant, intake = intake, load = load, fuelLevel = fuel, voltage = volt)
                }
            }
            cycle++

            val airGs = maf ?: speedDensity(rpm, map, iat)
            val now = SystemClock.elapsedRealtime()
            val dt = (now - last) / 1000.0
            last = now
            applyReading(rpm, speed, airGs, dt)
            speed?.let { appCtx?.let { c -> Drive.onSpeed(c, it) } }

            if (wantDtc) {
                wantDtc = false
                val codes = Dtc.parse(elm.cmd("03", 5000))
                _data.update { it.copy(dtcs = codes, dtcRead = true) }
            }
            delay(200)
        }
    }

    /** Massa de ar (g/s) estimada por pressão do coletor, para carros sem sensor MAF. */
    private fun speedDensity(rpm: Int?, mapKpa: Int?, iatC: Int): Float? {
        if (rpm == null || mapKpa == null) return null
        val displacementM3 = Prefs.engineCc.value / 1_000_000.0
        val volumetricEfficiency = 0.85
        val airPerSecondM3 = displacementM3 * rpm / 120.0 * volumetricEfficiency
        val moles = (mapKpa * 1000.0) * airPerSecondM3 / (8.314 * (iatC + 273.15))
        return (moles * 28.97).toFloat()
    }

    /** Converte ar em combustível e acumula a viagem. */
    private fun applyReading(rpm: Int?, speed: Int?, airGs: Float?, dt: Double) {
        // Gasolina brasileira (E27): ~13,2:1 e 750 g/L. Etanol hidratado: ~8,4:1 e 809 g/L.
        val (afr, density) = if (Prefs.fuelType.value == 1) 8.4f to 809f else 13.2f to 750f
        val lh = airGs?.let { it * 3600f / (afr * density) }
        val kml = if (lh != null && lh > 0.05f && speed != null && speed > 3) speed / lh else null
        val running = (speed ?: 0) > 0 || (rpm ?: 0) > 0
        if (running) secAcc += dt
        val whole = secAcc.toLong()
        secAcc -= whole
        _data.update {
            it.copy(
                rpm = rpm,
                speed = speed,
                fuelRateLh = lh,
                instantKml = kml,
                tripKm = it.tripKm + (speed ?: 0) * dt / 3600.0,
                tripLiters = it.tripLiters + (lh ?: 0f) * dt / 3600.0,
                tripSeconds = it.tripSeconds + whole,
            )
        }
    }

    private var secAcc = 0.0

    /* ---------- Demonstração (sem adaptador) ---------- */

    private suspend fun runDemo() {
        setStatus(ObdData.Status.DEMO, "Modo demonstração: dados simulados")
        var t = 0.0
        var last = SystemClock.elapsedRealtime()
        while (currentCoroutineContext().isActive) {
            t += 0.25
            val speed = max(0.0, 55 + 35 * sin(t / 9) + 6 * sin(t * 1.3)).toInt()
            val rpm = (850 + speed * 32 + 250 * sin(t * 0.9)).toInt()
            val coolant = (88 + 3 * sin(t / 20)).toInt()
            val now = SystemClock.elapsedRealtime()
            val dt = (now - last) / 1000.0
            last = now
            val airGs = (2.5 + rpm / 400.0 + speed / 12.0).toFloat()
            val (afr, density) = if (Prefs.fuelType.value == 1) 8.4f to 809f else 13.2f to 750f
            val lh = airGs * 3600f / (afr * density)
            secAcc += dt
            val whole = secAcc.toLong()
            secAcc -= whole
            _data.update {
                it.copy(
                    rpm = rpm,
                    speed = speed,
                    coolant = coolant,
                    intake = 31,
                    load = (25 + speed / 3).coerceAtMost(95),
                    voltage = (14.1 + 0.15 * sin(t)).toFloat(),
                    fuelLevel = 62,
                    fuelRateLh = lh,
                    instantKml = if (speed > 3) speed / lh else null,
                    tripKm = it.tripKm + speed * dt / 3600.0,
                    tripLiters = it.tripLiters + lh * dt / 3600.0,
                    tripSeconds = it.tripSeconds + whole,
                )
            }
            if (wantDtc) {
                wantDtc = false
                _data.update { it.copy(dtcs = listOf("P0171", "P0420"), dtcRead = true) }
            }
            delay(250)
        }
    }
}

/** Conversa com o ELM327: envia comando, lê até o prompt ">". */
private class Elm(private val inp: InputStream, private val out: OutputStream) {
    suspend fun cmd(c: String, timeoutMs: Long = 1500): String {
        while (inp.available() > 0) inp.read()
        out.write("$c\r".toByteArray())
        out.flush()
        val sb = StringBuilder()
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) {
            if (inp.available() > 0) {
                val ch = inp.read()
                if (ch < 0) throw IOException("Adaptador desconectou")
                if (ch.toChar() == '>') break
                sb.append(ch.toChar())
            } else {
                delay(8)
            }
        }
        return sb.toString()
            .replace("SEARCHING...", "")
            .replace("\r", "\n")
            .lines()
            .map { it.replace(" ", "").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    /** Consulta um PID do modo 01 e devolve os bytes de dados, ou null se o carro não suporta. */
    suspend fun pid(p: String): IntArray? {
        val resp = cmd("01$p")
        val line = resp.lines().firstOrNull { it.startsWith("41$p") } ?: return null
        val hex = line.substring(4)
        val n = hex.length / 2
        if (n == 0) return null
        val out = IntArray(n)
        for (i in 0 until n) {
            out[i] = hex.substring(i * 2, i * 2 + 2).toIntOrNull(16) ?: return null
        }
        return out
    }
}
