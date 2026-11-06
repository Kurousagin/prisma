package com.pedro.prisma.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

data class GameSystem(
    val label: String,
    val exts: Set<String>,
    val core: String,              // .so em jniLibs → roda embutido (LibretroDroid)
    val thumbDir: String? = null,  // pasta no thumbnails.libretro.com p/ as capas
)

/** Um jogo. `file` só existe quando é arquivo local (permite capa ao lado); SAF usa só `uri`.
 *  `zipEntry` != null quando a ROM está dentro de um .zip (nome da entry a extrair). */
data class GameEntry(
    val title: String, val system: GameSystem, val uri: Uri,
    val file: File? = null, val added: Long = 0L, val zipEntry: String? = null,
)

object Games {

    // Só consoles que rodam NATIVO no app — foco: rodar liso em hardware fraco.
    val systems = listOf(
        GameSystem("NES", setOf("nes"), "fceumm_libretro_android.so",
            "Nintendo - Nintendo Entertainment System"),
        GameSystem("Super Nintendo", setOf("smc", "sfc"), "snes9x_libretro_android.so",
            "Nintendo - Super Nintendo Entertainment System"),
        GameSystem("Game Boy", setOf("gb", "gbc"), "gambatte_libretro_android.so",
            "Nintendo - Game Boy"),
        GameSystem("Game Boy Advance", setOf("gba"), "mgba_libretro_android.so",
            "Nintendo - Game Boy Advance"),
        GameSystem("Mega Drive", setOf("md", "gen", "smd", "sms", "gg"), "genesis_plus_gx_libretro_android.so",
            "Sega - Mega Drive - Genesis"),
    )
    private val byExt = systems.flatMap { s -> s.exts.map { it to s } }.toMap()

    private const val PREFS = "prisma"
    private const val KEY_FOLDER = "rom_folder"

    /** Pasta do app (sem permissão) — scan principal é a pasta escolhida via SAF. */
    private fun romDirs(context: Context): List<File> =
        context.getExternalFilesDirs(null).filterNotNull().map { File(it, "Roms") }

    /** Auto-scan dos diretórios de arquivo + da pasta escolhida pelo usuário (SAF). Suporta .zip. */
    fun load(context: Context): List<GameEntry> {
        val out = ArrayList<GameEntry>()
        romDirs(context).forEach { dir ->
            dir.listFiles()?.forEach { f ->
                val ext = f.extension.lowercase()
                val sys = byExt[ext]
                if (sys != null) {
                    out += GameEntry(f.nameWithoutExtension, sys, Uri.fromFile(f), f, f.lastModified())
                } else if (ext == "zip") {
                    zipInner { f.inputStream() }?.let { (s, entry) ->
                        out += GameEntry(romName(entry), s, Uri.fromFile(f), f, f.lastModified(), entry)
                    }
                }
            }
        }
        savedFolder(context)?.let { tree ->
            DocumentFile.fromTreeUri(context, tree)?.listFiles()?.forEach { doc ->
                val name = doc.name ?: return@forEach
                val ext = name.substringAfterLast('.', "").lowercase()
                val sys = byExt[ext]
                if (sys != null) {
                    out += GameEntry(name.substringBeforeLast('.'), sys, doc.uri, added = doc.lastModified())
                } else if (ext == "zip") {
                    zipInner { context.contentResolver.openInputStream(doc.uri) }?.let { (s, entry) ->
                        out += GameEntry(romName(entry), s, doc.uri, added = doc.lastModified(), zipEntry = entry)
                    }
                }
            }
        }
        return out.distinctBy { it.title + "|" + it.system.label }.sortedBy { it.title.lowercase() }
    }

    private fun romName(entry: String) = entry.substringAfterLast('/').substringBeforeLast('.')

    /** Abre um zip e acha a 1ª ROM suportada dentro (sistema + nome da entry). */
    private fun zipInner(open: () -> java.io.InputStream?): Pair<GameSystem, String>? = runCatching {
        open()?.use { ins ->
            java.util.zip.ZipInputStream(java.io.BufferedInputStream(ins)).use { zis ->
                var e = zis.nextEntry
                while (e != null) {
                    if (!e.isDirectory) {
                        val sys = byExt[e.name.substringAfterLast('.', "").lowercase()]
                        if (sys != null) return@runCatching sys to e.name
                    }
                    e = zis.nextEntry
                }
            }
        }
        null
    }.getOrNull()

    /** Extrai os bytes de uma entry do zip (usado ao abrir o jogo). */
    fun extractZip(context: Context, uri: Uri, entryName: String): ByteArray? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { ins ->
            java.util.zip.ZipInputStream(java.io.BufferedInputStream(ins)).use { zis ->
                var e = zis.nextEntry
                while (e != null) {
                    if (e.name == entryName) return@runCatching zis.readBytes()
                    e = zis.nextEntry
                }
            }
        }
        null
    }.getOrNull()

    /** Nome do arquivo de save-state (estável por jogo+console). */
    fun stateKey(e: GameEntry): String =
        (e.title + "_" + e.system.label).replace(Regex("[^A-Za-z0-9]"), "_")

    /** load: "auto" (continua do auto-save), "new" (começa do zero) ou "1".."3" (carrega o slot). */
    fun launch(context: Context, entry: GameEntry, load: String = "auto") {
        val intent = Intent(context, com.pedro.prisma.GameActivity::class.java)
            .putExtra("core", entry.system.core)
            .putExtra("rom", entry.uri.toString())
            .putExtra("key", stateKey(entry))
            .putExtra("system", entry.system.label)
            .putExtra("title", entry.title)
            .putExtra("load", load)
            .putExtra("zipEntry", entry.zipEntry)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /* ---- pasta de ROMs escolhida pelo usuário (SAF, sem permissão especial) ---- */

    fun savedFolder(context: Context): Uri? {
        val s = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_FOLDER, null)
        return s?.let { runCatching { Uri.parse(it) }.getOrNull() }
    }

    fun saveFolder(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_FOLDER, uri.toString()).apply()
    }
}
