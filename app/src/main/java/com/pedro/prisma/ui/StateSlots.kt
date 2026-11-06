package com.pedro.prisma.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.swordfish.libretrodroid.GLRetroView
import java.io.File

/**
 * Save states manuais por jogo. Cada slot guarda o estado serializado + uma miniatura (PNG).
 * O auto-save ("Continuar") continua em "$key.state", separado dos slots.
 */
object StateSlots {
    const val COUNT = 3

    data class SlotInfo(val n: Int, val time: Long?, val hasThumb: Boolean) {
        val used get() = time != null
    }

    private fun dir(ctx: Context) = File(ctx.filesDir, "states").apply { mkdirs() }
    private fun stateFile(ctx: Context, key: String, n: Int) = File(dir(ctx), "$key.slot$n.state")
    private fun thumbFile(ctx: Context, key: String, n: Int) = File(dir(ctx), "$key.slot$n.png")

    fun list(ctx: Context, key: String): List<SlotInfo> = (1..COUNT).map { n ->
        val s = stateFile(ctx, key, n)
        SlotInfo(n, if (s.exists()) s.lastModified() else null, thumbFile(ctx, key, n).exists())
    }

    fun loadThumb(ctx: Context, key: String, n: Int): ImageBitmap? {
        val f = thumbFile(ctx, key, n)
        if (!f.exists()) return null
        return runCatching { BitmapFactory.decodeFile(f.path)?.asImageBitmap() }.getOrNull()
    }

    /** Salva o estado no slot e captura a miniatura (assíncrona). onDone roda ao terminar. */
    fun save(retro: GLRetroView, ctx: Context, key: String, n: Int, onDone: () -> Unit) {
        runCatching { stateFile(ctx, key, n).writeBytes(retro.serializeState()) }
        captureThumb(retro) { bmp ->
            if (bmp != null) runCatching {
                thumbFile(ctx, key, n).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
            }
            onDone()
        }
    }

    fun load(retro: GLRetroView, ctx: Context, key: String, n: Int): Boolean {
        val f = stateFile(ctx, key, n)
        if (!f.exists()) return false
        return runCatching { retro.unserializeState(f.readBytes()); true }.getOrDefault(false)
    }

    private fun captureThumb(view: GLRetroView, cb: (Bitmap?) -> Unit) {
        val w = view.width; val h = view.height
        if (w <= 0 || h <= 0) { cb(null); return }
        val full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        runCatching {
            PixelCopy.request(view, full, { res ->
                if (res == PixelCopy.SUCCESS) {
                    val tw = 240; val th = (h.toLong() * tw / w).toInt().coerceAtLeast(1)
                    cb(runCatching { Bitmap.createScaledBitmap(full, tw, th, true) }.getOrNull())
                } else cb(null)
            }, Handler(Looper.getMainLooper()))
        }.onFailure { cb(null) }
    }
}
