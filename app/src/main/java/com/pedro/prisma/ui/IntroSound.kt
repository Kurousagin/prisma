package com.pedro.prisma.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Som nostálgico da abertura: "tlec" do cartucho encaixando + chiado de TV ligando. Sintetizado, sem arquivo. */
object IntroSound {
    private const val RATE = 22050

    /** Toca em paralelo à animação. Retorna o track para parar se o usuário pular. */
    fun play(): AudioTrack? = runCatching {
        val buf = render()
        val track = AudioTrack(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            AudioFormat.Builder().setSampleRate(RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
            buf.size * 2, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
        track.write(buf, 0, buf.size)
        track.play()
        track
    }.getOrNull()

    fun stop(track: AudioTrack?) { runCatching { track?.pause(); track?.flush(); track?.release() } }

    private fun render(): ShortArray {
        val out = ShortArray((RATE * 1.9).toInt())
        val rnd = Random(7)
        clunk(out, (RATE * 1.20).toInt(), rnd)                        // encaixe do cartucho
        hiss(out, (RATE * 1.20).toInt(), (RATE * 0.55).toInt(), rnd)  // chiado da TV ligando
        return out
    }

    /** "Tlec": contato seco (ruído curtíssimo) + corpo grave que decai rápido. */
    private fun clunk(out: ShortArray, at: Int, rnd: Random) {
        val n = (RATE * 0.14).toInt()
        for (i in 0 until n) {
            val t = i.toFloat() / RATE
            val env = exp(-t * 34f)
            val body = sin(2.0 * Math.PI * 95.0 * t).toFloat()
            val tick = (rnd.nextFloat() * 2 - 1) * exp(-t * 220f)
            add(out, at + i, (body * 0.55f + tick * 0.5f) * env * 0.9f)
        }
    }

    /** Ruído meio filtrado (grave) que sobe rápido e some — TV de tubo ligando. */
    private fun hiss(out: ShortArray, at: Int, n: Int, rnd: Random) {
        var lp = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val env = (t * 3f).coerceAtMost(1f) * (1f - t)
            lp += ((rnd.nextFloat() * 2 - 1) - lp) * 0.5f
            add(out, at + i, lp * env * 0.22f)
        }
    }

    private fun add(out: ShortArray, i: Int, v: Float) {
        if (i < 0 || i >= out.size) return
        out[i] = (out[i] + v * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
    }
}
