package com.pedro.prisma.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.swordfish.libretrodroid.GLRetroView
import java.io.File

/** Captura o frame do jogo, monta numa moldura de TV retrô e abre o compartilhamento do Android. */
object Screenshot {

    fun shareFramed(view: GLRetroView, ctx: Context, title: String, system: String) {
        val w = view.width; val h = view.height
        if (w <= 0 || h <= 0) { Toast.makeText(ctx, "Tela ainda não pronta", Toast.LENGTH_SHORT).show(); return }
        val frame = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        runCatching {
            PixelCopy.request(view, frame, { res ->
                if (res != PixelCopy.SUCCESS) { toastFail(ctx); return@request }
                val framed = runCatching { compose(frame, systemColor(system).toArgb(), title, system) }.getOrNull()
                if (framed == null) { toastFail(ctx); return@request }
                runCatching { share(ctx, framed) }.onFailure { toastFail(ctx) }
            }, Handler(Looper.getMainLooper()))
        }.onFailure { toastFail(ctx) }
    }

    private fun toastFail(ctx: Context) =
        Toast.makeText(ctx, "Não consegui capturar a tela", Toast.LENGTH_SHORT).show()

    private const val SW = 1000  // largura alvo da captura dentro da moldura
    private const val PAD = 44
    private const val BAR = 128

    private fun compose(src: Bitmap, accent: Int, title: String, system: String): Bitmap {
        val sh = (src.height.toLong() * SW / src.width).toInt().coerceAtLeast(1)
        val shot = Bitmap.createScaledBitmap(src, SW, sh, true)
        val cw = SW + PAD * 2
        val ch = sh + PAD + BAR
        val out = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // bezel
        p.color = Color.parseColor("#141019")
        c.drawRoundRect(RectF(0f, 0f, cw.toFloat(), ch.toFloat()), 52f, 52f, p)

        // tela
        val screen = RectF(PAD.toFloat(), PAD.toFloat(), (PAD + SW).toFloat(), (PAD + sh).toFloat())
        c.save(); val clip = android.graphics.Path().apply { addRoundRect(screen, 18f, 18f, android.graphics.Path.Direction.CW) }
        c.clipPath(clip); c.drawBitmap(shot, null, screen, p); c.restore()
        p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = accent
        c.drawRoundRect(screen, 18f, 18f, p)
        p.style = Paint.Style.FILL

        // barra inferior: ponto na cor do console + título + marca
        val barY = PAD + sh + BAR / 2f
        p.color = accent; c.drawCircle((PAD + 16).toFloat(), barY, 13f, p)
        p.color = Color.parseColor("#F5F3FB")
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); p.textSize = 42f
        c.drawText(ellipsize(p, title, SW - 320f), (PAD + 42).toFloat(), barY + 15f, p)
        p.color = Color.parseColor("#8E88A8"); p.textSize = 32f; p.textAlign = Paint.Align.RIGHT
        c.drawText("PRISMA · $system", (cw - PAD).toFloat(), barY + 11f, p)
        return out
    }

    private fun ellipsize(p: Paint, text: String, maxW: Float): String {
        if (p.measureText(text) <= maxW) return text
        var s = text
        while (s.length > 1 && p.measureText("$s…") > maxW) s = s.dropLast(1)
        return "$s…"
    }

    private fun share(ctx: Context, bmp: Bitmap) {
        val dir = File(ctx.cacheDir, "shares").apply { mkdirs() }
        val file = File(dir, "prisma_print.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Jogando no Prisma 🦎")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "Compartilhar print").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
