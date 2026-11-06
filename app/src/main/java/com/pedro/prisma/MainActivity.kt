package com.pedro.prisma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import com.pedro.prisma.remote.MonkeyInput
import com.pedro.prisma.remote.RemoteServer
import com.pedro.prisma.ui.PrismaApp
import fi.iki.elonen.NanoHTTPD
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private var remote: RemoteServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // servidor do "controle pelo celular" (QR) + ponte de entrada rápida
        remote = RemoteServer(applicationContext)
        thread { runCatching { remote?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false) } }
        thread { MonkeyInput.warmup(applicationContext) }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0A0A12), surface = Color(0xFF18161F))) {
                val url by produceState<String?>(initialValue = null) {
                    value = runCatching { remote?.address() }.getOrNull()
                }
                PrismaApp(remoteUrl = url)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { remote?.stop() }
    }
}
