package com.pedro.prisma.ui

import android.content.Context

/** Jogos favoritados (por stateKey), guardados só no aparelho. */
object Favorites {
    private const val PREFS = "prisma"
    private const val KEY = "favorites"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(c: Context): Set<String> = prefs(c).getStringSet(KEY, emptySet())!!
    fun contains(c: Context, key: String) = all(c).contains(key)

    /** Alterna e retorna o novo estado (true = agora é favorito). */
    fun toggle(c: Context, key: String): Boolean {
        val s = all(c).toMutableSet()
        val now = if (s.contains(key)) { s.remove(key); false } else { s.add(key); true }
        prefs(c).edit().putStringSet(KEY, s).apply()
        return now
    }
}
