package com.pedro.prisma.ui

import android.content.Context
import com.swordfish.libretrodroid.ShaderConfig

/** Filtro de vídeo (shader) escolhido pelo usuário — aplicado em todos os jogos. */
object ShaderPrefs {
    const val SHARP = "sharp"    // pixel nítido (padrão)
    const val SMOOTH = "smooth"  // suavizado (bilinear)
    const val CRT = "crt"        // tubão (scanlines)
    const val LCD = "lcd"        // tela de portátil (grade)

    private const val PREFS = "prisma"
    private const val KEY = "shader"

    fun mode(c: Context): String =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SHARP) ?: SHARP

    fun setMode(c: Context, m: String) =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, m).apply()

    fun config(c: Context): ShaderConfig = when (mode(c)) {
        CRT -> ShaderConfig.CRT
        LCD -> ShaderConfig.LCD
        SMOOTH -> ShaderConfig.Default
        else -> ShaderConfig.Sharp
    }
}
