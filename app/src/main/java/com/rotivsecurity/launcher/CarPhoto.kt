package com.rotivsecurity.launcher

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** Foto do carro do usuário, copiada para a pasta do app. */
object CarPhoto {
    private val _image = MutableStateFlow<ImageBitmap?>(null)
    val image: StateFlow<ImageBitmap?> = _image

    private fun file(ctx: Context) = File(ctx.filesDir, "meu_carro.jpg")

    fun load(ctx: Context) {
        val f = file(ctx)
        if (!f.exists()) {
            _image.value = null
            return
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 1600) sample *= 2
        val bmp = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
        _image.value = bmp?.asImageBitmap()
    }

    /** Copia a imagem escolhida e ativa a foto. Devolve false se não deu para ler. */
    fun import(ctx: Context, uri: Uri): Boolean = try {
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            file(ctx).outputStream().use { input.copyTo(it) }
        }
        load(ctx)
        val ok = _image.value != null
        Prefs.setCarPhoto(ok)
        ok
    } catch (e: Exception) {
        false
    }

    fun remove(ctx: Context) {
        file(ctx).delete()
        _image.value = null
        Prefs.setCarPhoto(false)
    }
}
