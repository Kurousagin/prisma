package com.pedro.prisma.ui

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.libretrodroid.GLRetroView
import kotlin.math.hypot
import kotlin.math.roundToInt

private val CREAM = Color(0xFFF1ECDE)

/** Controles touch adaptados a cada console. Multitouch real: cada botão trata seu próprio dedo. */
@Composable
fun TouchGamepad(retro: GLRetroView, system: String) {
    if (system == "Nintendo 64") { N64Pad(retro); return }
    val hasLR = system == "Game Boy Advance" || system == "Super Nintendo"
    val selLabel = if (system == "Mega Drive") "MODE" else "SELECT"
    Box(Modifier.fillMaxSize()) {
        if (hasLR) {
            Shoulder("L", KeyEvent.KEYCODE_BUTTON_L1, retro, Modifier.align(Alignment.TopStart).padding(14.dp, 10.dp))
            Shoulder("R", KeyEvent.KEYCODE_BUTTON_R1, retro, Modifier.align(Alignment.TopEnd).padding(14.dp, 10.dp))
        }

        // D-pad (com diagonais)
        DPad(retro, Modifier.align(Alignment.BottomStart).padding(18.dp, 0.dp, 0.dp, 22.dp), 150.dp)

        // botões de ação — layout por console
        Box(Modifier.align(Alignment.BottomEnd).padding(0.dp, 0.dp, 18.dp, 22.dp)) {
            when (system) {
                // SNES: losango colorido (cores do Super Famicom)
                "Super Nintendo" -> Box(Modifier.size(168.dp)) {
                    Hold("X", KeyEvent.KEYCODE_BUTTON_X, retro, Modifier.align(Alignment.TopCenter), 56.dp, Color(0xFF2F6AE0))
                    Hold("Y", KeyEvent.KEYCODE_BUTTON_Y, retro, Modifier.align(Alignment.CenterStart), 56.dp, Color(0xFF36B06A))
                    Hold("A", KeyEvent.KEYCODE_BUTTON_A, retro, Modifier.align(Alignment.CenterEnd), 56.dp, Color(0xFFE0485A))
                    Hold("B", KeyEvent.KEYCODE_BUTTON_B, retro, Modifier.align(Alignment.BottomCenter), 56.dp, Color(0xFFE8B84A), Color(0xFF3A2A00))
                }
                // Mega Drive: A=RetroPad Y, B=B, C=A (mapeamento do genesis_plus_gx)
                "Mega Drive" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Hold("A", KeyEvent.KEYCODE_BUTTON_Y, retro, Modifier, 58.dp, Color(0xFF5A5A66))
                    Hold("B", KeyEvent.KEYCODE_BUTTON_B, retro, Modifier, 58.dp, Color(0xFF5A5A66))
                    Hold("C", KeyEvent.KEYCODE_BUTTON_A, retro, Modifier, 58.dp, Color(0xFF5A5A66))
                }
                // NES: A/B vermelhos
                "NES" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Hold("B", KeyEvent.KEYCODE_BUTTON_B, retro, Modifier, 62.dp, Color(0xFFC23B2E))
                    Hold("A", KeyEvent.KEYCODE_BUTTON_A, retro, Modifier, 62.dp, Color(0xFFC23B2E))
                }
                // Game Boy: A/B vinho
                "Game Boy" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Hold("B", KeyEvent.KEYCODE_BUTTON_B, retro, Modifier, 62.dp, Color(0xFF9E3357))
                    Hold("A", KeyEvent.KEYCODE_BUTTON_A, retro, Modifier, 62.dp, Color(0xFF9E3357))
                }
                // GBA: A/B roxos
                else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Hold("B", KeyEvent.KEYCODE_BUTTON_B, retro, Modifier, 62.dp, Color(0xFF6B5BFF))
                    Hold("A", KeyEvent.KEYCODE_BUTTON_A, retro, Modifier, 62.dp, Color(0xFF6B5BFF))
                }
            }
        }

        // start / select (ou start / mode no Mega Drive)
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Pill(selLabel, KeyEvent.KEYCODE_BUTTON_SELECT, retro)
            Pill("START", KeyEvent.KEYCODE_BUTTON_START, retro)
        }
    }
}

@Composable
private fun Hold(
    label: String, keyCode: Int, retro: GLRetroView, modifier: Modifier, d: Dp,
    fill: Color = CREAM, fg: Color = Color.White,
) {
    var down by remember { mutableStateOf(false) }
    Box(
        modifier.size(d).clip(CircleShape)
            .background(fill.copy(alpha = if (down) 1f else 0.6f))
            .border(1.dp, Color.White.copy(alpha = .35f), CircleShape)
            .pointerInput(keyCode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    down = true; retro.sendKeyEvent(KeyEvent.ACTION_DOWN, keyCode)
                    do { val e = awaitPointerEvent() } while (e.changes.any { it.pressed })
                    down = false; retro.sendKeyEvent(KeyEvent.ACTION_UP, keyCode)
                }
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
}

@Composable
private fun Shoulder(label: String, keyCode: Int, retro: GLRetroView, modifier: Modifier) {
    var down by remember { mutableStateOf(false) }
    Box(
        modifier.size(78.dp, 38.dp).clip(RoundedCornerShape(12.dp))
            .background(CREAM.copy(alpha = if (down) 0.9f else 0.3f))
            .border(1.dp, Color.White.copy(alpha = .3f), RoundedCornerShape(12.dp))
            .pointerInput(keyCode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    down = true; retro.sendKeyEvent(KeyEvent.ACTION_DOWN, keyCode)
                    do { val e = awaitPointerEvent() } while (e.changes.any { it.pressed })
                    down = false; retro.sendKeyEvent(KeyEvent.ACTION_UP, keyCode)
                }
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (down) Color(0xFF2A2433) else Color.White.copy(alpha = .9f), fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

@Composable
private fun Pill(label: String, keyCode: Int, retro: GLRetroView) {
    var down by remember { mutableStateOf(false) }
    Box(
        Modifier.clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = if (down) .6f else .4f))
            .border(1.dp, Color.White.copy(alpha = .22f), RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .pointerInput(keyCode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    down = true; retro.sendKeyEvent(KeyEvent.ACTION_DOWN, keyCode)
                    do { val e = awaitPointerEvent() } while (e.changes.any { it.pressed })
                    down = false; retro.sendKeyEvent(KeyEvent.ACTION_UP, keyCode)
                }
            },
    ) { Text(label, color = Color.White.copy(alpha = .85f), fontWeight = FontWeight.Bold, fontSize = 11.sp) }
}

/** D-pad com diagonais: um toque na área calcula até 2 direções pela posição do dedo. */
@Composable
private fun DPad(retro: GLRetroView, modifier: Modifier, d: Dp) {
    var active by remember { mutableStateOf(setOf<Int>()) }
    Box(
        modifier.size(d).pointerInput(Unit) {
            fun update(pos: Offset?) {
                val want = HashSet<Int>()
                if (pos != null) {
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val dead = size.width * 0.16f
                    if (pos.x < cx - dead) want.add(KeyEvent.KEYCODE_DPAD_LEFT)
                    if (pos.x > cx + dead) want.add(KeyEvent.KEYCODE_DPAD_RIGHT)
                    if (pos.y < cy - dead) want.add(KeyEvent.KEYCODE_DPAD_UP)
                    if (pos.y > cy + dead) want.add(KeyEvent.KEYCODE_DPAD_DOWN)
                }
                (want - active).forEach { retro.sendKeyEvent(KeyEvent.ACTION_DOWN, it) }
                (active - want).forEach { retro.sendKeyEvent(KeyEvent.ACTION_UP, it) }
                active = want
            }
            awaitEachGesture {
                val first = awaitFirstDown(requireUnconsumed = false)
                update(first.position)
                while (true) {
                    val e = awaitPointerEvent()
                    val ch = e.changes.firstOrNull { it.id == first.id }
                    if (ch == null || !ch.pressed) { update(null); break }
                    update(ch.position)
                }
            }
        },
    ) {
        // visual: cruz creme
        val arm = CircleShape
        Box(Modifier.align(Alignment.Center).size(d * 0.30f, d).clip(RoundedCornerShape(14.dp)).background(CREAM.copy(alpha = .3f)))
        Box(Modifier.align(Alignment.Center).size(d, d * 0.30f).clip(RoundedCornerShape(14.dp)).background(CREAM.copy(alpha = .3f)))
        val dir = active
        if (dir.contains(KeyEvent.KEYCODE_DPAD_UP)) Box(Modifier.align(Alignment.TopCenter).size(d * 0.30f, d * 0.35f).background(CREAM.copy(alpha = .6f)))
        if (dir.contains(KeyEvent.KEYCODE_DPAD_DOWN)) Box(Modifier.align(Alignment.BottomCenter).size(d * 0.30f, d * 0.35f).background(CREAM.copy(alpha = .6f)))
        if (dir.contains(KeyEvent.KEYCODE_DPAD_LEFT)) Box(Modifier.align(Alignment.CenterStart).size(d * 0.35f, d * 0.30f).background(CREAM.copy(alpha = .6f)))
        if (dir.contains(KeyEvent.KEYCODE_DPAD_RIGHT)) Box(Modifier.align(Alignment.CenterEnd).size(d * 0.35f, d * 0.30f).background(CREAM.copy(alpha = .6f)))
    }
}

/* ---------------- Nintendo 64 ---------------- */

/** Layout do N64: analógico (controle principal), C-buttons, B/A, Z, L/R e Start. */
@Composable
private fun N64Pad(retro: GLRetroView) {
    Box(Modifier.fillMaxSize()) {
        Shoulder("L", KeyEvent.KEYCODE_BUTTON_L1, retro, Modifier.align(Alignment.TopStart).padding(14.dp, 10.dp))
        Shoulder("R", KeyEvent.KEYCODE_BUTTON_R1, retro, Modifier.align(Alignment.TopEnd).padding(14.dp, 10.dp))
        Shoulder("Z", KeyEvent.KEYCODE_BUTTON_L2, retro, Modifier.align(Alignment.TopStart).padding(14.dp, 56.dp))

        AnalogStick(retro, Modifier.align(Alignment.BottomStart).padding(24.dp, 0.dp, 0.dp, 26.dp), 152.dp)

        // direita: C-buttons (amarelos) em cima, B/A embaixo
        Column(
            Modifier.align(Alignment.BottomEnd).padding(0.dp, 0.dp, 18.dp, 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CCluster(retro)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Hold("B", KeyEvent.KEYCODE_BUTTON_B, retro, Modifier, 58.dp, Color(0xFF36B06A))
                Hold("A", KeyEvent.KEYCODE_BUTTON_A, retro, Modifier, 66.dp, Color(0xFF2F6AE0))
            }
        }

        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)) {
            Pill("START", KeyEvent.KEYCODE_BUTTON_START, retro)
        }
    }
}

/** Analógico virtual: posição contínua do dedo → eixo esquerdo do core. */
@Composable
private fun AnalogStick(retro: GLRetroView, modifier: Modifier, d: Dp) {
    var knob by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier.size(d).clip(CircleShape)
            .background(CREAM.copy(alpha = if (knob == Offset.Zero) .14f else .22f))
            .border(1.dp, Color.White.copy(alpha = .28f), CircleShape)
            .pointerInput(Unit) {
                val r = size.width / 2f
                fun send(pos: Offset?) {
                    if (pos == null) {
                        knob = Offset.Zero
                        retro.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, 0f, 0f)
                        return
                    }
                    val dx = pos.x - r; val dy = pos.y - r
                    val m = hypot(dx, dy)
                    val cl = if (m > r) r else m
                    val ux = if (m > 0f) dx / m else 0f
                    val uy = if (m > 0f) dy / m else 0f
                    knob = Offset(ux * cl, uy * cl)
                    retro.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, ux * (cl / r), uy * (cl / r))
                }
                awaitEachGesture {
                    val first = awaitFirstDown(requireUnconsumed = false)
                    send(first.position)
                    while (true) {
                        val e = awaitPointerEvent()
                        val ch = e.changes.firstOrNull { it.id == first.id }
                        if (ch == null || !ch.pressed) { send(null); break }
                        send(ch.position)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.offset { IntOffset(knob.x.roundToInt(), knob.y.roundToInt()) }
                .size(d * 0.44f).clip(CircleShape).background(CREAM.copy(alpha = .55f))
                .border(1.dp, Color.White.copy(alpha = .4f), CircleShape),
        )
    }
}

/** Os 4 C-buttons (amarelos). Compartilham o eixo direito (default do mupen64plus_next). */
@Composable
private fun CCluster(retro: GLRetroView) {
    val down = remember { mutableStateMapOf<String, Boolean>() }
    fun push() {
        var x = 0f; var y = 0f
        if (down["left"] == true) x -= 1f
        if (down["right"] == true) x += 1f
        if (down["up"] == true) y -= 1f
        if (down["down"] == true) y += 1f
        retro.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_RIGHT, x, y)
    }
    Box(Modifier.size(132.dp)) {
        CBtn("▲", Modifier.align(Alignment.TopCenter)) { down["up"] = it; push() }
        CBtn("◀", Modifier.align(Alignment.CenterStart)) { down["left"] = it; push() }
        CBtn("▶", Modifier.align(Alignment.CenterEnd)) { down["right"] = it; push() }
        CBtn("▼", Modifier.align(Alignment.BottomCenter)) { down["down"] = it; push() }
    }
}

@Composable
private fun CBtn(label: String, modifier: Modifier, onDown: (Boolean) -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier.size(44.dp).clip(CircleShape)
            .background(Color(0xFFE8B84A).copy(alpha = if (pressed) 1f else .6f))
            .border(1.dp, Color.White.copy(alpha = .35f), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true; onDown(true)
                    do { val e = awaitPointerEvent() } while (e.changes.any { it.pressed })
                    pressed = false; onDown(false)
                }
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color(0xFF3A2A00), fontWeight = FontWeight.Bold, fontSize = 13.sp) }
}
