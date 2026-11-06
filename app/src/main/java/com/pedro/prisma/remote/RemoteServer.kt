package com.pedro.prisma.remote

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.net.NetworkInterface

/** Servidor do controle pelo celular (igual Lumen): D-pad + teclado, injeção global via monkey/ADB local. */
class RemoteServer(private val context: Context, port: Int = 8080) : NanoHTTPD(port) {

    private val keys = mapOf(
        "UP" to "KEYCODE_DPAD_UP", "DOWN" to "KEYCODE_DPAD_DOWN",
        "LEFT" to "KEYCODE_DPAD_LEFT", "RIGHT" to "KEYCODE_DPAD_RIGHT",
        "OK" to "KEYCODE_DPAD_CENTER", "BACK" to "KEYCODE_BACK",
        "HOME" to "KEYCODE_HOME", "MENU" to "KEYCODE_MENU",
        "VUP" to "KEYCODE_VOLUME_UP", "VDOWN" to "KEYCODE_VOLUME_DOWN",
        "MUTE" to "KEYCODE_VOLUME_MUTE", "POWER" to "KEYCODE_POWER",
        "PLAY" to "KEYCODE_MEDIA_PLAY_PAUSE",
        "ENTER" to "KEYCODE_ENTER", "DEL" to "KEYCODE_DEL", "SPACE" to "KEYCODE_SPACE",
    )

    // botões do modo gamepad (segurar via down/up)
    private val gameKeys = mapOf(
        "UP" to "KEYCODE_DPAD_UP", "DOWN" to "KEYCODE_DPAD_DOWN",
        "LEFT" to "KEYCODE_DPAD_LEFT", "RIGHT" to "KEYCODE_DPAD_RIGHT",
        "A" to "KEYCODE_BUTTON_A", "B" to "KEYCODE_BUTTON_B",
        "X" to "KEYCODE_BUTTON_X", "Y" to "KEYCODE_BUTTON_Y",
        "L" to "KEYCODE_BUTTON_L1", "R" to "KEYCODE_BUTTON_R1",
        "START" to "KEYCODE_BUTTON_START", "SELECT" to "KEYCODE_BUTTON_SELECT",
    )

    override fun serve(session: IHTTPSession): Response = when (session.uri) {
        "/" -> html()
        "/key" -> {
            val code = keys[session.parameters["k"]?.firstOrNull()]
            if (code == null) text(Response.Status.BAD_REQUEST, "bad key")
            else if (MonkeyInput.press(context, code) || Adb.key(context, code)) text(Response.Status.OK, "ok")
            else text(Response.Status.INTERNAL_ERROR, "adb falhou")
        }
        "/game" -> {
            val code = gameKeys[session.parameters["k"]?.firstOrNull()]
            if (code == null) text(Response.Status.BAD_REQUEST, "bad key")
            else {
                val ok = when (session.parameters["a"]?.firstOrNull()) {
                    "down" -> MonkeyInput.keyDown(context, code)
                    "up" -> MonkeyInput.keyUp(context, code)
                    else -> MonkeyInput.press(context, code)
                }
                if (ok) text(Response.Status.OK, "ok") else text(Response.Status.INTERNAL_ERROR, "falhou")
            }
        }
        "/type" -> {
            val t = session.parameters["t"]?.firstOrNull()
            if (t.isNullOrEmpty()) text(Response.Status.BAD_REQUEST, "sem texto")
            else {
                val safe = t.replace("'", "").replace("\n", " ").replace(" ", "%s")
                if (Adb.shell(context, "input text '$safe'") != null) text(Response.Status.OK, "ok")
                else text(Response.Status.INTERNAL_ERROR, "falhou")
            }
        }
        else -> text(Response.Status.NOT_FOUND, "404")
    }

    private fun html(): Response {
        val page = context.assets.open("remote.html").bufferedReader().use { it.readText() }
        return newFixedLengthResponse(Response.Status.OK, "text/html", page)
    }

    private fun text(status: Response.Status, msg: String) =
        newFixedLengthResponse(status, "text/plain", msg)

    fun address(): String? {
        val ip = localIp() ?: return null
        return "http://$ip:$listeningPort"
    }

    private fun localIp(): String? {
        for (nif in NetworkInterface.getNetworkInterfaces()) {
            if (!nif.isUp || nif.isLoopback) continue
            for (addr in nif.inetAddresses) {
                val host = addr.hostAddress ?: continue
                if (!addr.isLoopbackAddress && !host.contains(':')) return host
            }
        }
        return null
    }
}
