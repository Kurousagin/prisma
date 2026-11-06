package com.pedro.prisma.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// shortcut: chave Pix de exemplo; trocar pela real do Pedro antes de publicar.
private const val PIX_KEY = "pix@prisma.app"

private fun inProgress(ctx: Context, g: GameEntry) =
    File(ctx.filesDir, "states/${g.file.name}.state").exists()

@Composable
fun PrismaApp(remoteUrl: String?) {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf("home") }
    var detail by remember { mutableStateOf<GameEntry?>(null) }
    var libFilter by remember { mutableStateOf("Todos") }

    val games by produceState(initialValue = emptyList<GameEntry>()) {
        value = withContext(Dispatchers.IO) { Games.load(ctx) }
    }

    BackHandler(enabled = detail != null || tab != "home") {
        if (detail != null) detail = null else tab = "home"
    }

    Box(Modifier.fillMaxSize().background(P.bg)) {
        val d = detail
        if (d != null) {
            GameDetail(d, remoteUrl, onBack = { detail = null })
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        "home" -> HomeTab(games, onOpen = { detail = it }) { sys -> libFilter = sys; tab = "lib" }
                        "lib" -> LibraryTab(games, libFilter, { libFilter = it }, onOpen = { detail = it })
                        else -> SupportTab()
                    }
                }
                BottomNav(tab) { tab = it }
            }
        }
    }
}

/* ---------------- Boxart ---------------- */
@Composable
fun BoxArt(game: GameEntry, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val cover by produceState<ImageBitmap?>(initialValue = null, game.file.path) {
        value = withContext(Dispatchers.IO) { Covers.load(ctx, game)?.asImageBitmap() }
    }
    Box(modifier.clip(RoundedCornerShape(13.dp)).background(Brush.verticalGradient(coverGradient(game.system.label)))) {
        val c = cover
        if (c != null) {
            Image(c, game.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize()) {
                // etiqueta do console
                Box(
                    Modifier.fillMaxWidth().padding(8.dp).height(18.dp).clip(RoundedCornerShape(5.dp))
                        .background(Color.White.copy(alpha = .9f)),
                    contentAlignment = Alignment.Center,
                ) { Text(game.system.label, color = Color(0xFF1A1326), fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                Text(
                    game.title.take(1).uppercase(), color = Color.White.copy(alpha = .95f),
                    fontSize = 40.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center),
                )
                Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .82f))))
                        .padding(8.dp, 14.dp, 8.dp, 7.dp),
                ) {
                    Text(game.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 13.sp)
                }
            }
        }
    }
}

/* ---------------- Início ---------------- */
@Composable
private fun HomeTab(games: List<GameEntry>, onOpen: (GameEntry) -> Unit, onConsole: (String) -> Unit) {
    val ctx = LocalContext.current
    val resumable = games.filter { inProgress(ctx, it) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        AppBar("Prisma")
        if (resumable.isNotEmpty()) {
            Section("Continuar jogando")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                resumable.forEach { ContinueCard(it) { onOpen(it) } }
            }
        }
        Section("Consoles")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Games.systems.forEach { s -> ConsoleChip(s.label) { onConsole(s.label) } }
        }
        Section(if (games.isEmpty()) "Nenhuma ROM ainda" else "Seus jogos")
        if (games.isEmpty()) {
            Text(
                "Copie suas ROMs para /sdcard/Roms (ou uma pasta Roms no USB) e elas aparecem aqui.",
                color = P.dim, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                games.take(10).forEach { g -> BoxArt(g, Modifier.width(118.dp).aspectRatio(3f / 3.7f).clickable { onOpen(g) }) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ContinueCard(g: GameEntry, onClick: () -> Unit) {
    Row(
        Modifier.width(250.dp).clip(RoundedCornerShape(16.dp)).background(P.card)
            .border(1.dp, P.line, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BoxArt(g, Modifier.size(58.dp))
        Column(Modifier.weight(1f)) {
            Text(g.title, color = P.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(g.system.label, color = P.dim, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.clip(RoundedCornerShape(999.dp)).background(P.grad).padding(horizontal = 18.dp, vertical = 6.dp),
            ) { Text("▶ Continuar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ConsoleChip(label: String, onClick: () -> Unit) {
    val c = systemColor(label)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(66.dp).clickable(onClick = onClick)) {
        Box(
            Modifier.size(58.dp).clip(RoundedCornerShape(19.dp))
                .background(Brush.linearGradient(listOf(c, Color(0x59000000).compositeOverLike(c))))
                .border(1.dp, Color.White.copy(alpha = .18f), RoundedCornerShape(19.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(label.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        Spacer(Modifier.height(6.dp))
        Text(label, color = P.dim, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun Color.compositeOverLike(base: Color) = Color(base.red * .4f, base.green * .4f, base.blue * .4f, 1f)

/* ---------------- Biblioteca ---------------- */
@Composable
private fun LibraryTab(games: List<GameEntry>, filter: String, onFilter: (String) -> Unit, onOpen: (GameEntry) -> Unit) {
    val systems = listOf("Todos") + Games.systems.map { it.label }
    val shown = if (filter == "Todos") games else games.filter { it.system.label == filter }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        AppBar("Biblioteca")
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            systems.forEach { s -> FilterChip(s, s == filter) { onFilter(s) } }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(shown, key = { it.file.path }) { g ->
                BoxArt(g, Modifier.aspectRatio(3f / 3.9f).clickable { onOpen(g) })
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(999.dp))
            .then(if (on) Modifier.background(P.grad) else Modifier.background(P.card).border(1.dp, P.line, RoundedCornerShape(999.dp)))
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 7.dp),
    ) { Text(label, color = if (on) Color.White else P.dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

/* ---------------- Detalhe ---------------- */
@Composable
private fun GameDetail(g: GameEntry, remoteUrl: String?, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val resumes = inProgress(ctx, g)
    var showQr by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(200.dp)) {
            BoxArt(g, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, P.bg))))
            Box(
                Modifier.padding(16.dp).size(34.dp).clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = .4f)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("‹", color = Color.White, fontSize = 20.sp) }
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                BoxArt(g, Modifier.width(92.dp).height(112.dp))
                Column {
                    Text(g.title, color = P.ink, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 23.sp)
                    Text(g.system.label, color = P.dim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(if (resumes) "em andamento" else "novo jogo", color = P.faint, fontSize = 12.5.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(P.grad)
                    .clickable { Games.launch(ctx, g) }.padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (resumes) "▶ CONTINUAR" else "▶ JOGAR", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            if (resumes) {
                Text("Salva sozinho ao sair — você volta de onde parou.", color = P.faint, fontSize = 11.5.sp,
                    modifier = Modifier.padding(top = 8.dp))
            }
            Section("Extras")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExtraTile("🎮", "Controle\npelo celular", Modifier.weight(1f)) { showQr = true }
                ExtraTile("🏆", "Conquistas\nem breve", Modifier.weight(1f)) {}
                ExtraTile("☁️", "Backup\nem breve", Modifier.weight(1f)) {}
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (showQr) QrDialog(remoteUrl) { showQr = false }
}

@Composable
private fun ExtraTile(emoji: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(15.dp)).background(P.card).border(1.dp, P.line, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text(label, color = P.dim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

@Composable
private fun QrDialog(remoteUrl: String?, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.clip(RoundedCornerShape(22.dp)).background(P.card2).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Controle pelo celular", color = P.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Escaneie no outro aparelho pra usar de controle.", color = P.dim, fontSize = 12.5.sp,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
            if (remoteUrl == null) {
                Text("iniciando…", color = P.dim, fontSize = 13.sp)
            } else {
                Image(
                    remember(remoteUrl) { qrBitmap(remoteUrl) }, "QR",
                    Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(8.dp),
                )
                Text(remoteUrl, color = P.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

/* ---------------- Apoiar (Pix) ---------------- */
@Composable
private fun SupportTab() {
    val clip = LocalClipboardManager.current
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        AppBar("Apoie o Prisma")
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(P.card)
                .border(1.dp, P.line, RoundedCornerShape(20.dp)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("O Prisma é grátis pra sempre.\nSe te ajudou, um Pix mantém o projeto vivo 💜",
                color = P.dim, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Image(remember { qrBitmap(PIX_KEY) }, "QR Pix",
                Modifier.size(170.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(12.dp))
            Spacer(Modifier.height(12.dp))
            Text(PIX_KEY, color = P.ink, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = .3f)).padding(10.dp))
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(P.grad).clickable {
                    clip.setText(AnnotatedString(PIX_KEY))
                    Toast.makeText(ctx, "Chave Pix copiada", Toast.LENGTH_SHORT).show()
                }.padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Copiar chave Pix", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(P.card)
                .border(1.dp, P.line, RoundedCornerShape(18.dp)).padding(15.dp),
        ) {
            AboutLine("Sem anúncios, sem compras, sem pegadinha")
            AboutLine("Sem rastreio — nada sai do seu aparelho (LGPD)")
            AboutLine("Seus jogos e saves ficam só com você")
            AboutLine("Código aberto (GPLv3)")
            Text("Você traz suas próprias ROMs. O Prisma não distribui jogos.",
                color = P.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AboutLine(t: String) {
    Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(P.good))
        Text(t, color = P.ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

/* ---------------- comuns ---------------- */
@Composable
private fun AppBar(title: String) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(P.grad), contentAlignment = Alignment.Center) {
            Text("🎮", fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(title, color = P.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Section(title: String) {
    Text(title, color = P.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 18.dp, bottom = 11.dp))
}

@Composable
private fun BottomNav(tab: String, onTab: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(P.card).border(1.dp, P.line, RoundedCornerShape(0.dp))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        NavItem("⌂", "Início", tab == "home") { onTab("home") }
        NavItem("▦", "Biblioteca", tab == "lib") { onTab("lib") }
        NavItem("💜", "Apoiar", tab == "support") { onTab("support") }
    }
}

@Composable
private fun NavItem(icon: String, label: String, on: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 4.dp),
    ) {
        Text(icon, fontSize = 18.sp, color = if (on) P.p2 else P.faint)
        Text(label, fontSize = 10.sp, color = if (on) P.p2 else P.faint, fontWeight = FontWeight.SemiBold)
    }
}
