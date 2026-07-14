package com.gymgym.app.recording

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** On-device storage + sharing for workout recordings (app-private external dir). */
object RecordingStore {

    private val fileStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun dir(context: Context): File =
        File(context.getExternalFilesDir(null), "recordings").apply { mkdirs() }

    fun newFile(context: Context): File =
        File(dir(context), "workout_${fileStamp.format(Date())}.mp4")

    fun list(context: Context): List<File> =
        dir(context).listFiles()
            ?.filter { it.isFile && it.extension.equals("mp4", ignoreCase = true) && it.length() > 0 }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    private fun uriFor(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Opens the Android share sheet (WhatsApp, Telegram, email, …). */
    fun share(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share recording"))
    }

    /** Opens the recording in an external video player. */
    fun play(context: Context, file: File) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriFor(context, file), "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun delete(file: File): Boolean = file.delete()
}
