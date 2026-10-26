package br.com.habil.carona.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfilePhotoStorage(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val directory: File get() = File(context.filesDir, "profile_photos").apply { mkdirs() }

    suspend fun save(sourceUri: String, key: String): String = withContext(ioDispatcher) {
        val uri = Uri.parse(sourceUri)
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: error("Não foi possível abrir a imagem selecionada.")

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("O arquivo selecionado não é uma imagem válida.")

        val file = File(directory, "${key.hashCode().toUInt()}_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        file.absolutePath
    }

    suspend fun delete(path: String) {
        if (path.isBlank()) return
        withContext(ioDispatcher) { File(path).takeIf { it.exists() }?.delete() }
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= MAX_SIDE_PX && height / (sample * 2) >= MAX_SIDE_PX) sample *= 2
        return sample
    }

    private companion object {
        const val MAX_SIDE_PX = 512
    }
}
