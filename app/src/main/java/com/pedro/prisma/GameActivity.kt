package com.pedro.prisma

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.pedro.prisma.ui.TouchGamepad
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import kotlinx.coroutines.launch
import java.io.File

/** Emulação embutida (LibretroDroid). Recebe core + rom por intent, controla por gamepad. */
class GameActivity : ComponentActivity() {

    private lateinit var retroView: GLRetroView
    private var stateFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val core = intent.getStringExtra("core") ?: return finish()
        val romUri = intent.getStringExtra("rom")?.let { android.net.Uri.parse(it) } ?: return finish()
        val key = intent.getStringExtra("key") ?: romUri.lastPathSegment ?: "game"
        stateFile = File(filesDir, "states").apply { mkdirs() }.let { File(it, "$key.state") }

        val data = GLRetroViewData(this).apply {
            coreFilePath = "${applicationInfo.nativeLibraryDir}/$core"
            if (romUri.scheme == "file") {
                gameFilePath = romUri.path
            } else {
                // ROM de pasta escolhida (SAF): carrega os bytes
                gameFileBytes = contentResolver.openInputStream(romUri)!!.use { it.readBytes() }
            }
            systemDirectory = filesDir.absolutePath
            savesDirectory = filesDir.absolutePath
        }
        retroView = GLRetroView(this, data)
        lifecycle.addObserver(retroView)
        val root = FrameLayout(this)
        root.addView(retroView)
        root.addView(ComposeView(this).apply { setContent { TouchGamepad(retroView) { finish() } } })
        setContentView(root)

        // continua de onde parou: restaura o estado quando o core terminar de carregar
        lifecycleScope.launch {
            retroView.getGLRetroEvents().collect { ev ->
                if (ev is GLRetroView.GLRetroEvents.SurfaceCreated) {
                    val f = stateFile
                    if (f != null && f.exists()) runCatching { retroView.unserializeState(f.readBytes()) }
                }
            }
        }
    }

    // salva o estado ao sair/minimizar (core ainda vivo no onPause)
    override fun onPause() {
        val f = stateFile
        if (f != null) runCatching { f.writeBytes(retroView.serializeState()) }
        super.onPause()
    }

    // Botões de jogo: manda pro emulador E consome (senão o Android trata B como Voltar,
    // DPAD navega/abre barra de status, etc.). BACK/volume seguem normais.
    private fun isGameKey(k: Int) = k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN ||
        k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT || k == KeyEvent.KEYCODE_DPAD_CENTER ||
        k == KeyEvent.KEYCODE_BUTTON_A || k == KeyEvent.KEYCODE_BUTTON_B ||
        k == KeyEvent.KEYCODE_BUTTON_X || k == KeyEvent.KEYCODE_BUTTON_Y ||
        k == KeyEvent.KEYCODE_BUTTON_L1 || k == KeyEvent.KEYCODE_BUTTON_R1 ||
        k == KeyEvent.KEYCODE_BUTTON_L2 || k == KeyEvent.KEYCODE_BUTTON_R2 ||
        k == KeyEvent.KEYCODE_BUTTON_START || k == KeyEvent.KEYCODE_BUTTON_SELECT ||
        k == KeyEvent.KEYCODE_BUTTON_THUMBL || k == KeyEvent.KEYCODE_BUTTON_THUMBR

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isGameKey(keyCode)) return super.onKeyDown(keyCode, event)
        retroView.sendKeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (!isGameKey(keyCode)) return super.onKeyUp(keyCode, event)
        retroView.sendKeyEvent(KeyEvent.ACTION_UP, keyCode)
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, event.getAxisValue(MotionEvent.AXIS_HAT_X), event.getAxisValue(MotionEvent.AXIS_HAT_Y))
        retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, event.getAxisValue(MotionEvent.AXIS_X), event.getAxisValue(MotionEvent.AXIS_Y))
        retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, event.getAxisValue(MotionEvent.AXIS_Z), event.getAxisValue(MotionEvent.AXIS_RZ))
        return super.onGenericMotionEvent(event)
    }
}
