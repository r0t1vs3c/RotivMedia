package com.rotivsecurity.launcher

import java.io.DataInputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Cliente ADB mínimo, só para a própria central (127.0.0.1).
 *
 * Nesta central o adbd fica aberto na rede sem pedir autorização (ro.adb.secure=0)
 * e já roda como root. O RotivMedia usa isso para o que um app comum não consegue:
 * fechar ou redimensionar a janela de outro app (ex.: o vídeo do YouTube encaixado).
 * Não abre nada novo: só fala com a porta que a central já deixa aberta.
 */
object LocalShell {
    private val PORTS = intArrayOf(9876, 5555)
    private const val A_CNXN = 0x4e584e43
    private const val A_OPEN = 0x4e45504f
    private const val A_OKAY = 0x59414b4f
    private const val A_CLSE = 0x45534c43
    private const val A_WRTE = 0x45545257
    private const val VERSION = 0x01000001
    private const val MAX_DATA = 256 * 1024

    private class Msg(val cmd: Int, val arg0: Int, val arg1: Int, val data: ByteArray)

    /** Roda um comando de shell e devolve a saída (null se o ADB local não responder). */
    fun exec(command: String, timeoutMs: Int = 4000): String? {
        for (port in PORTS) {
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress("127.0.0.1", port), 1500)
                    s.soTimeout = timeoutMs
                    val out = s.getOutputStream()
                    val inp = DataInputStream(s.getInputStream())
                    send(out, A_CNXN, VERSION, MAX_DATA, "host::\u0000".toByteArray())
                    // espera o CNXN do aparelho (ignora outros pacotes)
                    var m = read(inp)
                    while (m.cmd != A_CNXN) m = read(inp)
                    val localId = 1
                    send(out, A_OPEN, localId, 0, "shell:$command\u0000".toByteArray())
                    val sb = StringBuilder()
                    var remoteId = 0
                    while (true) {
                        m = read(inp)
                        when (m.cmd) {
                            A_OKAY -> remoteId = m.arg0
                            A_WRTE -> {
                                sb.append(String(m.data))
                                send(out, A_OKAY, localId, m.arg0, ByteArray(0))
                            }
                            A_CLSE -> {
                                if (remoteId != 0) send(out, A_CLSE, localId, remoteId, ByteArray(0))
                                return sb.toString()
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // tenta a próxima porta
            }
        }
        return null
    }

    private fun send(out: OutputStream, cmd: Int, arg0: Int, arg1: Int, data: ByteArray) {
        val b = ByteBuffer.allocate(24 + data.size).order(ByteOrder.LITTLE_ENDIAN)
        b.putInt(cmd).putInt(arg0).putInt(arg1).putInt(data.size)
        b.putInt(data.fold(0) { acc, x -> acc + (x.toInt() and 0xff) })
        b.putInt(cmd xor -1)
        b.put(data)
        out.write(b.array())
        out.flush()
    }

    private fun read(inp: DataInputStream): Msg {
        val h = ByteArray(24)
        inp.readFully(h)
        val b = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN)
        val cmd = b.int
        val a0 = b.int
        val a1 = b.int
        val len = b.int
        val data = ByteArray(len)
        if (len > 0) inp.readFully(data)
        return Msg(cmd, a0, a1, data)
    }

    /** Id da pilha (stack) de janelas onde está o app, pelo "am stack list". */
    fun stackOf(pkg: String): Int? {
        val list = exec("am stack list") ?: return null
        var current: Int? = null
        for (line in list.lines()) {
            Regex("""Stack id=(\d+)""").find(line)?.let { current = it.groupValues[1].toInt() }
            if (line.contains("$pkg/")) return current
        }
        return null
    }

    /** Id da tarefa (task) do app, para redimensionar. */
    fun taskOf(pkg: String): Int? {
        val list = exec("am stack list") ?: return null
        for (line in list.lines()) {
            if (line.contains("$pkg/")) {
                Regex("""taskId=(\d+)""").find(line)?.let { return it.groupValues[1].toInt() }
            }
        }
        return null
    }
}
