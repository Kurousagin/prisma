package com.pedro.prisma.ui

import android.content.Context

/** Lembra o filtro, a ordenação e a visão (grade/estante) da Biblioteca entre sessões. */
object LibPrefs {
    private fun p(c: Context) = c.getSharedPreferences("prisma", Context.MODE_PRIVATE)
    fun filter(c: Context) = p(c).getString("lib_filter", "Todos") ?: "Todos"
    fun sort(c: Context) = p(c).getString("lib_sort", "az") ?: "az"
    fun view(c: Context) = p(c).getString("lib_view", "grade") ?: "grade"
    fun set(c: Context, key: String, v: String) = p(c).edit().putString("lib_$key", v).apply()
}
