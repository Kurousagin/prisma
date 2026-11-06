package com.pedro.prisma.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.net.URL
import java.net.URLEncoder

/** Capa (box art) do jogo: arquivo local ao lado da ROM, ou baixada do banco libretro (com cache). */
object Covers {

    fun load(context: Context, entry: GameEntry): Bitmap? {
        localCover(entry)?.let { return it }

        val cache = File(context.cacheDir, "covers/${entry.file.name}.png")
        if (cache.exists()) return BitmapFactory.decodeFile(cache.path)

        val url = libretroUrl(entry) ?: return null
        return runCatching {
            val bytes = URL(url).openConnection().apply {
                connectTimeout = 4000
                readTimeout = 6000
            }.getInputStream().use { it.readBytes() }
            cache.parentFile?.mkdirs()
            cache.writeBytes(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    /** <nome>.png/.jpg no mesmo diretório da ROM, ou numa subpasta covers/. */
    private fun localCover(entry: GameEntry): Bitmap? {
        val dir = entry.file.parentFile ?: return null
        val name = entry.file.nameWithoutExtension
        for (ext in listOf("png", "jpg", "jpeg")) {
            for (f in listOf(File(dir, "$name.$ext"), File(File(dir, "covers"), "$name.$ext"))) {
                if (f.exists()) BitmapFactory.decodeFile(f.path)?.let { return it }
            }
        }
        return null
    }

    private fun libretroUrl(entry: GameEntry): String? {
        val sys = entry.system.thumbDir ?: return null
        val name = entry.file.nameWithoutExtension.replace(Regex("[&*/:`<>?\\\\|]"), "_")
        return "https://thumbnails.libretro.com/${enc(sys)}/Named_Boxarts/${enc(name)}.png"
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
