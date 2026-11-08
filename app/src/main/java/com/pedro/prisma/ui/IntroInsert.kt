package com.pedro.prisma.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlinx.coroutines.delay

/** Quando mostrar a animação de inserir o cartucho (escolhido pelo usuário no menu Apoiar). */
object IntroPrefs {
    const val ALWAYS = "always"
    const val ONCE = "once"
    const val OFF = "off"
    private const val PREFS = "prisma"
    private const val KEY_MODE = "intro_anim"
    private const val KEY_SEEN = "intro_seen"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun mode(c: Context): String = prefs(c).getString(KEY_MODE, ALWAYS) ?: ALWAYS
    fun setMode(c: Context, m: String) = prefs(c).edit().putString(KEY_MODE, m).apply()

    /** Decide e já marca o jogo como "visto" quando o modo é só-a-primeira-vez. */
    fun shouldShow(c: Context, key: String): Boolean = when (mode(c)) {
        OFF -> false
        ALWAYS -> true
        else -> {
            val p = prefs(c)
            val seen = p.getStringSet(KEY_SEEN, emptySet())!!.toMutableSet()
            if (seen.contains(key)) false
            else { seen.add(key); p.edit().putStringSet(KEY_SEEN, seen).apply(); true }
        }
    }
}

/* ---- cores ---- */
private val SLOT = Color(0xFF05050B)
private val LED_OFF = Color(0xFF3A3A44)
private fun gy(v: Int) = Color(v or (0xFF shl 24))

private fun bodyBrush(sys: String): Brush = when (sys) {
    "Game Boy" -> Brush.verticalGradient(listOf(gy(0xB9BBB0), gy(0x9A9C90)))
    "Game Boy Advance" -> Brush.verticalGradient(listOf(gy(0x44445A), gy(0x26262E)))
    "Mega Drive" -> Brush.verticalGradient(listOf(gy(0x30303A), gy(0x18181E)))
    "Nintendo 64" -> Brush.verticalGradient(listOf(gy(0x4A4A52), gy(0x2E2E36))) // cartucho cinza
    else -> Brush.verticalGradient(listOf(gy(0xDCD9D0), gy(0xBFBCB0))) // NES / SNES
}

private fun DrawScope.rr(brush: Brush, x: Float, y: Float, w: Float, h: Float, r: Float) =
    drawRoundRect(brush, Offset(x, y), Size(w, h), CornerRadius(r, r))

private fun DrawScope.rr(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float) =
    drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r, r))

/**
 * Animação de abertura: o cartucho do jogo cai e entra no console daquele sistema.
 * Dura ~1,5s; toque pula. Cobre a tela enquanto o emulador carrega por baixo.
 */
@Composable
fun IntroInsert(system: String, cover: ImageBitmap?, onDone: () -> Unit) {
    val drop = remember { Animatable(0f) }
    var done by remember { mutableStateOf(false) }
    val finish = { if (!done) { done = true; onDone() } }

    // geometria (dp) por sistema
    val handheld = system == "Game Boy" || system == "Game Boy Advance"
    val consoleW = when (system) {
        "SNES", "Super Nintendo" -> 216f; "Mega Drive" -> 224f
        "Game Boy" -> 150f; "Game Boy Advance" -> 246f; else -> 232f
    }
    val consoleH = if (system == "Game Boy") 190f else 122f
    val cartW = 108f; val cartH = 150f
    val stageH = 262f; val stageW = 300f
    val insert = if (handheld) 34f else 58f
    val startY = -(cartH + 44f)
    val endY = (stageH - consoleH) + insert - cartH
    val y = startY + (endY - startY) * drop.value
    val ledOn = drop.value > 0.82f

    LaunchedEffect(Unit) {
        drop.animateTo(1f, keyframes {
            durationMillis = 1550
            0f at 0; 1.05f at 1250; 1f at 1550
        })
        delay(750)
        finish()
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(gy(0x1C1636), gy(0x05050C))))
            .clickable(remember { MutableInteractionSource() }, indication = null) { finish() },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(stageW.dp, stageH.dp)) {
                // cartucho (atrás — desenhado primeiro)
                CartridgeArt(system, cover, Modifier.align(Alignment.TopCenter).offset(y = y.dp).size(cartW.dp, cartH.dp))
                // console (na frente — tampa a parte de baixo do cartucho)
                Canvas(Modifier.align(Alignment.BottomCenter).size(consoleW.dp, consoleH.dp)) {
                    drawConsole(system, ledOn)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Inserindo no ${consoleName(system)}", color = Color(0xFFA8A3C0), fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(5.dp))
            Text("toque para pular", color = Color(0xFF6E6A88), fontSize = 11.sp)
        }
    }
}

private fun consoleName(s: String) = when (s) {
    "SNES", "Super Nintendo" -> "Super Nintendo"
    "Game Boy Advance", "GBA" -> "Game Boy Advance"
    else -> s
}

@Composable
private fun CartridgeArt(system: String, cover: ImageBitmap?, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height; val r = w * 0.10f
        rr(bodyBrush(system), 0f, 0f, w, h, r)
        // faixa de cor no topo do cartucho de Mega Drive
        if (system == "Mega Drive") rr(systemColor(system), 0f, 0f, w, h * 0.06f, r)
        // ranhuras de pega
        val ridge = if (system == "Game Boy Advance" || system == "Mega Drive" || system == "Nintendo 64")
            Color.White.copy(alpha = .12f) else Color.Black.copy(alpha = .2f)
        for (i in 0..4) rr(ridge, w * (0.14f + i * 0.16f), h * 0.07f, w * 0.09f, h * 0.045f, 2f)
        // janela com a capa (ou gradiente do console)
        val wx = w * 0.11f; val wy = h * 0.23f; val ww = w * 0.78f; val wh = h * 0.68f
        clipRect(wx, wy, wx + ww, wy + wh) {
            if (cover != null) {
                drawImage(
                    cover, srcOffset = IntOffset.Zero, srcSize = IntSize(cover.width, cover.height),
                    dstOffset = IntOffset(wx.toInt(), wy.toInt()), dstSize = IntSize(ww.toInt(), wh.toInt()),
                )
            } else {
                rr(Brush.verticalGradient(coverGradient(system)), wx, wy, ww, wh, 4f)
            }
        }
        rr(Color.Black.copy(alpha = .28f), wx, wy, ww, wh, 4f) // moldura sutil (só borda via alpha baixo)
    }
}

/* ---- consoles (silhueta fiel, sem marca) ---- */
private fun DrawScope.drawConsole(system: String, ledOn: Boolean) {
    val w = size.width; val h = size.height
    val led = if (ledOn) Color(0xFF56E39F) else LED_OFF
    when (system) {
        "NES" -> {
            rr(bodyBrush(system), 0f, 0f, w, h, h * 0.08f)
            rr(Brush.verticalGradient(listOf(gy(0xE6E3DA), gy(0xD2CFC5))), 0f, 0f, w, h * 0.3f, h * 0.08f)
            rr(SLOT, w * 0.2f, h * 0.10f, w * 0.6f, h * 0.10f, h * 0.03f)
            rr(Brush.verticalGradient(listOf(gy(0xA3A094), gy(0x8B887D))), w * 0.18f, h * 0.50f, w * 0.64f, h * 0.34f, h * 0.05f)
            rr(Color.Black.copy(alpha = .25f), w * 0.26f, h * 0.65f, w * 0.48f, h * 0.025f, 2f)
            rr(Color(0xFFB23B2E), w * 0.05f, h * 0.885f, w * 0.9f, h * 0.045f, 2f)
            rr(Color(0xFFB23B2E).copy(alpha = .85f), w * 0.05f, h * 0.95f, w * 0.9f, h * 0.035f, 2f)
            drawCircle(led, h * 0.03f, Offset(w * 0.1f, h * 0.93f))
        }
        "SNES", "Super Nintendo" -> {
            rr(bodyBrush(system), 0f, 0f, w, h, h * 0.1f)
            rr(Brush.verticalGradient(listOf(gy(0xD8D4CB), gy(0xC4C1B6))), w * 0.2f, 0f, w * 0.6f, h * 0.34f, w * 0.3f)
            rr(SLOT, w * 0.27f, h * 0.06f, w * 0.46f, h * 0.1f, h * 0.03f)
            for (i in 0..2) rr(gy(0xA6A295), w * 0.1f, h * 0.5f + i * h * 0.1f, w * 0.24f, h * 0.05f, 2f)
            rr(Color(0xFF6B5BBF), w * 0.72f, h * 0.52f, w * 0.16f, h * 0.09f, 3f)
            rr(Color(0xFF8A7BD6), w * 0.72f, h * 0.68f, w * 0.16f, h * 0.09f, 3f)
            drawCircle(led, h * 0.03f, Offset(w * 0.12f, h * 0.84f))
        }
        "Mega Drive" -> {
            rr(bodyBrush(system), 0f, 0f, w, h, h * 0.09f)
            rr(Brush.verticalGradient(listOf(gy(0x3C3C46), gy(0x26262E))), 0f, 0f, w, h * 0.3f, h * 0.09f)
            rr(SLOT, w * 0.22f, h * 0.09f, w * 0.56f, h * 0.1f, h * 0.03f)
            drawCircle(gy(0x2E2E38), h * 0.15f, Offset(w * 0.18f, h * 0.68f))
            drawCircle(Color.White.copy(alpha = .08f), h * 0.15f, Offset(w * 0.18f, h * 0.68f))
            for (i in 0..3) rr(gy(0x3A3A44), w * 0.62f + i * w * 0.08f, h * 0.6f, w * 0.045f, h * 0.18f, 2f)
            drawCircle(led, h * 0.03f, Offset(w * 0.2f, h * 0.4f))
        }
        "Game Boy" -> {
            rr(bodyBrush(system), 0f, 0f, w, h, w * 0.08f)
            rr(SLOT, w * 0.3f, h * 0.02f, w * 0.4f, h * 0.045f, 3f)
            rr(gy(0x2C3A26), w * 0.16f, h * 0.12f, w * 0.68f, h * 0.32f, 5f)
            rr(Color(0xFF8AA23C), w * 0.22f, h * 0.17f, w * 0.56f, h * 0.22f, 3f)
            // dpad
            rr(gy(0x2A2A30), w * 0.18f, h * 0.62f, w * 0.18f, h * 0.06f, 2f)
            rr(gy(0x2A2A30), w * 0.24f, h * 0.56f, w * 0.06f, h * 0.18f, 2f)
            // A/B
            drawCircle(Color(0xFF8E2B4A), h * 0.035f, Offset(w * 0.72f, h * 0.66f))
            drawCircle(Color(0xFF8E2B4A), h * 0.035f, Offset(w * 0.82f, h * 0.60f))
            // start/select
            rr(gy(0x6F7266), w * 0.38f, h * 0.84f, w * 0.1f, h * 0.025f, 2f)
            rr(gy(0x6F7266), w * 0.54f, h * 0.84f, w * 0.1f, h * 0.025f, 2f)
            drawCircle(led, h * 0.012f, Offset(w * 0.08f, h * 0.17f))
        }
        "Game Boy Advance", "GBA" -> {
            rr(Brush.verticalGradient(listOf(gy(0x5546C4), gy(0x3A2F93))), 0f, 0f, w, h, h * 0.42f)
            rr(gy(0x4A3CAE), w * 0.06f, 0f, w * 0.2f, h * 0.12f, 6f)
            rr(gy(0x4A3CAE), w * 0.74f, 0f, w * 0.2f, h * 0.12f, 6f)
            rr(SLOT, w * 0.38f, h * 0.02f, w * 0.24f, h * 0.05f, 3f)
            rr(gy(0x201830), w * 0.33f, h * 0.22f, w * 0.34f, h * 0.5f, 4f)
            rr(Color(0xFF4A6A8A), w * 0.36f, h * 0.28f, w * 0.28f, h * 0.38f, 2f)
            // dpad
            rr(gy(0x241D4A), w * 0.1f, h * 0.42f, w * 0.14f, h * 0.1f, 2f)
            rr(gy(0x241D4A), w * 0.14f, h * 0.32f, w * 0.06f, h * 0.3f, 2f)
            // A/B
            drawCircle(gy(0x2A2155), h * 0.09f, Offset(w * 0.86f, h * 0.46f))
            drawCircle(gy(0x2A2155), h * 0.09f, Offset(w * 0.78f, h * 0.62f))
            drawCircle(led, h * 0.03f, Offset(w * 0.12f, h * 0.24f))
        }
        "Nintendo 64" -> {
            // corpo charcoal
            rr(bodyBrush(system), 0f, h * 0.04f, w, h * 0.96f, h * 0.16f)
            // tampa superior (expansion/cartucho) mais clara
            rr(Brush.verticalGradient(listOf(gy(0x45454F), gy(0x30303A))), w * 0.14f, 0f, w * 0.72f, h * 0.4f, h * 0.12f)
            // slot do cartucho no topo-centro
            rr(SLOT, w * 0.33f, h * 0.04f, w * 0.34f, h * 0.13f, h * 0.03f)
            // faixa frontal recuada com as 4 portas de controle
            rr(gy(0x1C1C22), w * 0.06f, h * 0.56f, w * 0.88f, h * 0.36f, h * 0.08f)
            for (i in 0..3) {
                val cx = w * (0.19f + i * 0.21f)
                drawCircle(gy(0x0C0C10), w * 0.05f, Offset(cx, h * 0.74f))
                drawCircle(gy(0x34343E), w * 0.028f, Offset(cx, h * 0.74f))
            }
            // reset (vermelho) + power ao lado do slot
            rr(Color(0xFFC23B2E), w * 0.44f, h * 0.44f, w * 0.055f, h * 0.06f, 2f)
            rr(gy(0x6F7266), w * 0.515f, h * 0.44f, w * 0.055f, h * 0.06f, 2f)
            drawCircle(led, h * 0.03f, Offset(w * 0.12f, h * 0.48f))
        }
    }
}
