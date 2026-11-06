package com.pedro.prisma.remote

import android.view.KeyEvent
import com.swordfish.libretrodroid.GLRetroView

/**
 * Ponte em-processo: o controle pelo celular (QR) manda os botões direto pro jogo em execução,
 * sem precisar de ADB. GameActivity registra o GLRetroView atual aqui.
 */
object InputBus {
    @Volatile var view: GLRetroView? = null

    fun down(code: Int) { view?.sendKeyEvent(KeyEvent.ACTION_DOWN, code) }
    fun up(code: Int) { view?.sendKeyEvent(KeyEvent.ACTION_UP, code) }
    fun active() = view != null
}
