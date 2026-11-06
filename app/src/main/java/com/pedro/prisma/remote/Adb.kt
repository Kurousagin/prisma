package com.pedro.prisma.remote

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File

/** Ponte ADB local (127.0.0.1:5555): roda shell como usuário shell (INJECT_EVENTS, settings) sem root. */
object Adb {
    @Volatile private var conn: Dadb? = null
    private val lock = Any()

    fun shell(context: Context, cmd: String): String? = synchronized(lock) {
        return try {
            connect(context)
            conn!!.shell(cmd).output
        } catch (e: Exception) {
            conn = null
            null
        }
    }

    fun key(context: Context, keycode: String): Boolean =
        shell(context, "input keyevent $keycode") != null

    private fun connect(context: Context) {
        if (conn != null) return
        val priv = File(context.filesDir, "adbkey")
        val pub = File(context.filesDir, "adbkey.pub")
        if (!priv.exists() || !pub.exists()) AdbKeyPair.generate(priv, pub)
        conn = Dadb.create("127.0.0.1", 5555, AdbKeyPair.read(priv, pub))
    }
}
