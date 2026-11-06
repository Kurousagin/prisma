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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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

/** Gamepad + barra superior (save / ocultar controles / sair). Auto-save segue separado. */
@Composable
fun GameOverlay(retro: GLRetroView, system: String, stateKey: String, onExit: () -> Unit) {
    var showSlots by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    Box(Modifier.fillMaxSize()) {
        if (controls) TouchGamepad(retro, system)
        // barra superior sempre visível (mesmo com os controles ocultos)
        Row(
            Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TopBtn("💾") { showSlots = true }
            TopBtn(if (controls) "🚫" else "🎮") { controls = !controls }  // oculta/mostra o controle touch
            TopBtn("✕") { onExit() }
        }
    }
    if (showSlots) SlotSheet(retro, stateKey) { showSlots = false }
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
private fun SlotSheet(retro: GLRetroView, stateKey: String, onClose: () -> Unit) {
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
                            onClose()
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
