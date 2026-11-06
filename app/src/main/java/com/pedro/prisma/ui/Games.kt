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

/** Um jogo. `file` só existe quando é arquivo local (permite capa ao lado); SAF usa só `uri`. */
data class GameEntry(val title: String, val system: GameSystem, val uri: Uri, val file: File? = null, val added: Long = 0L)

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

    /** Auto-scan dos diretórios de arquivo + da pasta escolhida pelo usuário (SAF). */
    fun load(context: Context): List<GameEntry> {
        val out = ArrayList<GameEntry>()
        romDirs(context).forEach { dir ->
            dir.listFiles()?.forEach { f ->
                val sys = byExt[f.extension.lowercase()] ?: return@forEach
                out += GameEntry(f.nameWithoutExtension, sys, Uri.fromFile(f), f, f.lastModified())
            }
        }
        savedFolder(context)?.let { tree ->
            DocumentFile.fromTreeUri(context, tree)?.listFiles()?.forEach { doc ->
                val name = doc.name ?: return@forEach
                val sys = byExt[name.substringAfterLast('.', "").lowercase()] ?: return@forEach
                out += GameEntry(name.substringBeforeLast('.'), sys, doc.uri, added = doc.lastModified())
            }
        }
        return out.distinctBy { it.title + "|" + it.system.label }.sortedBy { it.title.lowercase() }
    }

    /** Nome do arquivo de save-state (estável por jogo+console). */
    fun stateKey(e: GameEntry): String =
        (e.title + "_" + e.system.label).replace(Regex("[^A-Za-z0-9]"), "_")

    fun launch(context: Context, entry: GameEntry) {
        val intent = Intent(context, com.pedro.prisma.GameActivity::class.java)
            .putExtra("core", entry.system.core)
            .putExtra("rom", entry.uri.toString())
            .putExtra("key", stateKey(entry))
            .putExtra("system", entry.system.label)
            .putExtra("title", entry.title)
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
