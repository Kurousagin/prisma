package com.pedro.prisma.ui

import android.content.Context
import android.content.Intent
import java.io.File

data class GameSystem(
    val label: String,
    val exts: Set<String>,
    val core: String,              // .so em jniLibs → roda embutido (LibretroDroid)
    val thumbDir: String? = null,  // pasta no thumbnails.libretro.com p/ as capas
)

data class GameEntry(val title: String, val file: File, val system: GameSystem)

object Games {

    // Só consoles que rodam NATIVO no app (sem depender de RetroArch) — foco: rodar liso em hardware fraco.
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

    /** /sdcard/Roms + Roms/ em cada USB montado + fallback na pasta do app. */
    fun romDirs(context: Context): List<File> {
        val dirs = mutableListOf(File("/sdcard/Roms"))
        File("/storage").listFiles()?.forEach { vol ->
            if (vol.name != "emulated" && vol.name != "self") dirs += File(vol, "Roms")
        }
        dirs += context.getExternalFilesDirs(null).filterNotNull().map { File(it, "Roms") }
        return dirs.distinct()
    }

    fun load(context: Context): List<GameEntry> =
        romDirs(context).flatMap { dir ->
            dir.listFiles().orEmpty().mapNotNull { f ->
                val sys = byExt[f.extension.lowercase()] ?: return@mapNotNull null
                GameEntry(f.nameWithoutExtension, f, sys)
            }
        }.sortedBy { it.title.lowercase() }

    fun launch(context: Context, entry: GameEntry) {
        val intent = Intent(context, com.pedro.prisma.GameActivity::class.java)
            .putExtra("core", entry.system.core)
            .putExtra("rom", entry.file.absolutePath)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
