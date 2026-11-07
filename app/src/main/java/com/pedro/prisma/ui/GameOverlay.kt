package com.pedro.prisma.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.swordfish.libretrodroid.GLRetroView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Gamepad + barra superior (pausar / ocultar controles). Pausa concentra save/carregar/reiniciar/sair. */
@Composable
fun GameOverlay(retro: GLRetroView, system: String, stateKey: String, title: String, onExit: () -> Unit) {
    val ctx = LocalContext.current
    var showSlots by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    // se já tem um gamepad conectado, começa com o controle touch oculto
    var controls by remember { mutableStateOf(!hasGamepad()) }

    fun resume() { runCatching { retro.frameSpeed = 1; retro.audioEnabled = true }; paused = false }
    fun pause() { runCatching { retro.frameSpeed = 0; retro.audioEnabled = false }; paused = true }

    Box(Modifier.fillMaxSize()) {
        if (controls) TouchGamepad(retro, system)
        Row(
            Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TopBtn("⏸") { pause() }
            TopBtn(if (controls) "🚫" else "🎮") { controls = !controls }  // oculta/mostra o controle touch
        }
    }
    if (paused) PauseMenu(
        onResume = { resume() },
        onSaves = { showSlots = true },
        onShare = { Screenshot.shareFramed(retro, ctx, title, system) },
        onReset = { runCatching { retro.reset() }; resume() },
        onExit = onExit,
    )
    if (showSlots) SlotSheet(retro, stateKey, onAfterLoad = { showSlots = false; resume() }) { showSlots = false }
}

@Composable
private fun PauseMenu(onResume: () -> Unit, onSaves: () -> Unit, onShare: () -> Unit, onReset: () -> Unit, onExit: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f))
            .clickable(remember { MutableInteractionSource() }, indication = null) { onResume() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.width(260.dp).clip(RoundedCornerShape(20.dp)).background(P.card2).padding(18.dp)
                .clickable(remember { MutableInteractionSource() }, indication = null) {},  // não fecha ao tocar no card
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Pausado", color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            MenuBtn("▶  Continuar", true, onResume)
            Spacer(Modifier.height(9.dp))
            MenuBtn("💾  Save states", false, onSaves)
            Spacer(Modifier.height(9.dp))
            MenuBtn("📸  Compartilhar print", false, onShare)
            Spacer(Modifier.height(9.dp))
            MenuBtn("↻  Reiniciar", false, onReset)
            Spacer(Modifier.height(9.dp))
            MenuBtn("✕  Sair", false, onExit)
        }
    }
}

@Composable
private fun MenuBtn(label: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
            .then(if (primary) Modifier.background(P.grad) else Modifier.background(P.card).border(1.dp, P.line, RoundedCornerShape(13.dp)))
            .clickable(onClick = onClick).padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (primary) Color.White else P.ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold) }
}

/** Há algum controle físico (gamepad/joystick, BLE ou com fio) conectado? */
private fun hasGamepad(): Boolean {
    for (id in android.view.InputDevice.getDeviceIds()) {
        val s = android.view.InputDevice.getDevice(id)?.sources ?: continue
        if (s and android.view.InputDevice.SOURCE_GAMEPAD == android.view.InputDevice.SOURCE_GAMEPAD ||
            s and android.view.InputDevice.SOURCE_JOYSTICK == android.view.InputDevice.SOURCE_JOYSTICK
        ) return true
    }
    return false
}

@Composable
private fun TopBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = .35f))
            .border(1.dp, Color.White.copy(alpha = .25f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 15.sp, color = Color.White.copy(alpha = .85f)) }
}

@Composable
private fun SlotSheet(retro: GLRetroView, stateKey: String, onAfterLoad: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var version by remember { mutableStateOf(0) }
    val slots = remember(version) { StateSlots.list(ctx, stateKey) }
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.width(360.dp).clip(RoundedCornerShape(20.dp)).background(P.card2).padding(18.dp),
        ) {
            Text("Save states", color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Salve e carregue em até ${StateSlots.COUNT} slots.", color = P.dim, fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
            slots.forEach { s ->
                SlotRow(
                    slot = s, stateKey = stateKey, version = version,
                    onSave = {
                        StateSlots.save(retro, ctx, stateKey, s.n) {
                            version++
                            Toast.makeText(ctx, "Salvo no slot ${s.n}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLoad = {
                        if (StateSlots.load(retro, ctx, stateKey, s.n)) {
                            Toast.makeText(ctx, "Carregado do slot ${s.n}", Toast.LENGTH_SHORT).show()
                            onAfterLoad()
                        } else {
                            Toast.makeText(ctx, "Slot vazio", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
            }
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(P.card)
                    .clickable(onClick = onClose).padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Fechar", color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

private val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

@Composable
private fun SlotRow(
    slot: StateSlots.SlotInfo, stateKey: String, version: Int,
    onSave: () -> Unit, onLoad: () -> Unit,
) {
    val ctx = LocalContext.current
    val thumb = remember(version, slot.n) { StateSlots.loadThumb(ctx, stateKey, slot.n) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(72.dp, 46.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = .35f)),
            contentAlignment = Alignment.Center,
        ) {
            if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text("vazio", color = P.faint, fontSize = 10.sp)
        }
        Column(Modifier.weight(1f)) {
            Text("Slot ${slot.n}", color = P.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(if (slot.used) fmt.format(Date(slot.time!!)) else "—", color = P.dim, fontSize = 11.5.sp)
        }
        SmallBtn("Salvar", P.grad, onSave)
        SmallBtn("Carregar", if (slot.used) P.grad else null, onLoad)
    }
}

@Composable
private fun SmallBtn(label: String, bg: androidx.compose.ui.graphics.Brush?, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(999.dp))
            .then(if (bg != null) Modifier.background(bg) else Modifier.background(P.card).border(1.dp, P.line, RoundedCornerShape(999.dp)))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
    ) { Text(label, color = if (bg != null) Color.White else P.dim, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
}
