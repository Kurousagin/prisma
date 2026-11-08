package com.pedro.prisma.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup dos saves num .zip: tudo em filesDir (save states, saves de bateria .srm e
 * capas trocadas pelo usuário). Capas baixadas ficam em cacheDir e não entram (recuperáveis).
 */
object Backup {

    private fun zipName() =
        "prisma-saves-${android.text.format.DateFormat.format("yyyyMMdd-HHmm", System.currentTimeMillis())}.zip"

    // só dados de save: states/ + capas trocadas (covers/) + saves de bateria (.srm/.rtc/.sav).
    // NÃO inclui a chave do controle remoto (adbkey) nem marcadores do sistema.
    private fun saveFiles(ctx: Context): List<File> {
        val fromDirs = listOf("states", "covers")
            .flatMap { File(ctx.filesDir, it).walkTopDown().filter(File::isFile) }
        val battery = ctx.filesDir.listFiles { f ->
            f.isFile && f.extension.lowercase() in setOf("srm", "rtc", "sav")
        }?.toList().orEmpty()
        return fromDirs + battery
    }

    /** Escreve o backup na pasta escolhida (SAF). Retorna o nome do arquivo, ou null se vazio/falhou. */
    fun exportToTree(ctx: Context, treeUri: Uri): String? {
        val files = saveFiles(ctx)
        if (files.isEmpty()) return null
        val name = zipName()
        val doc = DocumentFile.fromTreeUri(ctx, treeUri)?.createFile("application/zip", name) ?: return null
        return runCatching {
            ctx.contentResolver.openOutputStream(doc.uri)!!.use { writeZip(ctx, files, it) }
            Achievements.onBackup(ctx)
            name
        }.getOrNull()
    }

    /** Monta o backup no cache e abre o compartilhamento do Android. */
    fun share(ctx: Context) {
        val files = saveFiles(ctx)
        if (files.isEmpty()) { Toast.makeText(ctx, "Nenhum save pra compartilhar ainda", Toast.LENGTH_SHORT).show(); return }
        runCatching {
            val zip = File(ctx.cacheDir, "shares").apply { mkdirs() }.let { File(it, zipName()) }
            zip.outputStream().use { writeZip(ctx, files, it) }
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", zip)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Meus saves do Prisma 🦎")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(send, "Compartilhar saves").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Achievements.onBackup(ctx)
        }.onFailure { Toast.makeText(ctx, "Não consegui compartilhar", Toast.LENGTH_SHORT).show() }
    }

    /** Restaura um .zip de backup por cima dos saves atuais. Retorna quantos arquivos escreveu. */
    fun importFromZip(ctx: Context, zipUri: Uri): Int {
        val base = ctx.filesDir.canonicalPath + File.separator
        var n = 0
        runCatching {
            ctx.contentResolver.openInputStream(zipUri)!!.use { ins ->
                ZipInputStream(ins).use { zis ->
                    var e: ZipEntry? = zis.nextEntry
                    while (e != null) {
                        val out = File(ctx.filesDir, e.name)
                        // zip-slip: ignora entradas que tentem escrever fora do filesDir
                        if (!e.isDirectory && out.canonicalPath.startsWith(base)) {
                            out.parentFile?.mkdirs()
                            out.outputStream().use { zis.copyTo(it) }
                            n++
                        }
                        zis.closeEntry(); e = zis.nextEntry
                    }
                }
            }
        }
        return n
    }

    private fun writeZip(ctx: Context, files: List<File>, out: OutputStream) {
        val base = ctx.filesDir.canonicalPath
        ZipOutputStream(out).use { zos ->
            for (f in files) {
                zos.putNextEntry(ZipEntry(f.canonicalPath.removePrefix(base).trimStart(File.separatorChar)))
                f.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }
}
