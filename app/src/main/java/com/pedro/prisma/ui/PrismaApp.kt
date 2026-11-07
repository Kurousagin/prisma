package com.pedro.prisma.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

// chave Pix aleatória (EVP) do projeto
private const val PIX_KEY = "5213f532-d990-4e65-be9f-f0173882c9c4"

private fun inProgress(ctx: Context, g: GameEntry) =
    File(ctx.filesDir, "states/${Games.stateKey(g)}.state").exists()

@Composable
fun PrismaApp(remoteUrl: String?) {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf("home") }
    var detail by remember { mutableStateOf<GameEntry?>(null) }
    var libFilter by remember { mutableStateOf("Todos") }
    var reload by remember { mutableStateOf(0) }

    var games by remember { mutableStateOf<List<GameEntry>?>(null) }

    LaunchedEffect(reload) {
        games = null
        // abre a biblioteca assim que o scan termina; cada card baixa sua capa
        // sozinho (lazy, em paralelo), sem travar a abertura com downloads em série.
        games = withContext(Dispatchers.IO) { Games.load(ctx) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { Games.saveFolder(ctx, uri); reload++ }
    }
    val pickFolder = { picker.launch(null) }

    val list = games
    if (list == null) {
        OnboardingScan()
        return
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
                        "home" -> HomeTab(list, pickFolder, onOpen = { detail = it }, onConsole = { sys -> libFilter = sys; tab = "lib" })
                        "lib" -> LibraryTab(list, libFilter, { libFilter = it }, onOpen = { detail = it })
                        "config" -> SettingsTab(pickFolder)
                        else -> SupportTab()
                    }
                }
                BottomNav(tab) { tab = it }
            }
        }
    }
}

@Composable
private fun OnboardingScan() {
    Column(
        Modifier.fillMaxSize().background(P.bg).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            androidx.compose.ui.res.painterResource(com.pedro.prisma.R.mipmap.ic_launcher_foreground),
            contentDescription = null, modifier = Modifier.size(128.dp).clip(RoundedCornerShape(30.dp)),
        )
        Spacer(Modifier.height(14.dp))
        Text("Prisma", color = P.ink, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Text("Sua jornada nostálgica começa aqui.", color = P.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("Procurando seus jogos…", color = P.dim, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        androidx.compose.material3.LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(0.82f).height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = P.p2, trackColor = P.card,
        )
    }
}

@Composable
private fun LogoHeader() {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            androidx.compose.ui.res.painterResource(com.pedro.prisma.R.mipmap.ic_launcher_foreground),
            contentDescription = null, modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.width(9.dp))
        Text("Prisma", color = P.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

/* ---------------- Boxart ---------------- */
@Composable
fun BoxArt(game: GameEntry, modifier: Modifier = Modifier, reloadKey: Any = Unit) {
    val ctx = LocalContext.current
    val cover by produceState<ImageBitmap?>(initialValue = null, game.uri.toString(), reloadKey) {
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
private fun HomeTab(games: List<GameEntry>, onPickFolder: () -> Unit, onOpen: (GameEntry) -> Unit, onConsole: (String) -> Unit) {
    val ctx = LocalContext.current
    val resumable = games.filter { inProgress(ctx, it) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        LogoHeader()
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
        if (games.isEmpty()) {
            Section("Nenhuma ROM ainda")
            Text(
                "Escolha a pasta onde estão suas ROMs — o Prisma monta a biblioteca sozinho.",
                color = P.dim, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp),
            )
            PickFolderButton(onPickFolder)
        } else {
            val recentes = games.filter { System.currentTimeMillis() - it.added < 3L * 24 * 3600 * 1000 }
                .sortedByDescending { it.added }
            if (recentes.isNotEmpty()) {
                Section("Adicionados recentemente")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    recentes.take(12).forEach { g ->
                        BoxArt(g, Modifier.width(118.dp).aspectRatio(3f / 3.7f).clickable { onOpen(g) })
                    }
                }
            }
            Section("Todos os jogos")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                games.take(12).forEach { g ->
                    BoxArt(g, Modifier.width(100.dp).aspectRatio(3f / 3.7f).clickable { onOpen(g) })
                }
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
        ) { ConsoleGlyph() }
        Spacer(Modifier.height(6.dp))
        Text(label, color = P.dim, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun Color.compositeOverLike(base: Color) = Color(base.red * .4f, base.green * .4f, base.blue * .4f, 1f)

/** Ícone de cartucho desenhado (branco) — identidade de "console" em vez de letras. */
@Composable
private fun ConsoleGlyph() {
    androidx.compose.foundation.Canvas(Modifier.size(32.dp)) {
        val w = size.width; val h = size.height
        val white = Color.White.copy(alpha = 0.95f)
        val dark = Color(0xFF1A1326)
        drawRoundRect(white, topLeft = androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.08f),
            size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f, w * 0.04f))
        drawRoundRect(white, topLeft = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.16f),
            size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.70f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f))
        drawRoundRect(dark, topLeft = androidx.compose.ui.geometry.Offset(w * 0.31f, h * 0.30f),
            size = androidx.compose.ui.geometry.Size(w * 0.38f, h * 0.26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.03f, w * 0.03f))
        for (i in 0..3) drawRect(dark, topLeft = androidx.compose.ui.geometry.Offset(w * (0.33f + i * 0.095f), h * 0.66f),
            size = androidx.compose.ui.geometry.Size(w * 0.05f, h * 0.12f))
    }
}

/* ---------------- Biblioteca ---------------- */
@Composable
private fun LibraryTab(games: List<GameEntry>, filter: String, onFilter: (String) -> Unit, onOpen: (GameEntry) -> Unit) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("az") }   // az | recent
    val systems = listOf("Todos", "★ Favoritos") + Games.systems.map { it.label }
    val q = query.trim()
    val favs = Favorites.all(ctx)
    val shown = games.filter {
        val okFilter = when (filter) {
            "Todos" -> true
            "★ Favoritos" -> favs.contains(Games.stateKey(it))
            else -> it.system.label == filter
        }
        okFilter && (q.isEmpty() || it.title.contains(q, ignoreCase = true))
    }.let { l -> if (sort == "recent") l.sortedByDescending { it.added } else l.sortedBy { it.title.lowercase() } }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        AppBar("Biblioteca")
        SearchField(query) { query = it }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            systems.forEach { s -> FilterChip(s, s == filter) { onFilter(s) } }
        }
        Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${shown.size} ${if (shown.size == 1) "jogo" else "jogos"}", color = P.faint, fontSize = 11.5.sp)
            Spacer(Modifier.weight(1f))
            Text("Ordenar:", color = P.faint, fontSize = 11.5.sp, modifier = Modifier.padding(end = 7.dp))
            SortChip("A-Z", sort == "az") { sort = "az" }
            Spacer(Modifier.width(6.dp))
            SortChip("Recentes", sort == "recent") { sort = "recent" }
        }
        if (shown.isEmpty()) {
            Text("Nenhum jogo encontrado.", color = P.dim, fontSize = 13.sp, modifier = Modifier.padding(top = 32.dp))
        } else LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(shown, key = { it.uri.toString() }) { g ->
                BoxArt(g, Modifier.aspectRatio(3f / 3.9f).clickable { onOpen(g) })
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(P.card)
            .border(1.dp, P.line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🔍", fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Buscar jogo…", color = P.faint, fontSize = 14.sp)
            BasicTextField(
                value = query, onValueChange = onQuery, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = P.ink, fontSize = 14.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(P.p2),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Text("✕", color = P.dim, fontSize = 14.sp, modifier = Modifier.clickable { onQuery("") }.padding(start = 6.dp))
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

@Composable
private fun SortChip(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(999.dp))
            .background(if (on) P.card2 else Color.Transparent)
            .border(1.dp, if (on) P.p1 else P.line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 5.dp),
    ) { Text(label, color = if (on) P.p2 else P.dim, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
}

/* ---------------- Detalhe ---------------- */
@Composable
private fun GameDetail(g: GameEntry, remoteUrl: String?, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val resumes = inProgress(ctx, g)
    var showQr by remember { mutableStateOf(false) }
    var coverV by remember { mutableStateOf(0) }
    var fav by remember { mutableStateOf(Favorites.contains(ctx, Games.stateKey(g))) }
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            if (bytes != null) { Covers.saveUserCover(ctx, g, bytes); coverV++ }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(200.dp)) {
            BoxArt(g, Modifier.fillMaxSize(), coverV)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, P.bg))))
            Box(
                Modifier.padding(16.dp).size(34.dp).clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = .4f)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("‹", color = Color.White, fontSize = 20.sp) }
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                BoxArt(g, Modifier.width(92.dp).height(112.dp), coverV)
                Column(Modifier.weight(1f)) {
                    Text(g.title, color = P.ink, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 23.sp)
                    Text(g.system.label, color = P.dim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(if (resumes) "em andamento" else "novo jogo", color = P.faint, fontSize = 12.5.sp)
                }
                Text(
                    if (fav) "★" else "☆", fontSize = 26.sp, color = if (fav) Color(0xFFE8B84A) else P.dim,
                    modifier = Modifier.clickable { fav = Favorites.toggle(ctx, Games.stateKey(g)) }.padding(4.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            if (resumes) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(15.dp)).background(consoleGrad(g.system.label))
                            .clickable { Games.launch(ctx, g, "auto") }.padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("▶ CONTINUAR", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    Box(
                        Modifier.clip(RoundedCornerShape(15.dp)).background(P.card).border(1.dp, P.line, RoundedCornerShape(15.dp))
                            .clickable { Games.launch(ctx, g, "new") }.padding(horizontal = 16.dp, vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Novo jogo", color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                }
                Text("Continuar salva sozinho ao sair — você volta de onde parou.", color = P.faint, fontSize = 11.5.sp,
                    modifier = Modifier.padding(top = 8.dp))
            } else {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(consoleGrad(g.system.label))
                        .clickable { Games.launch(ctx, g, "auto") }.padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("▶ JOGAR", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
            val info by produceState<GameInfo.Info?>(null, g.uri) {
                value = withContext(Dispatchers.IO) { GameInfo.get(ctx, g.title) }
            }
            info?.let { AboutSection(it) }
            val savedSlots = remember(g.uri) { StateSlots.list(ctx, Games.stateKey(g)) }.filter { it.used }
            if (savedSlots.isNotEmpty()) {
                Section("Carregar estado")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    savedSlots.forEach { s -> SlotLoadCard(g, s) { Games.launch(ctx, g, s.n.toString()) } }
                }
            }
            Section("Extras")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExtraTile("🎮", "Controle\npelo celular", Modifier.weight(1f)) { showQr = true }
                ExtraTile("🖼️", "Trocar\ncapa", Modifier.weight(1f)) { pickCover.launch("image/*") }
                ExtraTile("🏆", "Conquistas\nem breve", Modifier.weight(1f)) {}
                ExtraTile("☁️", "Backup\nem breve", Modifier.weight(1f)) {}
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (showQr) QrDialog(remoteUrl) { showQr = false }
}

@Composable
private fun AboutSection(info: GameInfo.Info) {
    var expanded by remember { mutableStateOf(false) }
    Section("Sobre o jogo")
    if (info.year != null || info.genre != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
            info.year?.let { InfoChip("📅 $it") }
            info.genre?.let { InfoChip("🎯 ${it.replaceFirstChar { c -> c.uppercase() }}") }
        }
    }
    Text(
        info.about, color = P.dim, fontSize = 13.sp, lineHeight = 19.sp,
        maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
    )
    if (info.about.length > 160) {
        Text(
            if (expanded) "ver menos" else "ver mais", color = P.p2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp).clickable { expanded = !expanded },
        )
    }
    Text("Fonte: Wikipédia", color = P.faint, fontSize = 10.5.sp, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun InfoChip(text: String) {
    Box(
        Modifier.clip(RoundedCornerShape(999.dp)).background(P.card)
            .border(1.dp, P.line, RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
    ) { Text(text, color = P.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun SlotLoadCard(g: GameEntry, slot: StateSlots.SlotInfo, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val thumb = remember(slot.n) { StateSlots.loadThumb(ctx, Games.stateKey(g), slot.n) }
    val fmt = remember { java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()) }
    Column(
        Modifier.width(120.dp).clip(RoundedCornerShape(12.dp)).background(P.card)
            .border(1.dp, P.line, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(8.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = .35f)),
            contentAlignment = Alignment.Center,
        ) {
            if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text("sem print", color = P.faint, fontSize = 10.sp)
        }
        Text("Slot ${slot.n}", color = P.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        Text(slot.time?.let { fmt.format(java.util.Date(it)) } ?: "", color = P.dim, fontSize = 10.5.sp)
    }
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

@Composable
private fun PickFolderButton(onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(13.dp)).background(P.grad).clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) { Text("📂 Escolher pasta de ROMs", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
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
            Image(remember { qrBitmap(pixPayload(PIX_KEY)) }, "QR Pix",
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
private fun SettingsTab(onPickFolder: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        AppBar("Ajustes")
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(P.card)
                .border(1.dp, P.line, RoundedCornerShape(18.dp)).padding(15.dp),
        ) {
            Text("Pasta de ROMs", color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Escolha onde ficam seus jogos. O Prisma escaneia sozinho depois.",
                color = P.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            PickFolderButton(onPickFolder)
        }
        Spacer(Modifier.height(14.dp))
        ShaderSettingCard()
        Spacer(Modifier.height(14.dp))
        AnimSettingCard()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ShaderSettingCard() {
    val ctx = LocalContext.current
    var mode by remember { mutableStateOf(ShaderPrefs.mode(ctx)) }
    val pick = { m: String -> mode = m; ShaderPrefs.setMode(ctx, m) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(P.card)
            .border(1.dp, P.line, RoundedCornerShape(18.dp)).padding(15.dp),
    ) {
        Text("Filtro de vídeo", color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("Como a imagem aparece. Vale ao abrir o jogo.",
            color = P.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("Nítido", mode == ShaderPrefs.SHARP) { pick(ShaderPrefs.SHARP) }
            FilterChip("Suave", mode == ShaderPrefs.SMOOTH) { pick(ShaderPrefs.SMOOTH) }
            FilterChip("CRT", mode == ShaderPrefs.CRT) { pick(ShaderPrefs.CRT) }
            FilterChip("LCD", mode == ShaderPrefs.LCD) { pick(ShaderPrefs.LCD) }
        }
    }
}

@Composable
private fun AnimSettingCard() {
    val ctx = LocalContext.current
    var mode by remember { mutableStateOf(IntroPrefs.mode(ctx)) }
    val pick = { m: String -> mode = m; IntroPrefs.setMode(ctx, m) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(P.card)
            .border(1.dp, P.line, RoundedCornerShape(18.dp)).padding(15.dp),
    ) {
        Text("Animação de abertura", color = P.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("O cartucho entrando no console ao iniciar um jogo.",
            color = P.dim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("Sempre", mode == IntroPrefs.ALWAYS) { pick(IntroPrefs.ALWAYS) }
            FilterChip("Só a 1ª vez", mode == IntroPrefs.ONCE) { pick(IntroPrefs.ONCE) }
            FilterChip("Desativada", mode == IntroPrefs.OFF) { pick(IntroPrefs.OFF) }
        }
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
        Image(
            painter = androidx.compose.ui.res.painterResource(com.pedro.prisma.R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)),
        )
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
        NavItem("⚙", "Ajustes", tab == "config") { onTab("config") }
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
