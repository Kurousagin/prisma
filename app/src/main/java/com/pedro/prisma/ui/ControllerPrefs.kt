package com.pedro.prisma.ui

import android.content.Context
import android.view.KeyEvent

/** Captura global de tecla pra tela de mapeamento. Quando onKey != null, a próxima tecla é "aprendida". */
object KeyCapture {
    @Volatile
    var onKey: ((Int) -> Unit)? = null
}

/**
 * Mapeamento do controle físico: ação do emulador → keycode físico do controle.
 * Resolve controles que mandam códigos fora do padrão (USB baratos) e troca de layout
 * (ex.: A/B invertidos no Switch). Vazio = usa o reconhecimento padrão.
 */
object ControllerPrefs {
    // rótulo visível → keycode RetroPad que o core espera
    val ACTIONS = listOf(
        "A" to KeyEvent.KEYCODE_BUTTON_A,
        "B" to KeyEvent.KEYCODE_BUTTON_B,
        "X" to KeyEvent.KEYCODE_BUTTON_X,
        "Y" to KeyEvent.KEYCODE_BUTTON_Y,
        "L" to KeyEvent.KEYCODE_BUTTON_L1,
        "R" to KeyEvent.KEYCODE_BUTTON_R1,
        "L2 / Z" to KeyEvent.KEYCODE_BUTTON_L2,
        "R2" to KeyEvent.KEYCODE_BUTTON_R2,
        "Start" to KeyEvent.KEYCODE_BUTTON_START,
        "Select" to KeyEvent.KEYCODE_BUTTON_SELECT,
        "Cima" to KeyEvent.KEYCODE_DPAD_UP,
        "Baixo" to KeyEvent.KEYCODE_DPAD_DOWN,
        "Esquerda" to KeyEvent.KEYCODE_DPAD_LEFT,
        "Direita" to KeyEvent.KEYCODE_DPAD_RIGHT,
    )

    private fun p(c: Context) = c.getSharedPreferences("prisma", Context.MODE_PRIVATE)
    private fun k(label: String) = "ctrl_$label"

    fun physicalFor(c: Context, label: String): Int? =
        p(c).getInt(k(label), -1).takeIf { it != -1 }

    fun set(c: Context, label: String, physical: Int) = p(c).edit().putInt(k(label), physical).apply()

    fun clearAll(c: Context) = p(c).edit().apply { ACTIONS.forEach { remove(k(it.first)) } }.apply()

    fun isMapped(c: Context) = ACTIONS.any { physicalFor(c, it.first) != null }

    /** keycode físico → keycode RetroPad, pro dispatch em jogo (primeiro mapeamento vence). */
    fun retroMap(c: Context): Map<Int, Int> {
        val m = HashMap<Int, Int>()
        for ((label, retro) in ACTIONS) physicalFor(c, label)?.let { if (!m.containsKey(it)) m[it] = retro }
        return m
    }
}
