package com.pedro.prisma.remote

import android.view.KeyEvent
import com.swordfish.libretrodroid.GLRetroView

/**
 * Ponte em-processo: o controle pelo celular (QR) manda os botões direto pro jogo em execução,
 * sem precisar de ADB. GameActivity registra o GLRetroView atual aqui.
 */
object InputBus {
    @Volatile var view: GLRetroView? = null
    @Volatile var system: String = ""   // console do jogo aberto (pro controle QR se adaptar)

    fun down(code: Int) { view?.sendKeyEvent(KeyEvent.ACTION_DOWN, code) }
    fun up(code: Int) { view?.sendKeyEvent(KeyEvent.ACTION_UP, code) }
    /** Eixo analógico (N64): source = MOTION_SOURCE_ANALOG_LEFT/RIGHT; x,y em [-1,1]. */
    fun motion(source: Int, x: Float, y: Float) { view?.sendMotionEvent(source, x, y) }
    fun active() = view != null
}
