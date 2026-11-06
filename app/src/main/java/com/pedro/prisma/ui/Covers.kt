package com.pedro.prisma.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Capa (box art): arquivo local ao lado da ROM, ou baixada do banco libretro (com cache, inclusive de "não achou"). */
object Covers {

    fun load(context: Context, entry: GameEntry): Bitmap? {
        userCover(context, entry)?.let { return it }   // capa escolhida à mão vence tudo
        localCover(entry)?.let { return it }

        val sys = entry.system.thumbDir ?: return null
        val cacheName = (entry.title + "_" + entry.system.label).replace(Regex("[^A-Za-z0-9]"), "_")
        val cache = File(context.cacheDir, "covers/$cacheName.png")
        if (cache.exists()) {
            val b = cache.readBytes()
            return if (b.isNotEmpty()) BitmapFactory.decodeByteArray(b, 0, b.size) else null // vazio = já tentou e não achou
        }

        for (name in candidates(entry.title)) {
            val url = "https://thumbnails.libretro.com/${enc(sys)}/Named_Boxarts/${enc(name)}.png"
            val bytes = download(url) ?: continue
            runCatching { cache.parentFile?.mkdirs(); cache.writeBytes(bytes) }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        // marca como "não tem" pra não ficar re-baixando toda vez
        runCatching { cache.parentFile?.mkdirs(); cache.writeBytes(ByteArray(0)) }
        return null
    }

    /** Nomes candidatos no banco: o próprio, o base (sem tags) e as regiões comuns. */
    private fun candidates(title: String): List<String> {
        val base = title
            .replace(Regex("\\s*\\([^)]*\\)"), "")   // tira (BR), (T1.0), (www...), etc.
            .replace(Regex("\\s*\\[[^]]*\\]"), "")
            .trim()
        val names = mutableListOf(title, base)
        for (region in listOf("USA", "Europe", "Japan", "World", "USA, Europe")) names += "$base ($region)"
        return names.map { it.replace(Regex("[&*/:`<>?\\\\|]"), "_") }.filter { it.isNotBlank() }.distinct()
    }

    private fun download(url: String): ByteArray? = runCatching {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000; readTimeout = 6000; instanceFollowRedirects = true
        }
        if (c.responseCode != 200) { c.disconnect(); return null }
        c.inputStream.use { it.readBytes() }
    }.getOrNull()

    /** Capa só do que já está em disco (sem rede) — pra usos rápidos, como a animação de abertura. */
    fun cached(context: Context, entry: GameEntry): Bitmap? {
        userCover(context, entry)?.let { return it }
        localCover(entry)?.let { return it }
        entry.system.thumbDir ?: return null
        val cacheName = (entry.title + "_" + entry.system.label).replace(Regex("[^A-Za-z0-9]"), "_")
        val cache = File(context.cacheDir, "covers/$cacheName.png")
        if (cache.exists()) {
            val b = cache.readBytes()
            if (b.isNotEmpty()) return BitmapFactory.decodeByteArray(b, 0, b.size)
        }
        return null
    }

    /** Capa que o usuário escolheu da galeria, guardada por key (independe de ter caminho de arquivo). */
    private fun userFile(context: Context, entry: GameEntry) =
        File(context.filesDir, "covers/${Games.stateKey(entry)}.png")

    private fun userCover(context: Context, entry: GameEntry): Bitmap? {
        val f = userFile(context, entry)
        return if (f.exists()) BitmapFactory.decodeFile(f.path) else null
    }

    fun saveUserCover(context: Context, entry: GameEntry, bytes: ByteArray) {
        val f = userFile(context, entry)
        runCatching { f.parentFile?.mkdirs(); f.writeBytes(bytes) }
    }

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

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
}
