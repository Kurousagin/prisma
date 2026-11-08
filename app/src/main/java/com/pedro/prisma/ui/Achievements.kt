package com.pedro.prisma.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

data class Achievement(val id: String, val emoji: String, val title: String, val desc: String)

/**
 * Conquistas locais: marcos de uso guardados no aparelho, sem conta e sem internet.
 * Arquitetura aberta pra, no futuro, somar RetroAchievements por jogo (épico separado).
 */
object Achievements {
    val ALL = listOf(
        Achievement("first_game", "🎮", "Primeira partida", "Abriu seu primeiro jogo."),
        Achievement("collector", "📚", "Colecionador", "Abriu 10 jogos diferentes."),
        Achievement("multiconsole", "🌈", "Multiconsole", "Jogou em 3 consoles diferentes."),
        Achievement("time_traveler", "💾", "Viajante do tempo", "Criou seu primeiro save state."),
        Achievement("photographer", "📸", "Fotógrafo", "Compartilhou um print de um jogo."),
        Achievement("backup", "☁️", "Precavido", "Fez um backup dos saves."),
        Achievement("stylist", "🖼️", "Estilista", "Trocou a capa de um jogo."),
        Achievement("marathon", "🕹️", "Maratonista", "Somou 1 hora de jogo."),
    )

    private const val HOUR_MS = 3_600_000L

    private fun p(c: Context) = c.getSharedPreferences("prisma", Context.MODE_PRIVATE)
    private fun set(c: Context, key: String) = p(c).getStringSet(key, emptySet())!!.toMutableSet()

    fun unlocked(c: Context): Set<String> = p(c).getStringSet("ach_unlocked", emptySet())!!.toSet()
    fun isUnlocked(c: Context, id: String) = unlocked(c).contains(id)
    fun count(c: Context) = unlocked(c).size

    /** Desbloqueia (se ainda não) e avisa. */
    private fun unlock(c: Context, id: String) {
        val u = set(c, "ach_unlocked")
        if (u.add(id)) {
            p(c).edit().putStringSet("ach_unlocked", u).apply()
            ALL.find { it.id == id }?.let { toast(c, "🏆 Conquista: ${it.title}") }
        }
    }

    /* ---------------- eventos ---------------- */

    fun onGameLaunched(c: Context, title: String, system: String) {
        unlock(c, "first_game")
        val games = set(c, "ach_games").apply { add(title) }
        p(c).edit().putStringSet("ach_games", games).apply()
        if (games.size >= 10) unlock(c, "collector")
        val consoles = set(c, "ach_consoles").apply { add(system) }
        p(c).edit().putStringSet("ach_consoles", consoles).apply()
        if (consoles.size >= 3) unlock(c, "multiconsole")
    }

    fun onSaveState(c: Context) = unlock(c, "time_traveler")
    fun onShare(c: Context) = unlock(c, "photographer")
    fun onBackup(c: Context) = unlock(c, "backup")
    fun onCoverChanged(c: Context) = unlock(c, "stylist")

    fun addPlaytime(c: Context, ms: Long) {
        if (ms <= 0) return
        val total = p(c).getLong("ach_playtime", 0L) + ms
        p(c).edit().putLong("ach_playtime", total).apply()
        if (total >= HOUR_MS) unlock(c, "marathon")
    }

    private fun toast(c: Context, msg: String) =
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(c.applicationContext, msg, Toast.LENGTH_LONG).show()
        }
}
