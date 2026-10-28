package com.example.momentmock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.util.UUID

object Images {

    private var libraryCache: List<String>? = null

    private fun library(ctx: Context): List<String> {
        libraryCache?.let { return it }
        val names = ctx.assets.list("avatars")?.filter { it.endsWith(".jpg") }?.sorted() ?: emptyList()
        libraryCache = names
        return names
    }

    fun librarySize(ctx: Context): Int = library(ctx).size

    fun randomAvatar(ctx: Context): Bitmap? {
        val files = library(ctx)
        if (files.isEmpty()) return null
        return loadAsset(ctx, "avatars/" + files[(Math.random() * files.size).toInt()])
    }

    fun randomAvatars(ctx: Context, count: Int): List<Bitmap> {
        val files = library(ctx)
        if (files.isEmpty() || count <= 0) return emptyList()
        val idx = files.indices.shuffled()
        return idx.take(count).mapNotNull { loadAsset(ctx, "avatars/" + files[it]) }
    }

    private fun loadAsset(ctx: Context, path: String): Bitmap? = runCatching {
        ctx.assets.open(path).use { BitmapFactory.decodeStream(it) }
    }.getOrNull()

    fun saveFromUri(ctx: Context, uri: Uri): String? = runCatching {
        val name = "p_" + UUID.randomUUID().toString().take(8) + ".jpg"
        ctx.contentResolver.openInputStream(uri)!!.use { input ->
            File(ctx.filesDir, name).outputStream().use { out -> input.copyTo(out) }
        }
        name
    }.getOrNull()

    fun load(ctx: Context, name: String?): Bitmap? {
        if (name.isNullOrBlank()) return null
        val f = File(ctx.filesDir, name)
        if (!f.exists()) return null
        return decodeSampled(f.path, 1200)
    }

    private fun decodeSampled(path: String, maxSize: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        var sample = 1
        while (opts.outWidth / sample > maxSize || opts.outHeight / sample > maxSize) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun delete(ctx: Context, name: String?) {
        if (!name.isNullOrBlank()) runCatching { File(ctx.filesDir, name).delete() }
    }

    fun saveVideoFrame(ctx: Context, uri: Uri): String? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(ctx, uri)
            var frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return null
            val maxSide = maxOf(frame.width, frame.height)
            if (maxSide > 1600) {
                val s = 1600f / maxSide
                frame = Bitmap.createScaledBitmap(
                    frame,
                    (frame.width * s).toInt(),
                    (frame.height * s).toInt(),
                    true,
                )
            }
            val name = "v_" + UUID.randomUUID().toString().take(8) + ".jpg"
            File(ctx.filesDir, name).outputStream().use { out ->
                frame.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            name
        } finally {
            retriever.release()
        }
    }.getOrNull()
}
