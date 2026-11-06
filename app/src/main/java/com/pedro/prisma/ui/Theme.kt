package com.pedro.prisma.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Paleta do Prisma: escuro vibrante, roxo primário, cores quentes por console. */
object P {
    val bg = Color(0xFF0A0A12)
    val card = Color(0xFF18161F)
    val card2 = Color(0xFF201D2B)
    val ink = Color(0xFFF5F3FB)
    val dim = Color(0xFFA8A3C0)
    val faint = Color(0xFF6E6A88)
    val line = Color(0x14FFFFFF)
    val line2 = Color(0x24FFFFFF)
    val p1 = Color(0xFF7C4DFF)
    val p2 = Color(0xFFB36BFF)
    val good = Color(0xFF56E39F)
    val grad = Brush.linearGradient(listOf(p1, p2))
}

fun systemColor(sys: String): Color = when (sys) {
    "SNES", "Super Nintendo" -> Color(0xFF5B8DEF)
    "GBA", "Game Boy Advance" -> Color(0xFF6B5BFF)
    "NES" -> Color(0xFFE0485A)
    "Mega Drive" -> Color(0xFF2FA0DE)
    "Game Boy" -> Color(0xFF36B06A)
    "Nintendo 64" -> Color(0xFF36B06A)
    else -> Color(0xFF7C4DFF)
}

private fun darken(c: Color, f: Float) = Color(c.red * f, c.green * f, c.blue * f, 1f)

/** Gradiente de "boxart" quando não há capa real. */
fun coverGradient(sys: String): List<Color> {
    val c = systemColor(sys)
    return listOf(darken(c, 0.85f), darken(c, 0.28f))
}
