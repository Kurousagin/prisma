package com.pedro.prisma

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asImageBitmap
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
    private var system: String = ""
    private var key: String = "game"
    private var loaded = false   // carrega o estado só uma vez (não reverte ao voltar pro app)
    private var ctrlMap: Map<Int, Int> = emptyMap()   // mapeamento do controle físico (se o usuário configurou)
    private var resumeAt = 0L                          // marca do onResume p/ somar tempo de jogo (conquista)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val core = intent.getStringExtra("core") ?: return finish()
        val romUri = intent.getStringExtra("rom")?.let { android.net.Uri.parse(it) } ?: return finish()
        key = intent.getStringExtra("key") ?: romUri.lastPathSegment ?: "game"
        stateFile = File(filesDir, "states").apply { mkdirs() }.let { File(it, "$key.state") }
        ctrlMap = com.pedro.prisma.ui.ControllerPrefs.retroMap(this)

        val data = GLRetroViewData(this).apply {
            coreFilePath = "${applicationInfo.nativeLibraryDir}/$core"
            if (romUri.scheme == "file") {
                gameFilePath = romUri.path
            } else {
                // SAF: grava num arquivo temp COM a extensão certa e passa o caminho.
                // Cores multi-sistema (genesis_plus_gx: MD/SMS/GG) precisam da extensão —
                // carregar só por bytes falha ("Cannot load game").
                val ext = (romUri.lastPathSegment ?: "").substringAfterLast('.', "rom")
                val tmp = File(cacheDir, "currentrom.$ext")
                contentResolver.openInputStream(romUri)!!.use { ins -> tmp.outputStream().use { ins.copyTo(it) } }
                gameFilePath = tmp.absolutePath
            }
            systemDirectory = filesDir.absolutePath
            savesDirectory = filesDir.absolutePath
            variables = com.pedro.prisma.ui.CoreTuning.variables(core)  // tuning por core (ex: idle opt no GBA)
            skipDuplicateFrames = true                                   // não re-renderiza quadro igual (poupa GPU/bateria)
            shader = com.pedro.prisma.ui.ShaderPrefs.config(this@GameActivity)  // filtro de vídeo escolhido
        }
        retroView = GLRetroView(this, data)
        lifecycle.addObserver(retroView)
        val root = FrameLayout(this)
        root.addView(retroView)
        system = intent.getStringExtra("system") ?: ""
        val gameTitle = intent.getStringExtra("title") ?: key
        root.addView(ComposeView(this).apply {
            setContent {
                com.pedro.prisma.ui.GameOverlay(
                    retroView, system, key, gameTitle,
                    onReloadMap = { ctrlMap = com.pedro.prisma.ui.ControllerPrefs.retroMap(this@GameActivity) },
                ) { finish() }
            }
        })
        setContentView(root)
        goFullscreen()                                      // tela cheia: esconde a barra de status/navegação
        com.pedro.prisma.remote.InputBus.view = retroView   // controle pelo celular (QR) manda direto pro jogo
        com.pedro.prisma.remote.InputBus.system = system

        // carrega o estado escolhido na tela de detalhe quando o core terminar de carregar (só uma vez)
        lifecycleScope.launch {
            retroView.getGLRetroEvents().collect { ev ->
                if (ev is GLRetroView.GLRetroEvents.SurfaceCreated && !loaded) {
                    loaded = true
                    when (val which = intent.getStringExtra("load") ?: "auto") {
                        "new" -> {}  // começa do zero
                        "auto" -> stateFile?.takeIf { it.exists() }?.let { runCatching { retroView.unserializeState(it.readBytes()) } }
                        else -> which.toIntOrNull()?.let { com.pedro.prisma.ui.StateSlots.load(retroView, this@GameActivity, key, it) }
                    }
                }
            }
        }

        // animação "inserindo o cartucho no console" (por cima, enquanto o jogo carrega)
        if (com.pedro.prisma.ui.IntroPrefs.shouldShow(this, key)) {
            val title = intent.getStringExtra("title") ?: key
            val sysObj = com.pedro.prisma.ui.Games.systems.find { it.label == system }
            val cover = sysObj?.let {
                com.pedro.prisma.ui.Covers.cached(this, com.pedro.prisma.ui.GameEntry(title, it, romUri))
                    ?.asImageBitmap()
            }
            val overlay = ComposeView(this)
            overlay.setContent { com.pedro.prisma.ui.IntroInsert(system, cover) { root.removeView(overlay) } }
            root.addView(overlay)
        }
    }

    override fun onResume() {
        super.onResume()
        resumeAt = android.os.SystemClock.elapsedRealtime()
        com.pedro.prisma.remote.InputBus.view = retroView
        com.pedro.prisma.remote.InputBus.system = system
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goFullscreen()   // reaplica se as barras reaparecerem
    }

    /** Tela cheia imersiva (esconde status/navegação; aparecem só ao deslizar da borda). */
    private fun goFullscreen() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // salva o estado ao sair/minimizar (core ainda vivo no onPause)
    override fun onPause() {
        com.pedro.prisma.remote.InputBus.view = null
        com.pedro.prisma.remote.InputBus.system = ""
        if (resumeAt > 0L) {
            com.pedro.prisma.ui.Achievements.addPlaytime(this, android.os.SystemClock.elapsedRealtime() - resumeAt)
            resumeAt = 0L
        }
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

    // Gamepad físico (BLE ou com fio): captura as teclas antes do Compose (que senão
    // comeria o D-pad p/ navegação de foco) e manda pro emulador. BACK/volume seguem normais.
    @SuppressLint("RestrictedApi")  // override legítimo; o super chamado é @RestrictedApi do androidx
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // tela de mapeamento (no menu de pausa) aguardando um botão: aprende e consome.
        // Captura aqui porque o gamepad físico já chega neste dispatchKeyEvent (um Compose
        // Dialog não recebe — é janela separada; por isso o mapeamento mora inline no overlay).
        val cap = com.pedro.prisma.ui.KeyCapture.onKey
        if (cap != null && event.action == KeyEvent.ACTION_DOWN && !isSystemKey(event.keyCode)) {
            cap(event.keyCode)
            return true
        }
        if (::retroView.isInitialized) {
            // mapeamento do usuário tem prioridade; senão, reconhecimento padrão
            val code = event.keyCode
            val out = ctrlMap[code] ?: if (isGameKey(code)) code else null
            if (out != null) {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> retroView.sendKeyEvent(KeyEvent.ACTION_DOWN, out)
                    KeyEvent.ACTION_UP -> retroView.sendKeyEvent(KeyEvent.ACTION_UP, out)
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun isSystemKey(k: Int) = k == KeyEvent.KEYCODE_BACK || k == KeyEvent.KEYCODE_HOME ||
        k == KeyEvent.KEYCODE_APP_SWITCH || k == KeyEvent.KEYCODE_VOLUME_UP ||
        k == KeyEvent.KEYCODE_VOLUME_DOWN || k == KeyEvent.KEYCODE_VOLUME_MUTE || k == KeyEvent.KEYCODE_POWER

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, event.getAxisValue(MotionEvent.AXIS_HAT_X), event.getAxisValue(MotionEvent.AXIS_HAT_Y))
        retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, event.getAxisValue(MotionEvent.AXIS_X), event.getAxisValue(MotionEvent.AXIS_Y))
        retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, event.getAxisValue(MotionEvent.AXIS_Z), event.getAxisValue(MotionEvent.AXIS_RZ))
        return super.onGenericMotionEvent(event)
    }
}
