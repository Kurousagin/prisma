package com.pedro.prisma.remote

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

/** Entrada rápida via `monkey --port`: 1 VM-servidor, teclas por socket (dezenas de ms vs ~1,5s). */
object MonkeyInput {
    private const val PORT = 1080
    private val lock = Any()
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    @Volatile private var serverThread: Thread? = null

    fun warmup(context: Context) = synchronized(lock) { runCatching { ensure(context) } }

    fun press(context: Context, keycode: String) = cmd(context, "press $keycode")

    // down/up separados: necessário pra SEGURAR direção/botão nos jogos
    fun keyDown(context: Context, keycode: String) = cmd(context, "key down $keycode")
    fun keyUp(context: Context, keycode: String) = cmd(context, "key up $keycode")

    private fun cmd(context: Context, line: String): Boolean = synchronized(lock) {
        return try {
            ensure(context)
            writer!!.write("$line\n"); writer!!.flush()
            reader!!.readLine()
            true
        } catch (e: Exception) {
            close()
            false
        }
    }

    private fun ensure(context: Context) {
        if (socket?.isClosed == false) return
        if (!tryConnect()) {
            startServer(context)
            repeat(25) { if (tryConnect()) return; Thread.sleep(160) }
            throw java.io.IOException("monkey não subiu")
        }
    }

    private fun tryConnect(): Boolean = try {
        val s = Socket().apply {
            connect(InetSocketAddress("127.0.0.1", PORT), 400)
            soTimeout = 1500
        }
        socket = s
        writer = s.getOutputStream().bufferedWriter()
        reader = s.getInputStream().bufferedReader()
        true
    } catch (e: Exception) {
        false
    }

    private fun startServer(context: Context) {
        if (serverThread?.isAlive == true) return
        serverThread = thread(isDaemon = true) {
            runCatching {
                val priv = File(context.filesDir, "adbkey")
                val pub = File(context.filesDir, "adbkey.pub")
                if (!priv.exists() || !pub.exists()) AdbKeyPair.generate(priv, pub)
                val d = Dadb.create("127.0.0.1", 5555, AdbKeyPair.read(priv, pub))
                d.shell("monkey --port $PORT")
            }
        }
    }

    private fun close() {
        runCatching { socket?.close() }
        socket = null; writer = null; reader = null
    }
}
