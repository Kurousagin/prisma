package com.pedro.prisma.ui

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.libretrodroid.GLRetroView

private val CREAM = Color(0xFFF1ECDE)

/** Controles touch adaptados a cada console. Multitouch real: cada botão trata seu próprio dedo. */
@Composable
fun TouchGamepad(retro: GLRetroView, system: String, onExit: () -> Unit) {
    val hasLR = system == "Game Boy Advance" || system == "Super Nintendo"
    val selLabel = if (system == "Mega Drive") "MODE" else "SELECT"
    Box(Modifier.fillMaxSize()) {
        if (hasLR) {
            Shoulder("L", KeyEvent.KEYCODE_BUTTON_L1, retro, Modifier.align(Alignment.TopStart).padding(14.dp, 10.dp))
            Shoulder("R", KeyEvent.KEYCODE_BUTTON_R1, retro, Modifier.align(Alignment.TopEnd).padding(14.dp, 10.dp))
        }

        // sair (volta pro detalhe; o save acontece no onPause)
        Box(
            Modifier.align(Alignment.TopCenter).padding(top = 10.dp).size(40.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = .35f)).border(1.dp, Color.White.copy(alpha = .25f), CircleShape)
                .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false); onExit() } },
            contentAlignment = Alignment.Center,
        ) { Text("✕", color = Color.White.copy(alpha = .8f), fontSize = 17.sp) }

        // D-pad (com diagonais)
        DPad(retro, Modifier.align(Alignment.BottomStart).padding(18.dp, 0.dp, 0.dp, 22.dp), 150.dp)

        // botões de ação — layout por console
        Box(Modifier.align(Alignment.BottomEnd).padding(0.dp, 0.dp, 18.dp, 22.dp)) {
            when (system) {
                "Super Nintendo" -> Box(Modifier.size(168.dp)) {
                    Hold("X", KeyEvent.KEYCODE_BUTTON_X, retro, Modifier.align(Alignment.TopCenter), 56.dp)
                    Hold("Y", KeyEvent.KEYCODE_BUTTON_Y, retro, Modifier.align(Alignment.CenterStart), 56.dp)
                    Hold("A", KeyEvent.KEYCODE_BUTTON_A, retro, Modifier.align(Alignment.CenterEnd), 56.dp)
                    Hold("B", KeyEvent.KEYCODE_BUTTON_B, retro, Modifier.align(Alignment.BottomCenter), 56.dp)
                }
                // Mega Drive: A=RetroPad Y, B=B, C=A (mapeamento do genesis_plus_gx)
                "Mega Drive" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Hold("A", KeyEvent.KEYCODE_BUTTON_Y, retro, Modifier, 58.dp)
                    Hold("B", KeyEvent.KEYCODE_BUTTON_B, retro, Modifier, 58.dp)
                    Hold("C", KeyEvent.KEYCODE_BUTTON_A, retro, Modifier, 58.dp)
                }
                // NES, Game Boy, Game Boy Advance: só B e A
                else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Hold("B", KeyEvent.KEYCODE_BUTTON_B, retro, Modifier, 62.dp)
                    Hold("A", KeyEvent.KEYCODE_BUTTON_A, retro, Modifier, 62.dp)
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
private fun Hold(label: String, keyCode: Int, retro: GLRetroView, modifier: Modifier, d: Dp) {
    var down by remember { mutableStateOf(false) }
    Box(
        modifier.size(d).clip(CircleShape)
            .background(CREAM.copy(alpha = if (down) 0.9f else 0.32f))
            .border(1.dp, Color.White.copy(alpha = .3f), CircleShape)
            .pointerInput(keyCode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    down = true; retro.sendKeyEvent(KeyEvent.ACTION_DOWN, keyCode)
                    do { val e = awaitPointerEvent() } while (e.changes.any { it.pressed })
                    down = false; retro.sendKeyEvent(KeyEvent.ACTION_UP, keyCode)
                }
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (down) Color(0xFF2A2433) else Color.White.copy(alpha = .9f), fontWeight = FontWeight.Bold, fontSize = 18.sp) }
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
