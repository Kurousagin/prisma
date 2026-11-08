package com.pedro.prisma

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pedro.prisma.remote.MonkeyInput
import com.pedro.prisma.remote.RemoteServer
import com.pedro.prisma.ui.PrismaApp
import fi.iki.elonen.NanoHTTPD
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private var remote: RemoteServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // edge-to-edge com ícones claros (barras transparentes sobre fundo escuro).
        // Obrigatório com targetSdk 35: o Android 15 força edge-to-edge.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // servidor do "controle pelo celular" (QR) + ponte de entrada rápida
        remote = RemoteServer(applicationContext)
        thread { runCatching { remote?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false) } }
        thread { MonkeyInput.warmup(applicationContext) }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0A0A12), surface = Color(0xFF18161F))) {
                val url by produceState<String?>(initialValue = null) {
                    // tenta até o Wi-Fi/servidor estarem prontos (senão o QR vinha vazio)
                    repeat(20) {
                        val a = runCatching { remote?.address() }.getOrNull()
                        if (a != null) { value = a; return@produceState }
                        kotlinx.coroutines.delay(400)
                    }
                }
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    PrismaApp(remoteUrl = url)
                }
            }
        }
    }

    // Captura de tecla pra tela "Configurar controle": aprende qualquer botão de
    // controle (inclusive USB fora do padrão). Ignora teclas do sistema.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val cb = com.pedro.prisma.ui.KeyCapture.onKey
        if (cb != null && event.action == KeyEvent.ACTION_DOWN && !isSystemKey(event.keyCode)) {
            cb(event.keyCode)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun isSystemKey(k: Int) = k == KeyEvent.KEYCODE_BACK || k == KeyEvent.KEYCODE_HOME ||
        k == KeyEvent.KEYCODE_APP_SWITCH || k == KeyEvent.KEYCODE_VOLUME_UP ||
        k == KeyEvent.KEYCODE_VOLUME_DOWN || k == KeyEvent.KEYCODE_VOLUME_MUTE || k == KeyEvent.KEYCODE_POWER

    override fun onDestroy() {
        super.onDestroy()
        runCatching { remote?.stop() }
    }
}
