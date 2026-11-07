package com.pedro.prisma.ui

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Ficha do jogo (ano/gênero/sobre) da Wikipédia PT — sem chave, baixada uma vez e cacheada no aparelho. */
object GameInfo {
    data class Info(val year: String?, val genre: String?, val about: String)

    /** Roda em IO. Retorna da cache se já buscou (inclusive "não achou", p/ não re-baixar). */
    fun get(ctx: Context, title: String): Info? {
        val cache = File(ctx.cacheDir, "meta/${key(title)}.json")
        if (cache.exists()) return parseCache(cache)
        val info = runCatching { fetch(title) }.getOrNull()
        runCatching { cache.parentFile?.mkdirs(); cache.writeText(info?.let { toJson(it) } ?: "") }
        return info
    }

    private fun key(title: String) = title.replace(Regex("[^A-Za-z0-9]"), "_")

    /** Tira tags de região/tradução: "Sonic (USA, Europe)" -> "Sonic". */
    private fun base(title: String) = title
        .replace(Regex("\\s*\\([^)]*\\)"), "").replace(Regex("\\s*\\[[^]]*\\]"), "").trim()

    private fun fetch(title: String): Info? {
        val q = base(title).ifBlank { return null }
        val url = "https://pt.wikipedia.org/w/api.php?action=query&format=json" +
            "&prop=extracts%7Cdescription&exintro=1&explaintext=1&redirects=1" +
            "&generator=search&gsrlimit=5&gsrsearch=" + enc(q)
        val json = httpGet(url) ?: return null
        val pages = JSONObject(json).optJSONObject("query")?.optJSONObject("pages") ?: return null
        val list = pages.keys().asSequence().map { pages.getJSONObject(it) }
            .sortedBy { it.optInt("index", 99) }.toList()
        if (list.isEmpty()) return null

        val norm = normalize(q)
        val page = list.firstOrNull { normalize(it.optString("title")) == norm }     // título exato do jogo
            ?: list.firstOrNull { hasYear(it.optString("description")) }             // senão, o 1º com ano
            ?: list.first()
        val about = page.optString("extract").trim()
        if (about.isBlank()) return null
        return Info(year(page.optString("description") + " " + about), genre(about), about)
    }

    private fun normalize(s: String) = s.lowercase().replace(Regex("[^a-z0-9]"), "")
    private val yearRx = Regex("\\b(19|20)\\d{2}\\b")
    private fun hasYear(s: String) = yearRx.containsMatchIn(s)
    private fun year(s: String) = yearRx.find(s)?.value
    private fun genre(s: String) = Regex("do gênero ([^.,;]+)", RegexOption.IGNORE_CASE)
        .find(s)?.groupValues?.get(1)?.trim()?.takeIf { it.length in 3..40 }

    private fun httpGet(url: String): String? = runCatching {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000; readTimeout = 6000; instanceFollowRedirects = true
            setRequestProperty("User-Agent", "PrismaEmu/1.0 (retro game catalog)")
        }
        if (c.responseCode != 200) { c.disconnect(); return null }
        c.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }.getOrNull()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun toJson(i: Info) =
        JSONObject().put("year", i.year).put("genre", i.genre).put("about", i.about).toString()

    private fun parseCache(f: File): Info? {
        val t = f.readText()
        if (t.isBlank()) return null
        return runCatching {
            val o = JSONObject(t)
            Info(o.optString("year").ifBlank { null }, o.optString("genre").ifBlank { null }, o.optString("about"))
        }.getOrNull()
    }
}
