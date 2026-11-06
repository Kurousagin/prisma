package com.pedro.prisma.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.net.URL
import java.net.URLEncoder

/** Capa (box art): arquivo local ao lado da ROM (quando é arquivo), ou baixada do banco libretro (com cache). */
object Covers {

    fun load(context: Context, entry: GameEntry): Bitmap? {
        localCover(entry)?.let { return it }

        val cacheName = (entry.title + "_" + entry.system.label).replace(Regex("[^A-Za-z0-9]"), "_")
        val cache = File(context.cacheDir, "covers/$cacheName.png")
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

    /** <nome>.png/.jpg ao lado da ROM (só vale p/ ROM que é arquivo local). */
    private fun localCover(entry: GameEntry): Bitmap? {
        val f = entry.file ?: return null
        val dir = f.parentFile ?: return null
        val name = f.nameWithoutExtension
        for (ext in listOf("png", "jpg", "jpeg")) {
            for (c in listOf(File(dir, "$name.$ext"), File(File(dir, "covers"), "$name.$ext"))) {
                if (c.exists()) BitmapFactory.decodeFile(c.path)?.let { return it }
            }
        }
        return null
    }

    private fun libretroUrl(entry: GameEntry): String? {
        val sys = entry.system.thumbDir ?: return null
        val name = entry.title.replace(Regex("[&*/:`<>?\\\\|]"), "_")
        return "https://thumbnails.libretro.com/${enc(sys)}/Named_Boxarts/${enc(name)}.png"
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
